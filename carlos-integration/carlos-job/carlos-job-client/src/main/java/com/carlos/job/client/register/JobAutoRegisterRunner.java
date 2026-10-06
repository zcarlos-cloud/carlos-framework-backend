package com.carlos.job.client.register;

import com.carlos.core.exception.ComponentException;
import com.carlos.job.client.config.JobClientProperties;
import com.carlos.job.client.processor.JobAnnotationBeanPostProcessor;
import com.carlos.job.pojo.ao.JobRegisterAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * 注解任务启动自动注册：应用就绪后把 {@code @AutoRegisterJob} 声明的任务上报调度中心
 * </p>
 *
 * <p>四个刻意的取舍：</p>
 * <ul>
 *   <li><b>用 {@code ApplicationReadyEvent} 而不是 {@code ApplicationRunner}</b>：Runner 抛异常会终止启动，
 *       且与 {@code JobReconcileRunner} 的顺序无保证；调度中心未启动是预期运维状态。</li>
 *   <li><b>整个监听方法兜底 try/catch</b>：监听器抛出的异常会顺着
 *       {@code SpringApplication.run()} 抛出并<b>真的终止启动</b>（Spring 不吞监听器异常），
 *       所以连同步段（取计划/解析地址/提交任务）也必须包住。</li>
 *   <li><b>守护线程异步执行</b>：注册慢/失败都不能拖住业务服务启动。</li>
 *   <li><b>4xx 与业务拒绝不重试</b>：令牌不配对、参数校验不过属于契约问题，重试不会变好，只会在日志里刷三遍。</li>
 * </ul>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Slf4j
