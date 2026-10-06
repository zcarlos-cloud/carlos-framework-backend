package com.carlos.sample.cloud.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Feign 客户端熔断降级实现
 *
 * <p>当调用目标服务失败（超时、异常、服务不可用）时，会执行该降级逻辑。</p>
 *
 * <p><strong>降级策略：</strong></p>
 * <ul>
 *     <li>返回预设的默认值</li>
 *     <li>返回本地缓存数据</li>
 *     <li>返回友好的错误提示</li>
 * </ul>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@Component
public class DemoClientFallback implements DemoClient {

    @Override
    public Map<String, Object> hello(String name) {
        log.warn("DemoClient.hello 降级执行，参数: name={}", name);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", "SERVICE_FALLBACK");
        result.put("message", "服务暂时不可用，已触发降级策略");
        result.put("data", "Hello " + name + " (from fallback)");
        result.put("fallback", true);
        return result;
    }

    @Override
    public Map<String, Object> echo(String message) {
        log.warn("DemoClient.echo 降级执行，参数: message={}", message);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", "SERVICE_FALLBACK");
        result.put("message", "服务暂时不可用，已触发降级策略");
        result.put("data", "Echo: " + message + " (from fallback)");
        result.put("fallback", true);
        return result;
    }

    @Override
    public Map<String, Object> getServiceInfo() {
        log.warn("DemoClient.getServiceInfo 降级执行");
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", "SERVICE_FALLBACK");
        result.put("message", "服务暂时不可用，已触发降级策略");
        result.put("serviceName", "carlos-sample-service (fallback)");
        result.put("fallback", true);
        return result;
    }

}
