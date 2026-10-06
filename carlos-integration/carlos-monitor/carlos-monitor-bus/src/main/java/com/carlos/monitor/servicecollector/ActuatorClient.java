package com.carlos.monitor.servicecollector;

import cn.hutool.core.util.StrUtil;
import com.carlos.json.JsonUtils;
import com.carlos.monitor.config.MonitorProperties;
import com.carlos.monitor.enums.MonitorErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Actuator 客户端（微服务实例直拉，内网免鉴权 + 可配静态 header）
 * 超时取 carlos.monitor.service-monitor.*；所有异常翻译为 MonitorErrorCode
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@Component
public class ActuatorClient {

    private final MonitorProperties monitorProperties;

    private volatile RestClient restClient;

    public ActuatorClient(MonitorProperties monitorProperties) {
        this.monitorProperties = monitorProperties;
    }

    /**
     * GET actuator JSON 端点
     *
     * @param address 实例地址 ip:port
     * @param path    端点路径（/health、/metrics/{name}）
     * @return JSON Map（失败返回 null，调用方按不可用处理）
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String address, String path) {
        String url = baseUrl(address) + path;
        try {
            String body = client().get()
                .uri(url)
                .retrieve()
                .body(String.class);
            if (StrUtil.isBlank(body) || !JsonUtils.isValidJson(body)) {
                return null;
            }
            return JsonUtils.fromJson(body, Map.class);
        } catch (Exception e) {
            log.debug("actuator 拉取失败, url:{}, error:{}", url, e.getMessage());
            return null;
        }
    }

    /**
     * 读取 logger 当前级别（configuredLevel 优先，回退 effectiveLevel）
     */
    public String getLoggerLevel(String address, String loggerName) {
        Map<String, Object> result = get(address, loggerPath(loggerName));
        if (result == null) {
            return null;
        }
        Object configured = result.get("configuredLevel");
        if (configured != null) {
            return String.valueOf(configured);
        }
        Object effective = result.get("effectiveLevel");
        return effective == null ? null : String.valueOf(effective);
    }

    /**
     * 设置 logger 级别（level 为 null 表示重置）
     */
    public void setLoggerLevel(String address, String loggerName, String level) {
        String url = baseUrl(address) + loggerPath(loggerName);
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("configuredLevel", level);
            client().post()
                .uri(url)
                .body(body)
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            throw MonitorErrorCode.MONITOR_CONNECTION_UNAVAILABLE
                .exception("调整日志级别失败：" + StrUtil.maxLength(e.getMessage(), 200));
        }
    }

    private String loggerPath(String loggerName) {
        return basePath() + "/loggers/" + loggerName;
    }

    private String baseUrl(String address) {
        return "http://" + address + basePath();
    }

    private String basePath() {
        String basePath = monitorProperties.getServiceMonitor().getActuatorBasePath();
        return StrUtil.isBlank(basePath) ? "/actuator" : basePath;
    }

    /**
     * 惰性构建 RestClient（带超时与可选鉴权头）
     */
    private RestClient client() {
        if (restClient == null) {
            synchronized (this) {
                if (restClient == null) {
                    MonitorProperties.ServiceMonitor props = monitorProperties.getServiceMonitor();
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(props.getConnectTimeoutMs());
                    factory.setReadTimeout(props.getReadTimeoutMs());
                    RestClient.Builder builder = RestClient.builder().requestFactory(factory);
                    if (StrUtil.isNotBlank(props.getAuthHeaderName())) {
                        builder.defaultHeader(props.getAuthHeaderName(),
                            StrUtil.nullToEmpty(props.getAuthHeaderValue()));
                    }
                    restClient = builder.build();
                }
            }
        }
        return restClient;
    }
}
