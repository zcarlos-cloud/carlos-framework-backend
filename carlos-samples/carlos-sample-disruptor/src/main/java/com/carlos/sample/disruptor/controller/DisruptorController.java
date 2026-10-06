package com.carlos.sample.disruptor.controller;

import com.carlos.core.response.Result;
import com.carlos.disruptor.core.DisruptorManager;
import com.carlos.disruptor.core.DisruptorTemplate;
import com.carlos.sample.disruptor.event.OrderEvent;
import com.carlos.sample.disruptor.handler.OrderEventHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Disruptor 事件发布演示 Controller
 *
 * @author Carlos
 * @date 2026-03-15
 */
@Slf4j
@RestController
@RequestMapping("/api/disruptor")
@RequiredArgsConstructor
@Tag(name = "Disruptor演示", description = "高性能异步事件处理演示接口")
public class DisruptorController {

    private final DisruptorManager disruptorManager;
    private final OrderEventHandler orderEventHandler;

    private DisruptorTemplate<OrderEvent> orderDisruptor;

    @PostConstruct
    public void init() {
        // 创建 Disruptor 实例，绑定订单事件处理器
        orderDisruptor = disruptorManager.create("order",
            orderEventHandler
        );
        log.info("Disruptor 实例创建成功: order");
    }

    /**
     * 发布单个订单事件
     */
    @PostMapping("/publish")
    @Operation(summary = "发布单个订单事件", description = "发布一个订单事件到 Disruptor 队列")
    public Result<String> publishOrder(
        @Parameter(description = "订单金额") @RequestParam(defaultValue = "100.00") String amount,
        @Parameter(description = "商品名称") @RequestParam(defaultValue = "测试商品") String productName) {

        OrderEvent order = createOrder(amount, productName);

        log.info("[Controller] 准备发布订单事件: {}", order.getOrderNo());
        orderDisruptor.publishEvent(order);
        log.info("[Controller] 订单事件发布成功: {}", order.getOrderNo());

        return Result.success("订单事件发布成功: " + order.getOrderNo());
    }

    /**
     * 批量发布订单事件
     */
    @PostMapping("/publish-batch")
    @Operation(summary = "批量发布订单事件", description = "批量发布订单事件到 Disruptor 队列")
    public Result<String> publishBatch(
        @Parameter(description = "事件数量") @RequestParam(defaultValue = "10") int count) {

        List<OrderEvent> orders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            orders.add(createOrder("" + (i + 1) * 10, "批量商品-" + (i + 1)));
        }

        log.info("[Controller] 准备批量发布 {} 个订单事件", orders.size());
        orderDisruptor.publishEventList(orders);
        log.info("[Controller] 批量订单事件发布完成");

