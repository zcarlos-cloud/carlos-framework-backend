package com.carlos.job.dispatch;

import cn.hutool.core.util.StrUtil;
import com.carlos.job.convert.JobExecutionLogConvert;
import com.carlos.job.manager.JobExecutionLogManager;
import com.carlos.job.pojo.ao.JobResultAO;
import com.carlos.job.pojo.dto.JobExecutionLogDTO;
import com.carlos.job.pojo.enums.JobExecutionStatusEnum;
import com.carlos.job.pojo.enums.JobTriggerTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronTrigger;
import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * Quartz 统一任务入口抽象基类：所有任务都调度到本类子类，按 JobDataMap 中的目标服务与处理器派发执行
 * </p>
 * <p>
 * 并发模式由子类控制：{@link SerialDispatchJob} 集群级串行（@DisallowConcurrentExecution），
 * {@link ParallelDispatchJob} 允许并行。实现 {@link InterruptableJob} 支持任务超时真中断
 * （HTTP 回调随线程中断断连；本机 handler 为协作式中断）。Spring Boot QuartzAutoConfiguration
 * 使用 SpringBeanJobFactory 创建 Job 实例，字段上的 @Autowired 由 BeanPostProcessor 注入。
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Slf4j
public abstract class AbstractDispatchJob implements InterruptableJob {

    /**
     * JobDataMap 键：目标服务名
     */
    public static final String DATA_SERVICE_NAME = "serviceName";

    /**
     * JobDataMap 键：任务处理器名称
     */
    public static final String DATA_HANDLER_NAME = "handlerName";

    /**
     * JobDataMap 键：任务参数
     */
    public static final String DATA_PARAMS = "params";

    /**
     * JobDataMap 键：触发类型（手动触发时由 triggerJob 传入）
     */
    public static final String DATA_TRIGGER_TYPE = "triggerType";

    /**
     * JobDataMap 键：当前重试序号（由失败重试监听器写入）
     */
    public static final String DATA_RETRY_INDEX = "retryIndex";

    /**
     * 执行节点标识（进程级常量，懒初始化一次）
     */
    private static final String SERVER_NODE = resolveServerNode();

    /**
     * 正在执行的线程登记表（fireInstanceId -> 线程），供超时中断定位
     */
    private static final Map<String, Thread> RUNNING_THREADS = new ConcurrentHashMap<>();

    @Autowired
    private JobDispatcher jobDispatcher;

    @Autowired
    private JobExecutionLogManager executionLogManager;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String fireId = context.getFireInstanceId();
        RUNNING_THREADS.put(fireId, Thread.currentThread());
        try {
            doExecute(context);
        } finally {
            RUNNING_THREADS.remove(fireId);
        }
    }

    /**
     * 中断正在执行的任务（超时/手动终止）。对执行线程发中断信号，
     * HTTP 回调随之中断绝连；本机 handler 需响应中断（协作式）。
     */
    @Override
    public void interrupt() throws UnableToInterruptJobException {
        // Quartz 调用 interrupt 时不带 fireInstanceId，中断本实例所有在跑线程
        for (Thread thread : RUNNING_THREADS.values()) {
            thread.interrupt();
        }
    }

    private void doExecute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        String serviceName = dataMap.getString(DATA_SERVICE_NAME);
        String handlerName = dataMap.getString(DATA_HANDLER_NAME);
        String params = dataMap.getString(DATA_PARAMS);

        JobExecutionLogDTO logDTO = null;
        try {
            logDTO = new JobExecutionLogDTO()
                .setJobName(context.getJobDetail().getKey().getName())
                .setJobGroup(context.getJobDetail().getKey().getGroup())
                .setServiceName(serviceName)
                .setHandlerName(handlerName)
                .setTriggerType(resolveTriggerType(context, dataMap))
                .setCronExpression(resolveCron(context))
                .setStatus(JobExecutionStatusEnum.RUNNING.getCode())
                .setStartTime(LocalDateTime.now())
                .setServerNode(SERVER_NODE);
            executionLogManager.add(logDTO);

            JobResultAO result = jobDispatcher.dispatch(serviceName, handlerName, params);
            boolean success = Boolean.TRUE.equals(result.getSuccess());
            finishLog(logDTO, success ? JobExecutionStatusEnum.SUCCESS : JobExecutionStatusEnum.FAILED,
                result.getMessage());
            // 业务返回失败时抛出异常，让 JobListener 感知并驱动失败重试
            if (!success) {
                throw new JobExecutionException(result.getMessage() == null
                    ? "任务执行失败" : result.getMessage());
            }
        } catch (JobExecutionException e) {
            throw e;
        } catch (Exception e) {
            boolean interrupted = e instanceof InterruptedException
                || Thread.currentThread().isInterrupted() && isInterruptCause(e);
            String message = interrupted ? "任务被中断（超时或手动终止）" : e.getMessage();
            log.error("任务执行失败: {}.{}, interrupted={}", context.getJobDetail().getKey().getGroup(),
                context.getJobDetail().getKey().getName(), interrupted, e);
            if (logDTO != null && logDTO.getId() != null) {
                try {
                    finishLog(logDTO, JobExecutionStatusEnum.FAILED, message);
                } catch (Exception logEx) {
                    // 日志补写失败只记日志，不外抛掩盖原始异常
                    log.error("任务执行日志补写失败: id={}", logDTO.getId(), logEx);
                }
            }
            // 清中断标记，避免影响调度线程后续任务
            if (interrupted) {
                Thread.interrupted();
            }
            // 包装为 JobExecutionException 抛出，供失败重试监听器感知
            throw new JobExecutionException(message, e);
        }
    }

    /**
     * 判断异常链是否由线程中断引起
     */
    private boolean isInterruptCause(Throwable e) {
        Throwable cursor = e;
        while (cursor != null) {
            if (cursor instanceof InterruptedException
                || cursor instanceof java.nio.channels.ClosedByInterruptException) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private void finishLog(JobExecutionLogDTO logDTO, JobExecutionStatusEnum status, String message) {
        if (logDTO.getId() == null) {
            return;
        }
        LocalDateTime endTime = LocalDateTime.now();
        // 只更新结果字段，基于原日志 id 构造新对象
        JobExecutionLogDTO updateDTO = new JobExecutionLogDTO()
            .setId(logDTO.getId())
            .setStatus(status.getCode())
            .setEndTime(endTime)
            .setDurationMs(Duration.between(logDTO.getStartTime(), endTime).toMillis())
            .setResultMessage(StrUtil.maxLength(message, 2000));
        executionLogManager.updateById(JobExecutionLogConvert.INSTANCE.toDO(updateDTO));
    }

    private Integer resolveTriggerType(JobExecutionContext context, JobDataMap dataMap) {
        String triggerType = dataMap.getString(DATA_TRIGGER_TYPE);
        if (JobTriggerTypeEnum.MANUAL.name().equals(triggerType)) {
            return JobTriggerTypeEnum.MANUAL.getCode();
        }
        if (JobTriggerTypeEnum.RETRY.name().equals(triggerType)) {
            return JobTriggerTypeEnum.RETRY.getCode();
        }
        return context.getTrigger() instanceof CronTrigger
            ? JobTriggerTypeEnum.CRON.getCode() : JobTriggerTypeEnum.MANUAL.getCode();
    }

    private String resolveCron(JobExecutionContext context) {
        if (context.getTrigger() instanceof CronTrigger cronTrigger) {
            return cronTrigger.getCronExpression();
        }
        return null;
    }

    private static String resolveServerNode() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

}
