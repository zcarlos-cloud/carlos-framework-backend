package com.carlos.job.client.processor;

import com.carlos.core.exception.ComponentException;
import com.carlos.job.client.annotation.AutoRegisterJob;
import com.carlos.job.client.annotation.ScheduleJob;
import com.carlos.job.client.handler.JobHandler;
import com.carlos.job.client.handler.JobResult;
import com.carlos.job.client.register.JobAutoRegisterPlan;
import com.carlos.job.pojo.param.ApiJobRegisterParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>
 * 注解任务处理器扫描器单元测试
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
class JobAnnotationBeanPostProcessorTest {

    private static final String APP_NAME = "carlos-job-test";

    private DefaultListableBeanFactory beanFactory;

    private StandardEnvironment environment;

    private JobAnnotationBeanPostProcessor processor;

    @BeforeEach
    void setUp() {
        beanFactory = new DefaultListableBeanFactory();
        environment = new StandardEnvironment();
        TestPropertyValues.of("spring.application.name=" + APP_NAME).applyTo(environment);
        processor = newProcessor();
    }

    @Test
    @DisplayName("应该按注解声明值登记处理器")
    void should_register_handler_with_declared_name() {
        scan("namedJob", new NamedJob());

        JobHandler handler = processor.annotatedHandlers().get("namedHandler");
        assertNotNull(handler);
        assertEquals("namedHandler", handler.name());
    }

    @Test
    @DisplayName("应该忽略同一方法的重复扫描而不判为名称冲突")
    void should_ignore_duplicate_scan_of_same_method() {
        NamedJob job = new NamedJob();
        scan("namedJob", job);
        // 原型作用域 Bean 的每个实例都会走一次 BPP，@RefreshScope 的代理与目标类也各走一次；
        // 位置相同即同一次声明，不能判成"自己与自己冲突"而打死启动
        processor.postProcessAfterInitialization(job, "namedJob");

        assertNotNull(processor.annotatedHandlers().get("namedHandler"));
    }

    @Test
    @DisplayName("应该回退到方法名当注解取值为空时")
    void should_fall_back_to_method_name_when_value_blank() {
        scan("defaultNameJob", new DefaultNameJob());

        assertNotNull(processor.annotatedHandlers().get("everyMinute"));
    }

    @Test
    @DisplayName("应该登记注册计划并回退任务名与服务名")
    void should_build_register_plan_when_auto_register_annotated() {
        scan("namedJob", new NamedJob());

        JobAutoRegisterPlan plan = processor.autoRegisterPlans().get("namedHandler");
        assertNotNull(plan);
        // 任务名回退处理器名；服务名取本机应用名；cron 落到 scheduleConf
        assertEquals("namedHandler", plan.getJobName());
        assertEquals(APP_NAME, plan.getServiceName());
        assertEquals(Integer.valueOf(1), plan.getScheduleType());
        assertEquals("0 0 2 * * ?", plan.getScheduleConf());
        assertEquals("订单同步", plan.getDescription());
    }

    @Test
    @DisplayName("应该优先使用scheduleConf当同时声明了cron时")
    void should_prefer_schedule_conf_over_cron() {
        scan("bothScheduleJob", new BothScheduleJob());

        assertEquals("30000", processor.autoRegisterPlans().get("bothScheduleHandler").getScheduleConf());
    }

    @Test
    @DisplayName("应该把哨兵值归零为null当转换为注册参数时")
    void should_convert_sentinel_to_null_when_building_register_param() {
        scan("namedJob", new NamedJob());

        ApiJobRegisterParam param = processor.autoRegisterPlans().get("namedHandler").toRegisterParam();

        assertEquals("namedHandler", param.getJobName());
        assertEquals(APP_NAME, param.getServiceName());
        assertNull(param.getJobGroup());
        assertNull(param.getMisfireStrategy());
        assertNull(param.getConcurrentMode());
        assertNull(param.getTimeoutSeconds());
        assertNull(param.getFailRetryCount());
        assertNull(param.getStatus());
    }

