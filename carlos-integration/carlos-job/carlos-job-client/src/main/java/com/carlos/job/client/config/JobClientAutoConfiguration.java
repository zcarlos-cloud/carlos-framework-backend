package com.carlos.job.client.config;

import cn.hutool.core.util.StrUtil;
import com.carlos.job.client.controller.JobExecuteController;
import com.carlos.job.client.handler.JobHandler;
import com.carlos.job.client.handler.JobHandlerRegistry;
import com.carlos.job.client.processor.JobAnnotationBeanPostProcessor;
import com.carlos.job.client.register.JobAdminAddressResolver;
import com.carlos.job.client.register.JobAutoRegisterRunner;
import com.carlos.job.client.register.JobRegisterClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Map;

/**
 * <p>
 * 任务客户端自动装配（业务模块引入 carlos-job-client 即接入调度中心）
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = JobClientProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JobClientProperties.class)
public class JobClientAutoConfiguration {

    /**
     * 注解任务处理器扫描器
     *
     * <p>必须是 {@code static}：BPP 属于基础设施 Bean，非 static 方法会让所在自动配置类被提前实例化，
     * 触发配置类的依赖提前初始化。</p>
     */
    @Bean
    public static JobAnnotationBeanPostProcessor jobAnnotationBeanPostProcessor() {
        return new JobAnnotationBeanPostProcessor();
    }

    @Bean
    public JobHandlerRegistry jobHandlerRegistry(Map<String, JobHandler> handlerBeans,
                                                 JobAnnotationBeanPostProcessor annotationProcessor) {
        return new JobHandlerRegistry(handlerBeans, annotationProcessor.annotatedHandlers());
    }

    /**
     * 调度中心回调端点
     * <p>
     * {@code @ConditionalOnMissingBean} 是为了与组件扫描共存：{@link JobExecuteController} 上带
     * {@code @RestController}，宿主扫描根较宽时会先扫到它并注册一份。自动配置晚于组件扫描，
     * 据此让路，避免同名 Bean 冲突（overriding 默认 false）。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    public JobExecuteController jobExecuteController(JobHandlerRegistry handlerRegistry,
                                                     JobClientProperties clientProperties) {
        if (StrUtil.isBlank(clientProperties.getAccessToken())) {
            log.warn("============================================================");
            log.warn("carlos.job.client.access-token 未配置，/inner/job/execute 无回调鉴权！");
            log.warn("该端点虽不经过网关，但集群内任意服务均可直连，生产环境必须配置令牌");
            log.warn("============================================================");
        }
        return new JobExecuteController(handlerRegistry, clientProperties);
    }

    /**
     * 注解任务自动注册相关组件（未配置 admin-url 时 Runner 自行跳过，只是不发起出站调用）
     */
    @Bean
    @ConditionalOnProperty(prefix = JobClientProperties.PREFIX, name = "auto-register",
        havingValue = "true", matchIfMissing = true)
    public JobAdminAddressResolver jobAdminAddressResolver(JobClientProperties clientProperties) {
        return new JobAdminAddressResolver(clientProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = JobClientProperties.PREFIX, name = "auto-register",
        havingValue = "true", matchIfMissing = true)
    public JobRegisterClient jobRegisterClient(JobClientProperties clientProperties) {
        return new JobRegisterClient(clientProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = JobClientProperties.PREFIX, name = "auto-register",
        havingValue = "true", matchIfMissing = true)
    public JobAutoRegisterRunner jobAutoRegisterRunner(JobAnnotationBeanPostProcessor annotationProcessor,
                                                       JobAdminAddressResolver addressResolver,
                                                       JobRegisterClient registerClient,
                                                       JobClientProperties clientProperties) {
        return new JobAutoRegisterRunner(annotationProcessor, addressResolver, registerClient, clientProperties);
    }

}
