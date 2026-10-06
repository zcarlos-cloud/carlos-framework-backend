package com.carlos.sample.disruptor.handler;

import com.carlos.disruptor.core.DisruptorEvent;
import com.carlos.disruptor.core.DisruptorEventHandler;
import com.carlos.sample.disruptor.event.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单验证处理器
 * <p>
 * 验证订单数据的合法性
 * </p>
 *
 * @author Carlos
 * @date 2026-03-15
 */
@Slf4j
@Component
public class OrderEventHandler implements DisruptorEventHandler<OrderEvent> {

    @Override
    public void onEvent(DisruptorEvent<OrderEvent> event, long sequence, boolean endOfBatch) throws Exception {
        OrderEvent order = event.getData();
        if (order == null) {
            log.warn("[OrderEventHandler] 订单数据为空");
            return;
        }

        // 模拟验证处理
        log.info("[OrderEventHandler] 开始验证订单 [sequence={}], orderId={}, orderNo={}",
            sequence, order.getOrderId(), order.getOrderNo());

        // 模拟业务验证逻辑
        if (order.getAmount() == null || order.getAmount().doubleValue() <= 0) {
            log.warn("[OrderEventHandler] 订单金额无效: {}", order.getOrderId());
            order.setStatus(OrderEvent.Status.FAILED.getCode());
        } else {
            order.setStatus(OrderEvent.Status.VALIDATED.getCode());
            log.info("[OrderEventHandler] 订单验证通过: {}", order.getOrderId());
        }

        // 模拟处理耗时
        Thread.sleep(10);

        if (endOfBatch) {
            log.debug("[OrderEventHandler] 当前批次处理完成 [sequence={}]", sequence);
        }
    }

    @Override
    public String getHandlerName() {
        return "OrderValidateHandler";
    }

    @Override
    public boolean ignoreException() {
        // 发生异常时继续处理其他事件
        return true;
    }

    @Override
    public void onException(DisruptorEvent<OrderEvent> event, long sequence, Throwable exception) {
        log.error("[OrderEventHandler] 处理订单事件异常 [sequence={}], eventId={}",
            sequence, event != null ? event.getEventId() : "null", exception);
    }
}
