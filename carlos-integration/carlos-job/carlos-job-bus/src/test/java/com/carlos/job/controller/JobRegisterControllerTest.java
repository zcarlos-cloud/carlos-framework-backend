package com.carlos.job.controller;

import com.carlos.job.JobEndpointConstant;
import com.carlos.job.config.JobProperties;
import com.carlos.job.pojo.dto.JobRegisterDTO;
import com.carlos.job.pojo.param.ApiJobRegisterParam;
import com.carlos.job.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <p>
 * 调度中心内部注册端点测试（standalone MockMvc，不启动 Spring 上下文）
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@ExtendWith(MockitoExtension.class)
class JobRegisterControllerTest {

    private static final String TOKEN = "job-inner-token";

    private static final String REGISTER_PATH = JobEndpointConstant.REGISTER_PATH;

    private static final String BODY = """
        {"jobName":"orderSyncJob","serviceName":"carlos-order","handlerName":"orderSyncHandler",
         "scheduleType":1,"scheduleConf":"0 0 2 * * ?","description":"订单同步"}
        """;

    @Mock
    private JobService jobService;

    private JobProperties jobProperties;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jobProperties = new JobProperties();
        jobProperties.setAccessToken(TOKEN);
        mockMvc = MockMvcBuilders.standaloneSetup(new JobRegisterController(jobService, jobProperties)).build();
    }

    @Test
    @DisplayName("应该返回注册结果当令牌匹配时")
    void should_register_when_token_matches() throws Exception {
        when(jobService.registerIfAbsent(any(ApiJobRegisterParam.class))).thenReturn(new JobRegisterDTO()
            .setCreated(Boolean.TRUE)
            .setExisted(Boolean.FALSE)
            .setDrifted(Boolean.FALSE)
            .setStatus("NORMAL")
            .setMessage("任务已创建"));

        mockMvc.perform(post(REGISTER_PATH)
                .header(JobEndpointConstant.TOKEN_HEADER, TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.created").value(true))
            .andExpect(jsonPath("$.data.status").value("NORMAL"));

        verify(jobService).registerIfAbsent(any(ApiJobRegisterParam.class));
    }

    @Test
    @DisplayName("应该拒绝注册当令牌不匹配时")
    void should_reject_when_token_mismatch() throws Exception {
        mockMvc.perform(post(REGISTER_PATH)
                .header(JobEndpointConstant.TOKEN_HEADER, "wrong-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false));

        verify(jobService, never()).registerIfAbsent(any(ApiJobRegisterParam.class));
    }

    @Test
    @DisplayName("应该拒绝注册当令牌缺失时")
    void should_reject_when_token_missing() throws Exception {
        mockMvc.perform(post(REGISTER_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false));

        verify(jobService, never()).registerIfAbsent(any(ApiJobRegisterParam.class));
    }

    @Test
    @DisplayName("应该放行注册当未配置令牌时")
    void should_allow_register_when_token_not_configured() throws Exception {
        jobProperties.setAccessToken(null);
        when(jobService.registerIfAbsent(any(ApiJobRegisterParam.class)))
            .thenReturn(new JobRegisterDTO().setCreated(Boolean.TRUE).setStatus("NORMAL"));

        mockMvc.perform(post(REGISTER_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        verify(jobService).registerIfAbsent(any(ApiJobRegisterParam.class));
    }

    @Test
    @DisplayName("应该返回400当任务名称为空时")
    void should_return_400_when_job_name_blank() throws Exception {
        mockMvc.perform(post(REGISTER_PATH)
                .header(JobEndpointConstant.TOKEN_HEADER, TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serviceName\":\"carlos-order\",\"handlerName\":\"orderSyncHandler\"}"))
            .andExpect(status().isBadRequest());

        verify(jobService, never()).registerIfAbsent(any(ApiJobRegisterParam.class));
    }

    @Test
    @DisplayName("应该返回400当处理器名称含非法字符时")
    void should_return_400_when_handler_name_invalid() throws Exception {
        mockMvc.perform(post(REGISTER_PATH)
                .header(JobEndpointConstant.TOKEN_HEADER, TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jobName\":\"orderSyncJob\",\"serviceName\":\"carlos-order\","
                    + "\"handlerName\":\"order sync handler\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("应该返回400当初始状态取值非法时")
    void should_return_400_when_status_invalid() throws Exception {
        mockMvc.perform(post(REGISTER_PATH)
                .header(JobEndpointConstant.TOKEN_HEADER, TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jobName\":\"orderSyncJob\",\"serviceName\":\"carlos-order\","
                    + "\"handlerName\":\"orderSyncHandler\",\"status\":\"RUNNING\"}"))
            .andExpect(status().isBadRequest());
    }

}
