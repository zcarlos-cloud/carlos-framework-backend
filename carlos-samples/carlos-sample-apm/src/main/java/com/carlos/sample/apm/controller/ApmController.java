package com.carlos.sample.apm.controller;

import com.carlos.apm.TraceUtil;
import com.carlos.apm.annotation.TraceTag;
import com.carlos.apm.mdc.MdcUtil;
import com.carlos.apm.skywalking.util.SkyWalkingUtil;
import com.carlos.core.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * APM 功能演示 Controller
 *
 * @author Carlos
 * @date 2024-12-09
 */
@Slf4j
@RestController
@RequestMapping("/apm")
public class ApmController {

    /**
     * 获取追踪信息
     */
    @GetMapping("/trace")
    public Result<Map<String, Object>> getTraceInfo() {
        Map<String, Object> traceInfo = new HashMap<>();

        // 获取 Sleuth Trace ID
        String sleuthTraceId = TraceUtil.getTraceId();
        traceInfo.put("sleuthTraceId", sleuthTraceId);

        // 获取 SkyWalking Trace ID
        String swTraceId = TraceUtil.getSkyWalkingTraceId();
        traceInfo.put("skywalkingTraceId", swTraceId);

        // 获取统一 Trace ID（优先 Sleuth）
        String unifiedTraceId = TraceUtil.getUnifiedTraceId();
        traceInfo.put("unifiedTraceId", unifiedTraceId);

        // 获取 Span ID
        String spanId = TraceUtil.getSpanId();
        traceInfo.put("spanId", spanId);

        // 获取 MDC 中的 Trace ID
        String mdcTraceId = MdcUtil.getTraceId();
        traceInfo.put("mdcTraceId", mdcTraceId);

        // 是否在追踪上下文中
        boolean inTraceContext = TraceUtil.isInTraceContext();
        traceInfo.put("inTraceContext", inTraceContext);

        log.info("[TraceId: {}] 获取追踪信息", unifiedTraceId);

        return Result.success(traceInfo);
    }

    /**
     * 获取 SkyWalking 上下文信息
     */
    @GetMapping("/skywalking")
    public Result<Map<String, Object>> getSkyWalkingInfo() {
        Map<String, Object> swInfo = new HashMap<>();

        // SkyWalking Trace ID
        String traceId = SkyWalkingUtil.getTraceId();
        swInfo.put("traceId", traceId);

        // 是否在追踪上下文中
        boolean inContext = SkyWalkingUtil.inTraceContext();
        swInfo.put("inTraceContext", inContext);

        // 完整上下文信息
        String fullContext = SkyWalkingUtil.getFullContext();
        swInfo.put("fullContext", fullContext);

        // 添加自定义标签
        SkyWalkingUtil.setTag("apm.sample", "skywalking");
        SkyWalkingUtil.logInfo("SkyWalking 信息查询成功");

        log.info("[TraceId: {}] 获取 SkyWalking 信息", traceId);

        return Result.success(swInfo);
    }

    /**
     * 演示 TraceTag 注解 - 创建订单
     */
    @PostMapping("/order")
    @TraceTag(key = "order.action", value = "'create'")
    @TraceTag(key = "order.userId", value = "#userId")
    @TraceTag(key = "order.amount", value = "#amount")
    public Result<Map<String, Object>> createOrder(
        @RequestParam Long userId,
        @RequestParam String productName,
        @RequestParam Double amount) {

        String traceId = TraceUtil.getUnifiedTraceId();
        log.info("[TraceId: {}] 创建订单，用户: {}, 商品: {}, 金额: {}",
            traceId, userId, productName, amount);

        // 添加业务标签
        TraceUtil.tag("order.product", productName);
        TraceUtil.tag("order.amount", amount.toString());

        Map<String, Object> order = new HashMap<>();
        order.put("orderId", System.currentTimeMillis());
        order.put("userId", userId);
        order.put("productName", productName);
        order.put("amount", amount);
        order.put("status", "CREATED");
        order.put("traceId", traceId);

        // 记录事件
        TraceUtil.annotate("Order created successfully");
        SkyWalkingUtil.logInfo("订单创建成功: " + order.get("orderId"));

        return Result.success(order);
    }

    /**
     * 模拟耗时操作，演示性能监控
     */
    @GetMapping("/slow")
    @TraceTag(key = "apm.action", value = "'slow-operation'")
    public Result<Map<String, Object>> slowOperation(
        @RequestParam(defaultValue = "1000") long sleepTime) {

        String traceId = TraceUtil.getUnifiedTraceId();
        log.info("[TraceId: {}] 开始执行耗时操作，预计耗时: {}ms", traceId, sleepTime);

        long startTime = System.currentTimeMillis();

        try {
            // 模拟业务处理
            Thread.sleep(sleepTime);

            // 添加处理标签
            TraceUtil.tag("operation.sleepTime", String.valueOf(sleepTime));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[TraceId: {}] 耗时操作被中断", traceId);
            return Result.error("操作被中断");
        }

        long actualTime = System.currentTimeMillis() - startTime;
        log.info("[TraceId: {}] 耗时操作完成，实际耗时: {}ms", traceId, actualTime);

        Map<String, Object> result = new HashMap<>();
        result.put("expectedTime", sleepTime);
        result.put("actualTime", actualTime);
        result.put("traceId", traceId);

        return Result.success(result);
    }

    /**
     * 模拟错误场景
     */
    @GetMapping("/error")
    @TraceTag(key = "apm.action", value = "'simulate-error'")
    public Result<Map<String, Object>> simulateError(
        @RequestParam(defaultValue = "false") boolean shouldError) {

        String traceId = TraceUtil.getUnifiedTraceId();
        log.info("[TraceId: {}] 模拟错误场景，是否触发错误: {}", traceId, shouldError);

        if (shouldError) {
            // 记录错误信息到 SkyWalking
            SkyWalkingUtil.setError("用户触发模拟错误");
            SkyWalkingUtil.setTag("error.simulated", "true");
            log.error("[TraceId: {}] 模拟错误已触发", traceId);
            return Result.error("模拟的错误响应");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("message", "操作成功完成");
        result.put("shouldError", shouldError);
        result.put("traceId", traceId);

        SkyWalkingUtil.logInfo("模拟错误场景处理成功");

        return Result.success(result);
    }

    /**
     * 获取 MDC 信息
     */
    @GetMapping("/mdc")
    public Result<Map<String, Object>> getMdcInfo() {
        Map<String, Object> mdcInfo = new HashMap<>();

        // 获取 MDC 中的 Trace ID
        String traceId = MdcUtil.getTraceId();
        mdcInfo.put("traceId", traceId);

        // 获取其他 MDC 值
        String spanId = MdcUtil.get(TraceUtil.getSpanId());
        mdcInfo.put("spanId", spanId);

        log.info("[TraceId: {}] 获取 MDC 信息", traceId);

        return Result.success(mdcInfo);
    }
}
