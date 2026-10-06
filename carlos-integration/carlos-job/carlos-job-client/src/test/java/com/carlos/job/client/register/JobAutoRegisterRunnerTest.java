package com.carlos.job.client.register;

import com.carlos.core.exception.ComponentException;
import com.carlos.job.client.config.JobClientProperties;
import com.carlos.job.client.processor.JobAnnotationBeanPostProcessor;
import com.carlos.job.pojo.ao.JobRegisterAO;
import com.carlos.job.pojo.param.ApiJobRegisterParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 注解任务启动自动注册单元测试
 * </p>
 * <p>
 * 用同线程 Executor 注入，把异步注册变成确定性断言。
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@ExtendWith(MockitoExtension.class)
class JobAutoRegisterRunnerTest {

    private static final String ADMIN_URL = "http://127.0.0.1:9516";

    @Mock
    private JobAnnotationBeanPostProcessor annotationProcessor;

    @Mock
    private JobAdminAddressResolver addressResolver;

    @Mock
    private JobRegisterClient registerClient;

    private JobAutoRegisterRunner runner;

    @BeforeEach
    void setUp() {
        JobClientProperties properties = new JobClientProperties();
        properties.setRegisterMaxAttempts(3);
        properties.setRegisterRetryBackoffMs(0);
        runner = new JobAutoRegisterRunner(annotationProcessor, addressResolver, registerClient, properties,
            Runnable::run);
    }

    @Test
    @DisplayName("应该不做任何出站调用当没有注册计划时")
    void should_skip_when_no_plans() {
        when(annotationProcessor.autoRegisterPlans()).thenReturn(Map.of());

        runner.onApplicationEvent(mock(ApplicationReadyEvent.class));

        verifyNoInteractions(addressResolver, registerClient);
    }

    @Test
    @DisplayName("应该跳过注册并保持启动成功当未配置调度中心地址时")
    void should_skip_when_admin_url_absent() {
        when(annotationProcessor.autoRegisterPlans()).thenReturn(plans(plan("orderSyncJob")));
        when(addressResolver.resolve()).thenReturn(null);

        runner.onApplicationEvent(mock(ApplicationReadyEvent.class));

        verify(registerClient, never()).register(anyString(), any(ApiJobRegisterParam.class));
    }

    @Test
    @DisplayName("应该按解析出的地址注册全部计划当应用就绪时")
    void should_register_plans_when_ready() {
        when(annotationProcessor.autoRegisterPlans()).thenReturn(plans(plan("orderSyncJob")));
        when(addressResolver.resolve()).thenReturn(ADMIN_URL);
        when(registerClient.register(anyString(), any(ApiJobRegisterParam.class)))
            .thenReturn(new JobRegisterAO().setCreated(Boolean.TRUE).setStatus("NORMAL"));

        runner.onApplicationEvent(mock(ApplicationReadyEvent.class));

        verify(registerClient).register(eq(ADMIN_URL),
            argThat(param -> "orderSyncJob".equals(param.getJobName()) && "carlos-order".equals(param.getServiceName())));
    }

    @Test
    @DisplayName("应该重试后成功当首轮为瞬时故障时")
    void should_retry_then_succeed_when_transient_error() {
        when(annotationProcessor.autoRegisterPlans()).thenReturn(plans(plan("orderSyncJob")));
        when(addressResolver.resolve()).thenReturn(ADMIN_URL);
        when(registerClient.register(anyString(), any(ApiJobRegisterParam.class)))
            .thenThrow(new RestClientException("io error"))
            .thenReturn(new JobRegisterAO().setCreated(Boolean.TRUE).setStatus("NORMAL"));

        runner.onApplicationEvent(mock(ApplicationReadyEvent.class));

        verify(registerClient, times(2)).register(anyString(), any(ApiJobRegisterParam.class));
    }

