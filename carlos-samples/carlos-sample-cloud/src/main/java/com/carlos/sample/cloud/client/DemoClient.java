package com.carlos.sample.cloud.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Feign 客户端接口 - 演示服务间调用
 *
 * <p>该接口使用 OpenFeign 声明式 HTTP 客户端，演示如何调用其他微服务。</p>
 *
 * <p><strong>说明：</strong></p>
 * <ul>
 *     <li>{@code name} 属性指定目标服务的注册名称（对应 spring.application.name）</li>
 *     <li>{@code url} 属性可用于直接指定服务地址（开发调试时使用）</li>
 *     <li>{@code fallback} 属性指定熔断降级实现类</li>
 * </ul>
 *
 * <p><strong>示例场景：</strong></p>
 * <pre>
 * // 调用 carlos-sample-service 服务
 * &#64;FeignClient(name = "carlos-sample-service", fallback = DemoClientFallback.class)
 * public interface DemoClient { ... }
 * </pre>
 *
 * @author carlos
 * @since 3.0.0
 */
@FeignClient(
    name = "carlos-sample-service",
    url = "${demo.service.url:}",
    fallback = DemoClientFallback.class
)
public interface DemoClient {

    /**
     * 调用示例服务的 hello 接口
     *
     * @param name 名称参数
     * @return 响应结果
     */
    @GetMapping("/api/demo/hello")
    Map<String, Object> hello(@RequestParam("name") String name);

    /**
     * 调用示例服务的 echo 接口
     *
     * @param message 消息内容
     * @return 响应结果
     */
    @GetMapping("/api/demo/echo/{message}")
    Map<String, Object> echo(@PathVariable("message") String message);

    /**
     * 获取服务实例信息
     *
     * @return 服务实例信息
     */
    @GetMapping("/api/demo/info")
    Map<String, Object> getServiceInfo();

}