public class JobAutoRegisterRunner implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    private final JobAnnotationBeanPostProcessor annotationProcessor;

    private final JobAdminAddressResolver addressResolver;

    private final JobRegisterClient registerClient;

    private final JobClientProperties clientProperties;

    private final Executor executor;

    public JobAutoRegisterRunner(JobAnnotationBeanPostProcessor annotationProcessor,
                                 JobAdminAddressResolver addressResolver,
                                 JobRegisterClient registerClient,
                                 JobClientProperties clientProperties) {
        this(annotationProcessor, addressResolver, registerClient, clientProperties, defaultExecutor());
    }

    public JobAutoRegisterRunner(JobAnnotationBeanPostProcessor annotationProcessor,
                                 JobAdminAddressResolver addressResolver,
                                 JobRegisterClient registerClient,
                                 JobClientProperties clientProperties,
                                 Executor executor) {
        this.annotationProcessor = annotationProcessor;
        this.addressResolver = addressResolver;
        this.registerClient = registerClient;
        this.clientProperties = clientProperties;
        this.executor = executor;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 监听器抛出的异常会顺着 SpringApplication.run() 抛出去并终止启动（Spring 不吞监听器异常），
        // 而"注册不影响业务服务启动"是本模块的对外契约 —— 故同步段整体兜底，宁可少注册也不能起不来
        try {
            Map<String, JobAutoRegisterPlan> plans = annotationProcessor.autoRegisterPlans();
            if (plans.isEmpty()) {
                return;
            }
            String adminUrl = addressResolver.resolve();
            if (adminUrl == null) {
                log.warn("未配置 carlos.job.client.admin-url，跳过注解任务自动注册: count={}，这些任务需在管理后台手工创建",
                    plans.size());
                return;
            }
            log.info("开始注解任务自动注册: count={}, adminUrl={}", plans.size(), adminUrl);
            executor.execute(() -> registerAll(adminUrl, plans.values()));
        } catch (Exception e) {
            log.error("注解任务自动注册的启动阶段异常（已忽略，不影响本服务启动）", e);
        }
    }

    /**
     * 逐个注册并汇总结果（单个任务失败不影响其余）
     *
     * @param adminUrl 调度中心基础地址
     * @param plans    注册计划
     */
    void registerAll(String adminUrl, Collection<JobAutoRegisterPlan> plans) {
        int created = 0;
        int existed = 0;
        int drifted = 0;
        int failed = 0;
        Map<String, Integer> failureKinds = new LinkedHashMap<>();
        for (JobAutoRegisterPlan plan : plans) {
            try {
                JobRegisterAO result = registerWithRetry(adminUrl, plan);
                if (Boolean.TRUE.equals(result.getCreated())) {
                    created++;
                } else if (Boolean.TRUE.equals(result.getDrifted())) {
                    drifted++;
                } else {
                    existed++;
                }
                log.info("自动注册任务结果: jobName={}, created={}, existed={}, drifted={}, status={}, message={}",
                    plan.getJobName(), result.getCreated(), result.getExisted(), result.getDrifted(),
                    result.getStatus(), result.getMessage());
            } catch (Exception e) {
                failed++;
                String kind = describeFailure(e);
                failureKinds.merge(kind, 1, Integer::sum);
                log.error("任务自动注册失败: jobName={}, handler={}, serviceName={}, scheduleConf={}, 原因={}",
                    plan.getJobName(), plan.getHandlerName(), plan.getServiceName(), plan.getScheduleConf(), kind, e);
            }
        }
        log.info("注解任务自动注册完成: total={}, created={}, existed={}, drifted={}, failed={}",
            plans.size(), created, existed, drifted, failed);
        if (failed > 0 || drifted > 0) {
            // 漂移不是失败（任务已存在、库中配置为主），但同样需要人工看一眼，故与失败一起用 ERROR 保证可见：
            // 本框架的 logback 把 WARN 只写进 debug.log，info/error 两个文件里看不到
            log.error("注解任务注册需人工确认: 失败 {} 个（原因: {}），漂移 {} 个（声明与 job_info 不一致，已保持库中配置）",
                failed, failureKinds.isEmpty() ? "无" : String.join("、", failureKinds.keySet()), drifted);
        }
    }

    /**
     * 把注册失败归类成运维能直接判断的一句话
     *
     * <p>起因：真机验证时看到「连不上调度中心」的堆栈，会被误读成"应用启动失败"。分类后一眼能看出是
     * 连通性问题（重试/重启即可）还是契约问题（令牌、参数写错），两者的处置动作完全不同。</p>
     */
    static String describeFailure(Exception e) {
        if (e instanceof ResourceAccessException) {
            return "调度中心不可达（调度中心恢复后重启本服务即自动注册，无需手工建任务）";
        }
        if (e instanceof RestClientResponseException response && response.getStatusCode().is4xxClientError()) {
            return "注册请求被拒绝（HTTP " + response.getStatusCode().value() + "，检查 access-token 与注解参数）";
        }
        if (e instanceof ComponentException) {
            return "注册被调度中心拒绝: " + e.getMessage();
        }
        return "注册异常: " + e.getMessage();
    }

    /**
     * 带重试的注册：网络/5xx 线性退避重试，4xx 与响应体明确的业务拒绝直接失败
     */
    private JobRegisterAO registerWithRetry(String adminUrl, JobAutoRegisterPlan plan) {
        int maxAttempts = Math.max(1, clientProperties.getRegisterMaxAttempts());
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return registerClient.register(adminUrl, plan.toRegisterParam());
            } catch (ComponentException e) {
                // 响应体 success=false（鉴权不通过、参数被拒）或响应无法解析：都是契约问题，重试不会变好
                throw e;
            } catch (RestClientResponseException e) {
                if (e.getStatusCode().is4xxClientError()) {
                    throw e;
                }
                lastError = e;
            } catch (RuntimeException e) {
                lastError = e;
            }
            if (attempt < maxAttempts) {
                log.warn("任务自动注册第 {}/{} 次失败，稍后重试: jobName={}, cause={}",
                    attempt, maxAttempts, plan.getJobName(), lastError.getMessage());
                sleepQuietly(clientProperties.getRegisterRetryBackoffMs() * attempt);
            }
        }
        throw lastError;
    }

    private void sleepQuietly(int millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ComponentException("任务自动注册被中断", e);
        }
    }

    @Override
    public void destroy() {
        if (executor instanceof ExecutorService executorService) {
            executorService.shutdownNow();
        }
    }

    private static Executor defaultExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "job-auto-register");
            thread.setDaemon(true);
            return thread;
        });
    }

}
