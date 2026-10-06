package com.carlos.job.client.config;

import com.carlos.job.client.annotation.AutoRegisterJob;
import com.carlos.job.client.annotation.ScheduleJob;
import com.carlos.job.client.handler.JobHandler;
import com.carlos.job.client.handler.JobHandlerRegistry;
import com.carlos.job.client.handler.JobResult;
import com.carlos.job.client.processor.JobAnnotationBeanPostProcessor;
import com.carlos.job.client.register.JobAdminAddressResolver;
import com.carlos.job.client.register.JobAutoRegisterPlan;
import com.carlos.job.client.register.JobAutoRegisterRunner;
import com.carlos.job.client.register.JobRegisterClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * 任务客户端自动装配集成测试（真实 Spring 上下文，不起 web 环境）
 * </p>
 * <p>
 * 覆盖单测覆盖不到的那一段：BPP 是否真的被 Spring 回调、注册表是否在
 * {@code afterSingletonsInstantiated} 里合并了两类来源、条件装配是否按开关生效。
 * 出站 HTTP 由 {@code JobRegisterClientTest} 用 MockRestServiceServer 单独覆盖。
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
class JobClientAutoConfigurationTest {

    private static final String APP_NAME = "ctx-test-app";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JobClientAutoConfiguration.class))
        .withUserConfiguration(SampleJobConfig.class)
        .withPropertyValues("spring.application.name=" + APP_NAME);

    @Test
    @DisplayName("应该在真实上下文中登记注解处理器并装配自动注册组件")
    void should_register_annotated_handler_and_auto_register_beans() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            JobHandlerRegistry registry = context.getBean(JobHandlerRegistry.class);
            // 注解来源与接口来源共存
            assertThat(registry.getHandler("sampleAnnotatedHandler")).isNotNull();
            assertThat(registry.getHandler("interfaceSampleHandler")).isNotNull();
            assertThat(context).hasSingleBean(JobAdminAddressResolver.class);
            assertThat(context).hasSingleBean(JobRegisterClient.class);
            assertThat(context).hasSingleBean(JobAutoRegisterRunner.class);
        });
    }

    @Test
    @DisplayName("应该通过注册表执行注解方法并把参数透传给目标Bean")
    void should_execute_annotated_method_through_registry() {
        contextRunner.run(context -> {
            JobHandler handler = context.getBean(JobHandlerRegistry.class).getHandler("sampleAnnotatedHandler");
            JobResult result = executeQuietly(handler, "{\"orderId\":7}");

            assertThat(result.getSuccess()).isTrue();
            assertThat(result.getMessage()).isEqualTo("注解任务执行成功");
            assertThat(context.getBean(SampleAnnotatedJob.class).getReceived()).isEqualTo("{\"orderId\":7}");
        });
    }

    @Test
    @DisplayName("应该收集注册计划并用应用名回退目标服务名")
    void should_collect_plans_with_application_name_fallback() {
        contextRunner.run(context -> {
            Map<String, JobAutoRegisterPlan> plans =
                context.getBean(JobAnnotationBeanPostProcessor.class).autoRegisterPlans();

            assertThat(plans).containsKeys("sampleAnnotatedHandler", "fallbackHandler");
            assertThat(plans.get("sampleAnnotatedHandler").getServiceName()).isEqualTo("carlos-sample");
            assertThat(plans.get("fallbackHandler").getServiceName()).isEqualTo(APP_NAME);
            assertThat(plans.get("fallbackHandler").getJobName()).isEqualTo("fallbackHandler");
            assertThat(plans.get("fallbackHandler").getScheduleConf()).isEqualTo("0 0 4 * * ?");
        });
    }

    @Test
    @DisplayName("应该只关闭自动注册组件当开关关闭时")
    void should_disable_only_auto_register_when_switch_off() {
        contextRunner.withPropertyValues("carlos.job.client.auto-register=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(JobAutoRegisterRunner.class);
            assertThat(context).doesNotHaveBean(JobRegisterClient.class);
            // 处理器登记不依赖自动注册开关
            assertThat(context.getBean(JobHandlerRegistry.class).getHandler("sampleAnnotatedHandler")).isNotNull();
        });
    }

    @Test
    @DisplayName("应该整体不装配当客户端开关关闭时")
    void should_not_configure_when_client_disabled() {
        contextRunner.withPropertyValues("carlos.job.client.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(JobHandlerRegistry.class);
            assertThat(context).doesNotHaveBean(JobAutoRegisterRunner.class);
        });
    }

    private static JobResult executeQuietly(JobHandler handler, String params) {
        try {
            return handler.execute(params);
        } catch (Exception e) {
            throw new IllegalStateException("注解处理器执行异常", e);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class SampleJobConfig {
        @Bean
        SampleAnnotatedJob sampleAnnotatedJob() {
            return new SampleAnnotatedJob();
        }

        @Bean
        FallbackAnnotatedJob fallbackAnnotatedJob() {
            return new FallbackAnnotatedJob();
        }

        @Bean
        InterfaceSampleHandler interfaceSampleHandler() {
            return new InterfaceSampleHandler();
        }

    }

    /**
     * 注解式：显式处理器名 + 显式服务名 + JobResult 返回
     */
    static class SampleAnnotatedJob {

        private String received;

        public String getReceived() {
            return received;
        }

        @ScheduleJob("sampleAnnotatedHandler")
        @AutoRegisterJob(cron = "0 0 3 * * ?", serviceName = "carlos-sample", description = "注解示例")
        public JobResult sync(String params) {
            this.received = params;
            return JobResult.ok("注解任务执行成功");
        }

    }

    /**
     * 注解式：处理器名回退方法名、服务名回退 spring.application.name、cron 走 scheduleConf 兼容字段
     */
    static class FallbackAnnotatedJob {

        @ScheduleJob
        @AutoRegisterJob(cron = "0 0 4 * * ?")
        public void fallbackHandler() {
        }

    }

    /**
     * 接口式：与注解式共存，验证两类来源可同时登记
     */
    static class InterfaceSampleHandler implements JobHandler {

        @Override
        public JobResult execute(String params) {
            return JobResult.ok();
        }

    }

}
