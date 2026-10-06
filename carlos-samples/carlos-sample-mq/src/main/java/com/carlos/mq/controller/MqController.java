package com.carlos.mq.controller;

import com.carlos.core.response.Result;
import com.carlos.mq.core.MqCapability;
import com.carlos.mq.core.MqTemplate;
import com.carlos.mq.core.SendResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MQ 消息发送示例接口
 * <p>
 * 随契约收敛同步调整：移除了带 tag 与顺序消息的端点（当前传输不提供这些能力，
 * 保留只会演示「不生效的接口」）；延迟消息改用毫秒数（任意精度），
 * 不再使用 18 级量化的 {@code DelayLevel}。
 * </p>
 *
 * @author Carlos
 * @date 2026/3/15
 */
@RestController
@RequestMapping("mq")
@Tag(name = "MQ消息接口", description = "消息队列发送示例接口")
@Slf4j
@RequiredArgsConstructor
public class MqController {

    private final MqTemplate mqTemplate;

    /**
     * 订单消息对象
     */
    @lombok.Data
    public static class OrderMessage implements Serializable {
        private static final long serialVersionUID = 1L;

        @Parameter(description = "订单ID")
        private String orderId;

        @Parameter(description = "用户ID")
        private String userId;

        @Parameter(description = "商品名称")
        private String productName;

        @Parameter(description = "订单金额")
        private BigDecimal amount;

        @Parameter(description = "订单状态")
        private String status;

        @Parameter(description = "创建时间")
        private LocalDateTime createTime;
    }

    /**
     * 发送普通消息
     */
    @PostMapping("send")
    @Operation(summary = "发送普通消息")
    public Result<String> sendMessage(
        @Parameter(description = "消息主题", required = true) @RequestParam String topic,
        @Parameter(description = "消息内容", required = true) @RequestParam String message) {
        SendResult result = mqTemplate.send(topic, message);
        log.info("发送消息：topic={}, status={}, recordId={}", topic, result.getStatus(), result.getRecordId());
        return Result.success(result.getStatus().name());
    }

    /**
     * 异步发送消息
     */
    @PostMapping("sendAsync")
    @Operation(summary = "异步发送消息")
    public Result<Void> sendAsyncMessage(
        @Parameter(description = "消息主题", required = true) @RequestParam String topic,
        @Parameter(description = "消息内容", required = true) @RequestParam String message) {
        mqTemplate.sendAsync(topic, message,
            result -> log.info("异步发送完成：topic={}, success={}, status={}",
                topic, result.isSuccess(), result.getStatus()));
        return Result.success();
    }

    /**
     * 发送延迟消息（任意精度，单位毫秒）
     */
    @PostMapping("sendDelayed")
    @Operation(summary = "发送延迟消息")
    public Result<String> sendDelayedMessage(
        @Parameter(description = "消息主题", required = true) @RequestParam String topic,
        @Parameter(description = "消息内容", required = true) @RequestParam String message,
        @Parameter(description = "延迟毫秒数（上限见 carlos.redis.delay.max-delay，默认 1 小时）",
            required = true) @RequestParam long delayMillis) {
        SendResult result = mqTemplate.sendDelayed(topic, message, Duration.ofMillis(delayMillis));
        log.info("发送延迟消息：topic={}, delayMillis={}, status={}", topic, delayMillis, result.getStatus());
        return Result.success(result.getStatus().name());
    }

    /**
     * 发送对象消息（订单示例）
     */
    @PostMapping("sendOrder")
    @Operation(summary = "发送订单消息")
    public Result<String> sendOrderMessage(@RequestBody OrderMessage order) {
        order.setCreateTime(LocalDateTime.now());
        SendResult result = mqTemplate.send("order-topic", order);
        log.info("发送订单消息：orderId={}, status={}", order.getOrderId(), result.getStatus());
        return Result.success(result.getStatus().name());
    }

    /**
     * 发送延迟订单消息
     */
    @PostMapping("sendOrderDelayed")
    @Operation(summary = "发送延迟订单消息")
    public Result<String> sendOrderDelayed(
        @RequestBody OrderMessage order,
        @Parameter(description = "延迟毫秒数", required = true) @RequestParam long delayMillis) {
        order.setCreateTime(LocalDateTime.now());
        SendResult result = mqTemplate.sendDelayed("order-delay-topic", order,
            Duration.ofMillis(delayMillis));
        log.info("发送延迟订单消息：orderId={}, delayMillis={}, status={}",
            order.getOrderId(), delayMillis, result.getStatus());
        return Result.success(result.getStatus().name());
    }

    /**
     * 发送单向消息（不等待结果）
     */
    @PostMapping("sendOneWay")
    @Operation(summary = "发送单向消息")
    public Result<Void> sendOneWayMessage(
        @Parameter(description = "消息主题", required = true) @RequestParam String topic,
        @Parameter(description = "消息内容", required = true) @RequestParam String message) {
        mqTemplate.sendOneWay(topic, message);
        return Result.success();
    }

    /**
     * 检查 MQ 连接状态（做真实往返）
     */
    @GetMapping("ping")
    @Operation(summary = "检查MQ连接状态")
    public Result<Boolean> ping() {
        boolean available = mqTemplate.ping();
        log.info("MQ连接状态：{}", available);
        return Result.success(available);
    }

    /**
     * 获取当前 MQ 类型
     */
    @GetMapping("type")
    @Operation(summary = "获取当前MQ类型")
    public Result<String> getMqType() {
        return Result.success(mqTemplate.getMqType().getCode());
    }

    /**
     * 获取当前传输能力集合
     * <p>
     * 排查「某方法为何不生效」的第一手信息：能力集合里没有的能力，调用会返回明确的
     * 「能力不支持」而不是静默失效。
     * </p>
     */
    @GetMapping("capabilities")
    @Operation(summary = "获取当前传输能力集合")
    public Result<List<String>> capabilities() {
        return Result.success(mqTemplate.capabilities().stream()
            .map(MqCapability::getCode)
            .sorted()
            .toList());
    }
}
