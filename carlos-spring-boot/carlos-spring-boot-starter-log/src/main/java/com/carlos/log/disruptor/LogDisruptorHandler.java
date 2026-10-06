package com.carlos.log.disruptor;

import com.carlos.disruptor.core.DisruptorEvent;
import com.carlos.disruptor.core.DisruptorEventHandler;
import com.carlos.log.entity.OperationLog;
import com.carlos.log.storage.LogStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 日志 Disruptor 事件处理器
 * <p>
 * 适配通用 Disruptor 框架，处理操作日志事件
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class LogDisruptorHandler implements DisruptorEventHandler<OperationLog> {

    private final LogStorage logStorage;

    // 批量处理队列
    private final BlockingQueue<OperationLog> batchQueue = new LinkedBlockingQueue<>();

    // 批量大小
    private final int batchSize = 100;

    @Override
    public void onEvent(DisruptorEvent<OperationLog> event, long sequence, boolean endOfBatch) throws Exception {
        OperationLog operationLog = event.getData();

        if (operationLog == null) {
            log.warn("收到空的日志事件，序列号: {}", sequence);
            return;
        }

        try {
            // 数据清洗和 enrich
            enrichLog(operationLog);

            // 添加到批量队列或直接处理
            if (logStorage.supportBatch()) {
                batchQueue.offer(operationLog);
                if (batchQueue.size() >= batchSize || endOfBatch) {
                    processBatch();
                }
            } else {
                // 单条处理
                boolean success = logStorage.store(operationLog);
                if (!success) {
                    log.warn("日志存储返回失败");
                }
            }

            // 如果是批量处理且是最后一个事件，处理剩余队列
            if (endOfBatch && logStorage.supportBatch()) {
                processBatch();
            }

        } catch (Exception e) {
            log.error("处理日志事件失败，序列号: {}", sequence, e);
        }
    }

    /**
     * 数据清洗和 enrich
     */
    private void enrichLog(OperationLog operationLog) {
        // 确保时间字段
        if (operationLog.getCreatedTime() == null) {
            operationLog.setCreatedTime(LocalDateTime.now());
        }

        // 确保状态
        if (operationLog.getLogStatus() == null) {
            operationLog.setLogStatus("SUCCESS");
        }

        // 确保风险等级
        if (operationLog.getRiskLevel() == null) {
            operationLog.setRiskLevel(0);
        }
    }

    /**
     * 执行批量处理
     */
    private void processBatch() {
        List<OperationLog> logs = new ArrayList<>();
        batchQueue.drainTo(logs, batchSize);

        if (logs.isEmpty()) {
            return;
        }

        try {
            boolean success = logStorage.storeBatch(logs);
            if (!success) {
                log.warn("批量日志存储失败，数量: {}", logs.size());
                // 降级为单条处理
                for (OperationLog operationLog : logs) {
                    logStorage.store(operationLog);
                }
            }
        } catch (Exception e) {
            log.error("批量日志存储异常，数量: {}", logs.size(), e);
            // 降级为单条处理
            for (OperationLog operationLog : logs) {
                try {
                    logStorage.store(operationLog);
                } catch (Exception ex) {
                    log.error("单条日志存储失败", ex);
                }
            }
        }
    }

    /**
     * 刷新所有待处理的日志
     */
    public void flush() {
        while (!batchQueue.isEmpty()) {
            processBatch();
        }
    }

    @Override
    public String getHandlerName() {
        return "LogDisruptorHandler";
    }
}
