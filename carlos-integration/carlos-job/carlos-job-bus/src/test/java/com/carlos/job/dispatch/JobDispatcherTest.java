package com.carlos.job.dispatch;

import com.carlos.core.exception.BusinessException;
import com.carlos.job.client.handler.JobHandler;
import com.carlos.job.client.handler.JobHandlerRegistry;
import com.carlos.job.client.handler.JobResult;
import com.carlos.job.config.JobProperties;
import com.carlos.job.enums.JobErrorCode;
import com.carlos.job.pojo.ao.JobResultAO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.mock.env.MockEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 任务派发器单元测试
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@ExtendWith(MockitoExtension.class)
class JobDispatcherTest {

    @Mock
    private ObjectProvider<JobHandlerRegistry> localRegistryProvider;

    @Mock
    private ObjectProvider<LoadBalancerClient> loadBalancerClientProvider;

    @Mock
    private ObjectProvider<ObjectMapper> objectMapperProvider;

    private MockEnvironment environment;

    private JobDispatcher jobDispatcher;

    private JobHandlerRegistry localRegistry;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment().withProperty("spring.application.name", "carlos-job");
        Map<String, JobHandler> beans = new HashMap<>();
        beans.put("okHandler", params -> JobResult.ok("done"));
        beans.put("failHandler", params -> JobResult.fail("biz fail"));
        beans.put("errorHandler", params -> {
            throw new IllegalStateException("boom");
        });
        localRegistry = new JobHandlerRegistry(beans);
        localRegistry.afterSingletonsInstantiated();
        lenient().when(localRegistryProvider.getIfAvailable()).thenReturn(localRegistry);
        lenient().when(objectMapperProvider.getIfAvailable(any())).thenReturn(new ObjectMapper());
        jobDispatcher = new JobDispatcher(localRegistryProvider, loadBalancerClientProvider,
            environment, new JobProperties(), objectMapperProvider);
    }

    @Test
    @DisplayName("应该本机执行成功当服务名为本应用且处理器存在时")
    void should_execute_locally_when_service_is_local_app() {
        JobResultAO result = jobDispatcher.dispatch("carlos-job", "okHandler", "{}");

        assertTrue(result.getSuccess());
        assertEquals("done", result.getMessage());
    }

    @Test
    @DisplayName("应该返回失败结果当处理器业务返回失败时")
    void should_return_fail_result_when_handler_returns_fail() {
        JobResultAO result = jobDispatcher.dispatch("carlos-job", "failHandler", null);

        assertFalse(result.getSuccess());
        assertEquals("biz fail", result.getMessage());
    }

    @Test
    @DisplayName("应该返回失败结果当处理器抛出异常时")
    void should_return_fail_result_when_handler_throws() {
        JobResultAO result = jobDispatcher.dispatch("carlos-job", "errorHandler", null);

        assertFalse(result.getSuccess());
        assertTrue(result.getMessage().contains("boom"));
    }

    @Test
    @DisplayName("应该抛出处理器不存在异常当无服务发现且本机无处理器时")
    void should_throw_handler_not_found_when_no_lb_and_no_local_handler() {
        when(loadBalancerClientProvider.getIfAvailable()).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> jobDispatcher.dispatch("carlos-message", "notExistsHandler", null));

        assertEquals(JobErrorCode.JOB_HANDLER_NOT_FOUND.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("应该抛出服务不可用异常当目标服务无实例时")
    void should_throw_service_unavailable_when_no_instance() {
        LoadBalancerClient loadBalancerClient = mock(LoadBalancerClient.class);
        when(loadBalancerClient.choose("carlos-message")).thenReturn(null);
        when(loadBalancerClientProvider.getIfAvailable()).thenReturn(loadBalancerClient);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> jobDispatcher.dispatch("carlos-message", "anyHandler", null));

        assertEquals(JobErrorCode.JOB_SERVICE_UNAVAILABLE.getCode(), exception.getErrorCode());
    }

}
