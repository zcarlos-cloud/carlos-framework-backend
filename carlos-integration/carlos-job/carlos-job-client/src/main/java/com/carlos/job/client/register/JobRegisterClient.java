package com.carlos.job.client.register;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.carlos.core.exception.ComponentException;
import com.carlos.job.JobEndpointConstant;
import com.carlos.job.client.config.JobClientProperties;
import com.carlos.job.pojo.ao.JobRegisterAO;
import com.carlos.job.pojo.param.ApiJobRegisterParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * <p>
 * 调度中心注册客户端：{@code POST /inner/job/register}（携带 {@code X-Job-Token}）
 * </p>
 *
 * <p>与 {@code JobDispatcher} 的回调方向对称。不引 Feign 是刻意的：引 Feign 会把
 * "加一个依赖" 变成 "加依赖 + {@code @EnableFeignClients}"，且它的降级工厂会把注册失败藏起来。</p>
 *
 * <p>解析统一响应走 Fastjson2（框架自有 JSON 实现），不依赖 Jackson ——
 * carlos-job-client 的类路径上没有 Jackson 2，Spring Boot 4 的 MVC 用的是 Jackson 3，
 * 业务服务里两者也未必同时存在。</p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Slf4j
public class JobRegisterClient {

    private final RestClient restClient;

    private final String accessToken;

    public JobRegisterClient(JobClientProperties clientProperties) {
        this(buildRestClient(clientProperties), clientProperties);
    }

    JobRegisterClient(RestClient restClient, JobClientProperties clientProperties) {
        this.restClient = restClient;
        this.accessToken = StrUtil.blankToDefault(clientProperties.getAccessToken(), "");
    }

    /**
     * 向调度中心幂等注册任务
     *
     * @param adminUrl 调度中心基础地址（形如 {@code http://host:port}）
     * @param param    注册参数
     * @return 注册结果
     * @throws org.springframework.web.client.RestClientResponseException HTTP 状态非 2xx（4xx 重试无意义）
     * @throws ComponentException 响应体非成功包装或无法解析
     */
    public JobRegisterAO register(String adminUrl, ApiJobRegisterParam param) {
        String uri = adminUrl + JobEndpointConstant.REGISTER_PATH;
        // 用 Fastjson2 自己序列化并原样交给 RestClient：不依赖运行环境里的消息转换器与 Jackson 命名策略
        // （业务服务若配置了全局 snake_case 等策略，委托转换器会把字段名改掉导致服务端 400）
        String body = restClient.post()
            .uri(uri)
            .header(JobEndpointConstant.TOKEN_HEADER, accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(JSON.toJSONString(param))
            .retrieve()
            .body(String.class);
        return parseResult(body, param.getJobName());
    }

    /**
     * 解析统一响应 {@code Result<JobRegisterAO>}
     */
    private JobRegisterAO parseResult(String body, String jobName) {
        if (StrUtil.isBlank(body)) {
            throw new ComponentException("任务注册响应为空: " + jobName);
        }
        JSONObject root;
        try {
            root = JSON.parseObject(body);
        } catch (RuntimeException e) {
            throw new ComponentException("任务注册响应解析失败: " + jobName + ", body=" + body, e);
        }
        if (root == null || !Boolean.TRUE.equals(root.getBoolean("success"))) {
            throw new ComponentException("任务注册失败: " + jobName + ", msg="
                + (root == null ? body : root.getString("msg")));
        }
        JSONObject data = root.getJSONObject("data");
        if (data == null || data.isEmpty()) {
            throw new ComponentException("任务注册响应缺少结果数据: " + jobName);
        }
        try {
            return data.toJavaObject(JobRegisterAO.class);
        } catch (RuntimeException e) {
            throw new ComponentException("任务注册结果解析失败: " + jobName, e);
        }
    }

    private static RestClient buildRestClient(JobClientProperties clientProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(clientProperties.getRegisterConnectTimeoutMs());
        requestFactory.setReadTimeout(clientProperties.getRegisterReadTimeoutMs());
        return RestClient.builder().requestFactory(requestFactory).build();
    }

}
