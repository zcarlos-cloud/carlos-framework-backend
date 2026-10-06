package com.carlos.sample.cloud.controller;

import com.carlos.core.response.Result;
import com.carlos.sample.cloud.client.DemoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Feign 调用演示控制器
 *
 * <p>演示 Spring Cloud 微服务调用的各种场景：</p>
 * <ul>
 *     <li>OpenFeign 声明式服务调用</li>
 *     <li>Nacos 服务发现查询</li>
 *     <li>负载均衡效果演示</li>
 *     <li>熔断降级效果演示</li>
 * </ul>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/cloud")
@RequiredArgsConstructor
public class FeignController {

    private final DemoClient demoClient;
    private final DiscoveryClient discoveryClient;

    /**
     * 通过 Feign 调用示例服务的 hello 接口
     *
     * @param name 名称
     * @return 调用结果
     */
    @GetMapping("/hello")
    public Result<Map<String, Object>> hello(@RequestParam(value = "name", defaultValue = "World") String name) {
        log.info("通过 Feign 调用 hello 接口，参数: name={}", name);
        try {
            Map<String, Object> result = demoClient.hello(name);
            return Result.success(result);
        } catch (Exception e) {
            log.error("调用 hello 接口失败", e);
            return Result.error("调用服务失败: " + e.getMessage());
        }
    }

    /**
     * 通过 Feign 调用示例服务的 echo 接口
     *
     * @param message 消息
     * @return 调用结果
     */
    @GetMapping("/echo/{message}")
    public Result<Map<String, Object>> echo(@PathVariable String message) {
        log.info("通过 Feign 调用 echo 接口，参数: message={}", message);
        try {
            Map<String, Object> result = demoClient.echo(message);
            return Result.success(result);
        } catch (Exception e) {
            log.error("调用 echo 接口失败", e);
            return Result.error("调用服务失败: " + e.getMessage());
        }
    }

    /**
     * 通过 Feign 获取示例服务信息
     *
     * @return 服务信息
     */
    @GetMapping("/service-info")
    public Result<Map<String, Object>> getServiceInfo() {
        log.info("通过 Feign 获取服务信息");
        try {
            Map<String, Object> result = demoClient.getServiceInfo();
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取服务信息失败", e);
            return Result.error("调用服务失败: " + e.getMessage());
        }
    }

    /**
     * 查询 Nacos 中注册的服务列表
     *
     * @return 服务列表
     */
    @GetMapping("/services")
    public Result<Map<String, Object>> getServices() {
        log.info("查询 Nacos 服务列表");
        List<String> services = discoveryClient.getServices();

        Map<String, Object> result = new HashMap<>();
        result.put("serviceCount", services.size());
        result.put("services", services);

        // 获取每个服务的实例详情
        Map<String, List<ServiceInstance>> instances = new HashMap<>();
        for (String service : services) {
            List<ServiceInstance> serviceInstances = discoveryClient.getInstances(service);
            instances.put(service, serviceInstances);
        }
        result.put("instances", instances);

        return Result.success(result);
    }

    /**
     * 查询指定服务的实例列表
     *
     * @param serviceName 服务名称
     * @return 实例列表
     */
    @GetMapping("/instances/{serviceName}")
    public Result<List<ServiceInstance>> getInstances(@PathVariable String serviceName) {
        log.info("查询服务实例列表，服务名: {}", serviceName);
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        return Result.success(instances);
    }

    /**
     * 获取当前服务信息
     *
     * @return 当前服务信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("serviceName", "carlos-sample-cloud");
        info.put("description", "Carlos Spring Cloud 示例服务");
        info.put("features", new String[]{
            "Nacos 服务注册与发现",
            "OpenFeign 声明式 HTTP 客户端",
            "Spring Cloud LoadBalancer 负载均衡",
            "Sentinel 流量控制（可选）"
        });
        return Result.success(info);
    }

}
