package com.carlos.sample.web;

import com.carlos.core.response.Result;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Web功能演示控制器
 *
 * @author carlos
 */
@Slf4j
@RestController
@RequestMapping("/demo")
public class DemoController {

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("Service is running");
    }

    /**
     * 获取服务器时间
     */
    @GetMapping("/time")
    public Result<Map<String, Object>> getTime() {
        Map<String, Object> data = new HashMap<>();
        data.put("currentTime", LocalDateTime.now());
        data.put("timestamp", System.currentTimeMillis());
        return Result.success(data);
    }

    /**
     * GET请求示例 - 带路径参数
     */
    @GetMapping("/hello/{name}")
    public Result<String> hello(@PathVariable String name) {
        return Result.success("Hello, " + name + "!");
    }

    /**
     * GET请求示例 - 带查询参数
     */
    @GetMapping("/greet")
    public Result<String> greet(@RequestParam(required = false, defaultValue = "World") String name) {
        return Result.success("Greetings, " + name + "!");
    }

    /**
     * POST请求示例 - 参数校验
     */
    @PostMapping("/user")
    public Result<Map<String, Object>> createUser(@Valid @RequestBody UserParam param) {
        log.info("Creating user: {}", param.getUsername());
        Map<String, Object> result = new HashMap<>();
        result.put("username", param.getUsername());
        result.put("email", param.getEmail());
        result.put("age", param.getAge());
        result.put("createdAt", LocalDateTime.now());
        return Result.success(result);
    }

    /**
     * PUT请求示例
     */
    @PutMapping("/user/{id}")
    public Result<Map<String, Object>> updateUser(@PathVariable Long id, @Valid @RequestBody UserParam param) {
        log.info("Updating user {}: {}", id, param.getUsername());
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("username", param.getUsername());
        result.put("updatedAt", LocalDateTime.now());
        return Result.success(result);
    }

    /**
     * DELETE请求示例
     */
    @DeleteMapping("/user/{id}")
    public Result<String> deleteUser(@PathVariable Long id) {
        log.info("Deleting user: {}", id);
        return Result.success("User " + id + " deleted successfully");
    }
}