    @Test
    @DisplayName("应该不重试当响应为4xx时")
    void should_not_retry_when_client_error() {
        when(annotationProcessor.autoRegisterPlans()).thenReturn(plans(plan("orderSyncJob")));
        when(addressResolver.resolve()).thenReturn(ADMIN_URL);
        when(registerClient.register(anyString(), any(ApiJobRegisterParam.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        runner.onApplicationEvent(mock(ApplicationReadyEvent.class));

        verify(registerClient, times(1)).register(anyString(), any(ApiJobRegisterParam.class));
    }

    @Test
    @DisplayName("应该不重试当响应体明确拒绝时")
    void should_not_retry_when_rejected_by_envelope() {
        when(annotationProcessor.autoRegisterPlans()).thenReturn(plans(plan("orderSyncJob")));
        when(addressResolver.resolve()).thenReturn(ADMIN_URL);
        // 鉴权不通过时服务端返回的是 200 + success=false，走的是这条异常路径而非 4xx
        when(registerClient.register(anyString(), any(ApiJobRegisterParam.class)))
            .thenThrow(new ComponentException("任务注册鉴权失败"));

        runner.onApplicationEvent(mock(ApplicationReadyEvent.class));

        verify(registerClient, times(1)).register(anyString(), any(ApiJobRegisterParam.class));
    }

    @Test
    @DisplayName("应该在达到最大尝试次数后放弃且不抛出异常")
    void should_stop_after_max_attempts() {
        when(annotationProcessor.autoRegisterPlans()).thenReturn(plans(plan("orderSyncJob")));
        when(addressResolver.resolve()).thenReturn(ADMIN_URL);
        when(registerClient.register(anyString(), any(ApiJobRegisterParam.class)))
            .thenThrow(new RestClientException("io error"));

        runner.onApplicationEvent(mock(ApplicationReadyEvent.class));

        verify(registerClient, times(3)).register(anyString(), any(ApiJobRegisterParam.class));
    }

    @Test
    @DisplayName("应该继续注册其余任务当其中一个失败时")
    void should_continue_when_one_plan_fails() {
        when(registerClient.register(anyString(), any(ApiJobRegisterParam.class))).thenAnswer(invocation -> {
            ApiJobRegisterParam param = invocation.getArgument(1);
            if ("badJob".equals(param.getJobName())) {
                throw new RestClientException("io error");
            }
            return new JobRegisterAO().setCreated(Boolean.TRUE).setStatus("NORMAL");
        });

        runner.registerAll(ADMIN_URL, List.of(plan("badJob"), plan("goodJob")));

        verify(registerClient, times(3)).register(eq(ADMIN_URL), argThat(param -> "badJob".equals(param.getJobName())));
        verify(registerClient, times(1)).register(eq(ADMIN_URL), argThat(param -> "goodJob".equals(param.getJobName())));
    }

    @Test
    @DisplayName("应该把漂移态计入汇总而不是失败")
    void should_count_drift_result() {
        when(annotationProcessor.autoRegisterPlans()).thenReturn(plans(plan("orderSyncJob")));
        when(addressResolver.resolve()).thenReturn(ADMIN_URL);
        when(registerClient.register(anyString(), any(ApiJobRegisterParam.class)))
            .thenReturn(new JobRegisterAO().setCreated(Boolean.FALSE).setDrifted(Boolean.TRUE));

        runner.onApplicationEvent(mock(ApplicationReadyEvent.class));

        verify(registerClient, times(1)).register(anyString(), any(ApiJobRegisterParam.class));
    }

    @Test
    @DisplayName("应该吞掉异常当收集注册计划失败时")
    void should_not_propagate_when_plan_collection_throws() {
        // 监听器抛出的异常会顺着 ApplicationReadyEvent 冒到 SpringApplication.run 并**真的终止启动**，
        // 与"注册不阻断业务服务启动"的契约相悖，故必须被吞掉
        when(annotationProcessor.autoRegisterPlans()).thenThrow(new IllegalStateException("boom"));

        assertDoesNotThrow(() -> runner.onApplicationEvent(mock(ApplicationReadyEvent.class)));
    }

    @Test
    @DisplayName("应该吞掉异常当解析调度中心地址失败时")
    void should_not_propagate_when_address_resolve_throws() {
        when(annotationProcessor.autoRegisterPlans()).thenReturn(plans(plan("orderSyncJob")));
        when(addressResolver.resolve()).thenThrow(new IllegalStateException("boom"));

        assertDoesNotThrow(() -> runner.onApplicationEvent(mock(ApplicationReadyEvent.class)));
    }

    @Test
    @DisplayName("应该区分连通性问题与契约问题来描述注册失败")
    void should_describe_failure_by_kind() {
        // 连不上调度中心曾被误读成"应用启动失败"，分类描述就是为消除这个歧义
        assertTrue(JobAutoRegisterRunner.describeFailure(new ResourceAccessException("io error"))
            .contains("调度中心不可达"));
        assertTrue(JobAutoRegisterRunner.describeFailure(new HttpClientErrorException(HttpStatus.BAD_REQUEST))
            .contains("注册请求被拒绝"));
        assertTrue(JobAutoRegisterRunner.describeFailure(new ComponentException("任务注册鉴权失败"))
            .contains("注册被调度中心拒绝"));
        assertTrue(JobAutoRegisterRunner.describeFailure(new IllegalStateException("boom"))
            .contains("注册异常"));
    }

    private Map<String, JobAutoRegisterPlan> plans(JobAutoRegisterPlan... plans) {
        Map<String, JobAutoRegisterPlan> map = new LinkedHashMap<>();
        for (JobAutoRegisterPlan plan : plans) {
            map.put(plan.getHandlerName(), plan);
        }
        return map;
    }

    private JobAutoRegisterPlan plan(String jobName) {
        return new JobAutoRegisterPlan()
            .setHandlerName(jobName + "Handler")
            .setJobName(jobName)
            .setServiceName("carlos-order")
            .setScheduleType(1)
            .setScheduleConf("0 0 2 * * ?");
    }

}