    @Test
    @DisplayName("应该使用注解显式指定的服务名当提供时")
    void should_use_declared_service_name_when_provided() {
        scan("remoteJob", new RemoteServiceJob());

        assertEquals("carlos-audit", processor.autoRegisterPlans().get("remoteHandler").getServiceName());
    }

    @Test
    @DisplayName("应该只登记处理器而不产生注册计划当只标注ScheduleJob时")
    void should_register_handler_without_plan_when_only_schedule_job_annotated() {
        scan("defaultNameJob", new DefaultNameJob());

        assertEquals(1, processor.annotatedHandlers().size());
        assertTrue(processor.autoRegisterPlans().isEmpty());
    }

    @Test
    @DisplayName("应该启动失败当方法为静态方法时")
    void should_fail_fast_when_method_is_static() {
        StaticMethodJob job = new StaticMethodJob();

        assertThrows(ComponentException.class, () -> scan("staticMethodJob", job));
    }

    @Test
    @DisplayName("应该启动失败当方法参数不合法时")
    void should_fail_fast_when_multi_param_signature() {
        MultiParamJob job = new MultiParamJob();

        assertThrows(ComponentException.class, () -> scan("multiParamJob", job));
    }

    @Test
    @DisplayName("应该启动失败当参数类型不是String时")
    void should_fail_fast_when_param_type_not_string() {
        NonStringParamJob job = new NonStringParamJob();

        assertThrows(ComponentException.class, () -> scan("nonStringParamJob", job));
    }

    @Test
    @DisplayName("应该启动失败当返回类型不受支持时")
    void should_fail_fast_when_return_type_unsupported() {
        BadReturnJob job = new BadReturnJob();

        assertThrows(ComponentException.class, () -> scan("badReturnJob", job));
    }

    @Test
    @DisplayName("应该启动失败当处理器名不合法时")
    void should_fail_fast_when_handler_name_invalid() {
        BadNameJob job = new BadNameJob();

        assertThrows(ComponentException.class, () -> scan("badNameJob", job));
    }

    @Test
    @DisplayName("应该启动失败当注解任务缺少调度配置时")
    void should_fail_fast_when_schedule_conf_missing() {
        NoScheduleJob job = new NoScheduleJob();

        assertThrows(ComponentException.class, () -> scan("noScheduleJob", job));
    }

    @Test
    @DisplayName("应该启动失败当无法解析目标服务名时")
    void should_fail_fast_when_service_name_unresolvable() {
        // 无 spring.application.name 且注解未显式指定 serviceName：留空会让调度中心在本机执行
        processor = newProcessor(new StandardEnvironment());

        assertThrows(ComponentException.class, () -> scan("namedJob", new NamedJob()));
    }

    @Test
    @DisplayName("应该允许仅登记处理器当未声明自动注册时无需目标服务名")
    void should_allow_register_without_service_name_when_auto_register_absent() {
        processor = newProcessor(new StandardEnvironment());

        scan("defaultNameJob", new DefaultNameJob());

        assertNotNull(processor.annotatedHandlers().get("everyMinute"));
    }

    @Test
    @DisplayName("应该启动失败当注解处理器名称冲突时")
    void should_fail_fast_when_handler_name_conflicts() {
        scan("firstJob", new FirstDuplicateJob());

        SecondDuplicateJob second = new SecondDuplicateJob();
        assertThrows(ComponentException.class, () -> scan("secondJob", second));
        // 保留先注册者
        assertNotNull(processor.annotatedHandlers().get("duplicateHandler"));
    }

    @Test
    @DisplayName("应该跳过非法声明并记录日志当关闭快速失败时")
    void should_skip_invalid_declaration_when_fail_fast_disabled() {
        TestPropertyValues.of("carlos.job.client.fail-fast-on-invalid=false").applyTo(environment);
        processor = newProcessor(environment);

        StaticMethodJob job = new StaticMethodJob();
        scan("staticMethodJob", job);

        assertTrue(processor.annotatedHandlers().isEmpty());
    }

