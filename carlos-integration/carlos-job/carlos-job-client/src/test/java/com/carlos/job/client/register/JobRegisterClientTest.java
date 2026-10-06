package com.carlos.job.client.register;

import com.carlos.core.exception.ComponentException;
import com.carlos.job.JobEndpointConstant;
import com.carlos.job.client.config.JobClientProperties;
import com.carlos.job.pojo.ao.JobRegisterAO;
import com.carlos.job.pojo.param.ApiJobRegisterParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * <p>
 * 调度中心注册客户端单元测试（MockRestServiceServer，不发真实请求）
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
class JobRegisterClientTest {

    private static final String ADMIN_URL = "http://127.0.0.1:9516";

    private MockRestServiceServer server;

    private JobRegisterClient registerClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        JobClientProperties properties = new JobClientProperties();
        properties.setAccessToken("job-token");
        registerClient = new JobRegisterClient(builder.build(), properties);
    }

    @Test
    @DisplayName("应该按内部端点路径与令牌发送注册请求")
    void should_post_to_register_endpoint_with_token() {
        server.expect(requestTo(ADMIN_URL + JobEndpointConstant.REGISTER_PATH))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(JobEndpointConstant.TOKEN_HEADER, "job-token"))
            .andExpect(jsonPath("$.jobName").value("orderSyncJob"))
            .andExpect(jsonPath("$.handlerName").value("orderSyncHandler"))
            .andExpect(jsonPath("$.scheduleConf").value("0 0 2 * * ?"))
            .andRespond(withSuccess("""
                {"success":true,"data":{"created":true,"existed":false,"drifted":false,
                 "status":"NORMAL","message":"任务已创建"}}
                """, MediaType.APPLICATION_JSON));

        JobRegisterAO result = registerClient.register(ADMIN_URL, buildParam());

        assertTrue(result.getCreated());
        assertEquals("NORMAL", result.getStatus());
        server.verify();
    }

    @Test
    @DisplayName("应该抛出组件异常当响应包装为失败时")
    void should_throw_when_envelope_failed() {
        server.expect(requestTo(ADMIN_URL + JobEndpointConstant.REGISTER_PATH))
            .andRespond(withSuccess("{\"success\":false,\"msg\":\"任务注册鉴权失败\"}", MediaType.APPLICATION_JSON));

        ApiJobRegisterParam param = buildParam();
        assertThrows(ComponentException.class, () -> registerClient.register(ADMIN_URL, param));
    }

    @Test
    @DisplayName("应该抛出组件异常当响应缺少结果数据时")
    void should_throw_when_result_data_missing() {
        server.expect(requestTo(ADMIN_URL + JobEndpointConstant.REGISTER_PATH))
            .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        ApiJobRegisterParam param = buildParam();
        assertThrows(ComponentException.class, () -> registerClient.register(ADMIN_URL, param));
    }

    @Test
    @DisplayName("应该抛出组件异常当响应体为空时")
    void should_throw_when_body_blank() {
        server.expect(requestTo(ADMIN_URL + JobEndpointConstant.REGISTER_PATH))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        ApiJobRegisterParam param = buildParam();
        assertThrows(ComponentException.class, () -> registerClient.register(ADMIN_URL, param));
    }

    @Test
    @DisplayName("应该原样抛出4xx响应异常以便调用方跳过重试")
    void should_propagate_client_error_when_http_4xx() {
        server.expect(requestTo(ADMIN_URL + JobEndpointConstant.REGISTER_PATH))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        ApiJobRegisterParam param = buildParam();
        RestClientResponseException exception = assertThrows(RestClientResponseException.class,
            () -> registerClient.register(ADMIN_URL, param));

        assertTrue(exception.getStatusCode().is4xxClientError());
    }

    private ApiJobRegisterParam buildParam() {
        return new ApiJobRegisterParam()
            .setJobName("orderSyncJob")
            .setJobGroup("ORDER")
            .setServiceName("carlos-order")
            .setHandlerName("orderSyncHandler")
            .setScheduleType(1)
            .setScheduleConf("0 0 2 * * ?");
    }

}
