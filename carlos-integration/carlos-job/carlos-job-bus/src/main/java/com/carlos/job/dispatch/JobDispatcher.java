package com.carlos.job.dispatch;

import cn.hutool.core.util.StrUtil;
import com.carlos.job.JobEndpointConstant;
import com.carlos.job.client.handler.JobHandler;
import com.carlos.job.client.handler.JobHandlerRegistry;
import com.carlos.job.client.handler.JobResult;
import com.carlos.job.config.JobProperties;
import com.carlos.job.enums.JobErrorCode;
import com.carlos.job.pojo.ao.JobResultAO;
import com.carlos.job.pojo.param.ApiJobExecuteParam;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;

/**
 * <p>
 * 任务派发器：优先本机执行（boot 单体模式），否则通过服务发现回调目标业务服务
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Slf4j
@Component
public class JobDispatcher {

    /**
     * 业务端内部执行端点路径
     *
     * @deprecated 改用 {@link JobEndpointConstant#EXECUTE_PATH}，保留仅为源码兼容
     */
    @Deprecated
    public static final String EXECUTE_PATH = JobEndpointConstant.EXECUTE_PATH;

    private final ObjectProvider<JobHandlerRegistry> localRegistryProvider;

    private final ObjectProvider<LoadBalancerClient> loadBalancerClientProvider;

    private final Environment environment;

    private final JobProperties jobProperties;

    private final ObjectMapper objectMapper;

    private final RestClient restClient;

    public JobDispatcher(ObjectProvider<JobHandlerRegistry> localRegistryProvider,
                         ObjectProvider<LoadBalancerClient> loadBalancerClientProvider,
                         Environment environment,
                         JobProperties jobProperties,
                         ObjectProvider<ObjectMapper> objectMapperProvider) {
        this.localRegistryProvider = localRegistryProvider;
        this.loadBalancerClientProvider = loadBalancerClientProvider;
        this.environment = environment;
        this.jobProperties = jobProperties;
        // 使用容器中的 ObjectMapper，保留框架统一序列化配置（如 Long 转 String）
        this.objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(jobProperties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(jobProperties.getReadTimeoutMs());
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    /**
     * 派发任务到目标服务执行
     *
     * @param serviceName 目标服务名（Nacos 注册名）
     * @param handlerName 任务处理器名称
     * @param params      任务参数
     * @return 执行结果
     */
    public JobResultAO dispatch(String serviceName, String handlerName, String params) {
        JobHandlerRegistry localRegistry = localRegistryProvider.getIfAvailable();
        String localAppName = environment.getProperty("spring.application.name", "");
        boolean localTarget = StrUtil.isBlank(serviceName) || serviceName.equalsIgnoreCase(localAppName);
        // 本机优先：boot 单体模式或调度中心自身任务，避免无谓的 HTTP 回环
        if (localTarget && localRegistry != null) {
            JobHandler handler = localRegistry.getHandler(handlerName);
            if (handler != null) {
                return executeLocal(handler, handlerName, params);
            }
            // 目标是本机但本机没有处理器，不再落入远程分支，直接报错
            throw JobErrorCode.JOB_HANDLER_NOT_FOUND.exception("任务处理器不存在: " + handlerName);
        }
        return executeRemote(serviceName, handlerName, params, localRegistry);
    }

    private JobResultAO executeLocal(JobHandler handler, String handlerName, String params) {
        log.info("任务本机执行: handler={}", handlerName);
        try {
            JobResult result = handler.execute(params);
            return new JobResultAO().setSuccess(result.getSuccess()).setMessage(result.getMessage());
        } catch (Exception e) {
            log.error("任务本机执行异常: handler={}", handlerName, e);
            return JobResultAO.fail("任务执行异常: " + e.getMessage());
        }
    }

    private JobResultAO executeRemote(String serviceName, String handlerName, String params,
                                      JobHandlerRegistry localRegistry) {
        LoadBalancerClient loadBalancerClient = loadBalancerClientProvider.getIfAvailable();
        if (loadBalancerClient == null) {
            // 无服务发现（单体模式），只能本机兜底
            if (localRegistry != null) {
                JobHandler handler = localRegistry.getHandler(handlerName);
                if (handler != null) {
                    return executeLocal(handler, handlerName, params);
                }
            }
            throw JobErrorCode.JOB_HANDLER_NOT_FOUND.exception(
                "任务处理器不存在且服务发现不可用: " + handlerName);
        }
        ServiceInstance instance = loadBalancerClient.choose(serviceName);
        if (instance == null) {
            throw JobErrorCode.JOB_SERVICE_UNAVAILABLE.exception("目标服务无可用实例: " + serviceName);
        }
        URI uri = instance.getUri().resolve(JobEndpointConstant.EXECUTE_PATH);
        log.info("任务远程派发: service={}, handler={}, uri={}", serviceName, handlerName, uri);
        try {
            ApiJobExecuteParam executeParam = new ApiJobExecuteParam()
                .setHandlerName(handlerName)
                .setParams(params);
            String body = restClient.post()
                .uri(uri)
                .header(JobEndpointConstant.TOKEN_HEADER,
                    StrUtil.blankToDefault(jobProperties.getAccessToken(), ""))
                .body(executeParam)
                .retrieve()
                .body(String.class);
            return parseResult(body);
        } catch (Exception e) {
            log.error("任务远程派发异常: service={}, handler={}", serviceName, handlerName, e);
            throw JobErrorCode.JOB_DISPATCH_ERROR.exception("任务派发异常: " + e.getMessage());
        }
    }

    /**
     * 解析业务端返回的统一响应（Result&lt;JobResultAO&gt;）
     */
    private JobResultAO parseResult(String body) {
        if (StrUtil.isBlank(body)) {
            return JobResultAO.fail("业务端响应为空");
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isNull()) {
                return JobResultAO.fail("业务端响应异常: " + root.path("msg").asText(body));
            }
            return new JobResultAO()
                .setSuccess(data.path("success").asBoolean(false))
                .setMessage(data.path("message").isNull() ? null : data.path("message").asText());
        } catch (Exception e) {
            return JobResultAO.fail("业务端响应解析失败: " + e.getMessage());
        }
    }

}