    @Test
    @DisplayName("应该保留先注册者并跳过重复项当关闭快速失败时")
    void should_keep_first_when_duplicated_and_fail_fast_disabled() {
        TestPropertyValues.of("carlos.job.client.fail-fast-on-invalid=false").applyTo(environment);
        processor = newProcessor(environment);

        FirstDuplicateJob first = new FirstDuplicateJob();
        SecondDuplicateJob second = new SecondDuplicateJob();
        scan("firstJob", first);
        scan("secondJob", second);

        assertEquals(1, processor.annotatedHandlers().size());
        assertNotNull(processor.annotatedHandlers().get("duplicateHandler"));
    }

    @Test
    @DisplayName("应该登记方法上声明的处理器但保持调用语义当注解方法可执行时")
    void should_register_executable_handler_when_annotation_present() throws Exception {
        scan("namedJob", new NamedJob());

        JobHandler handler = processor.annotatedHandlers().get("namedHandler");
        JobResult result = handler.execute("{}");

        assertTrue(result.getSuccess());
    }

    @Test
    @DisplayName("应该不登记任何处理器当类上没有注解时")
    void should_not_register_when_no_annotation_present() {
        scan("plain", new PlainJob());

        assertTrue(processor.annotatedHandlers().isEmpty());
    }

    private JobAnnotationBeanPostProcessor newProcessor() {
        return newProcessor(environment);
    }

    private JobAnnotationBeanPostProcessor newProcessor(StandardEnvironment env) {
        JobAnnotationBeanPostProcessor postProcessor = new JobAnnotationBeanPostProcessor();
        postProcessor.setBeanFactory(beanFactory);
        postProcessor.setEnvironment(env);
        return postProcessor;
    }

    private void scan(String beanName, Object bean) {
        beanFactory.registerSingleton(beanName, bean);
        processor.postProcessAfterInitialization(bean, beanName);
    }

    /**
     * 显式命名 + 完整自动注册声明
     */
    static class NamedJob {

        @ScheduleJob("namedHandler")
        @AutoRegisterJob(cron = "0 0 2 * * ?", description = "订单同步", params = "{\"a\":1}")
        public void sync(String params) {
        }

    }

    /**
     * 处理器名回退方法名，不带自动注册
     */
    static class DefaultNameJob {

        @ScheduleJob
        public void everyMinute() {
        }

    }

    /**
     * scheduleConf 与 cron 同时声明
     */
    static class BothScheduleJob {

        @ScheduleJob("bothScheduleHandler")
        @AutoRegisterJob(cron = "0 0 2 * * ?", scheduleConf = "30000", scheduleType = 2)
        public void interval() {
        }

    }

    /**
     * 显式声明目标服务名
     */
    static class RemoteServiceJob {

        @ScheduleJob("remoteHandler")
        @AutoRegisterJob(cron = "0 0 3 * * ?", serviceName = "carlos-audit")
        public void archive() {
        }

    }

    static class PlainJob {

        public void nothing() {
        }

    }

    static class StaticMethodJob {

        @ScheduleJob
        public static void staticHandler() {
        }

    }

    static class MultiParamJob {

        @ScheduleJob
        public void multi(String first, String second) {
        }

    }

    static class NonStringParamJob {

        @ScheduleJob
        public void nonString(Integer value) {
        }

    }

    static class BadReturnJob {

        @ScheduleJob
        public String badReturn() {
            return "x";
        }

    }

    static class BadNameJob {

        @ScheduleJob("bad name!")
        public void invalidName() {
        }

    }

    static class NoScheduleJob {

        @ScheduleJob("noScheduleHandler")
        @AutoRegisterJob(description = "缺调度配置")
        public void noSchedule() {
        }

    }

    static class FirstDuplicateJob {

        @ScheduleJob("duplicateHandler")
        public void first() {
        }

    }

    static class SecondDuplicateJob {

        @ScheduleJob("duplicateHandler")
        public void second() {
        }

    }

}