        return Result.success("批量发布 " + count + " 个订单事件成功");
    }

    /**
     * 尝试发布事件（非阻塞）
     */
    @PostMapping("/try-publish")
    @Operation(summary = "尝试发布事件", description = "如果队列满则立即返回 false，不会阻塞")
    public Result<String> tryPublish(
        @Parameter(description = "订单金额") @RequestParam(defaultValue = "100.00") String amount,
        @Parameter(description = "商品名称") @RequestParam(defaultValue = "测试商品") String productName) {

        OrderEvent order = createOrder(amount, productName);

        boolean success = orderDisruptor.tryPublishEvent(order);
        if (success) {
            return Result.success("订单事件发布成功: " + order.getOrderNo());
        } else {
            return Result.error("队列已满，发布失败");
        }
    }

    /**
     * 带超时时间发布事件
     */
    @PostMapping("/publish-timeout")
    @Operation(summary = "带超时发布事件", description = "带超时时间的发布，超时返回失败")
    public Result<String> publishWithTimeout(
        @Parameter(description = "订单金额") @RequestParam(defaultValue = "100.00") String amount,
        @Parameter(description = "商品名称") @RequestParam(defaultValue = "测试商品") String productName,
        @Parameter(description = "超时毫秒") @RequestParam(defaultValue = "100") long timeoutMs) {

        OrderEvent order = createOrder(amount, productName);

        boolean success = orderDisruptor.tryPublishEvent(order, timeoutMs, TimeUnit.MILLISECONDS);
        if (success) {
            return Result.success("订单事件发布成功: " + order.getOrderNo());
        } else {
            return Result.error("发布超时");
        }
    }

    /**
     * 发布带类型的事件
     */
    @PostMapping("/publish-typed")
    @Operation(summary = "发布带类型的事件", description = "发布带有特定事件类型的订单事件")
    public Result<String> publishTyped(
        @Parameter(description = "事件类型") @RequestParam(defaultValue = "ORDER_CREATED") String eventType,
        @Parameter(description = "订单金额") @RequestParam(defaultValue = "100.00") String amount,
        @Parameter(description = "商品名称") @RequestParam(defaultValue = "测试商品") String productName) {

        OrderEvent order = createOrder(amount, productName);

        log.info("[Controller] 准备发布类型为 [{}] 的订单事件: {}", eventType, order.getOrderNo());
        orderDisruptor.publishEvent(eventType, order);

        return Result.success("类型化订单事件发布成功: " + order.getOrderNo());
    }

    /**
     * 获取 Disruptor 状态信息
     */
    @GetMapping("/stats")
    @Operation(summary = "获取状态信息", description = "获取 Disruptor 队列的容量和统计信息")
    public Result<DisruptorStats> getStats() {
        DisruptorStats stats = new DisruptorStats();
        stats.setBufferSize(orderDisruptor.getBufferSize());
        stats.setRemainingCapacity(orderDisruptor.remainingCapacity());
        stats.setCursor(orderDisruptor.getCursor());
        stats.setUsageRate(String.format("%.2f%%",
            (1.0 - (double) orderDisruptor.remainingCapacity() / orderDisruptor.getBufferSize()) * 100));

        DisruptorManager.HandlerStats handlerStats = disruptorManager.getStats("order");
        if (handlerStats != null) {
            stats.setProcessedCount(handlerStats.processedCount());
            stats.setExceptionCount(handlerStats.exceptionCount());
        }

        return Result.success(stats);
    }

    /**
     * 创建订单事件
     */
    private OrderEvent createOrder(String amount, String productName) {
        OrderEvent order = new OrderEvent();
        order.setOrderId(UUID.randomUUID().toString().replace("-", ""));
        order.setOrderNo("ORD" + System.currentTimeMillis());
        order.setUserId(System.currentTimeMillis());
        order.setAmount(new BigDecimal(amount));
        order.setProductName(productName);
        order.setStatus(OrderEvent.Status.PENDING.getCode());
        order.setCreateTime(LocalDateTime.now());
        return order;
    }

    /**
     * Disruptor 统计信息
     */
    public static class DisruptorStats {
        private long bufferSize;
        private long remainingCapacity;
        private long cursor;
        private String usageRate;
        private long processedCount;
        private long exceptionCount;

        // Getters and Setters
        public long getBufferSize() {
            return bufferSize;
        }

        public void setBufferSize(long bufferSize) {
            this.bufferSize = bufferSize;
        }

        public long getRemainingCapacity() {
            return remainingCapacity;
        }

        public void setRemainingCapacity(long remainingCapacity) {
            this.remainingCapacity = remainingCapacity;
        }

        public long getCursor() {
            return cursor;
        }

        public void setCursor(long cursor) {
            this.cursor = cursor;
        }

        public String getUsageRate() {
            return usageRate;
        }

        public void setUsageRate(String usageRate) {
            this.usageRate = usageRate;
        }

        public long getProcessedCount() {
            return processedCount;
        }

        public void setProcessedCount(long processedCount) {
            this.processedCount = processedCount;
        }

        public long getExceptionCount() {
            return exceptionCount;
        }

        public void setExceptionCount(long exceptionCount) {
            this.exceptionCount = exceptionCount;
        }
    }
}
