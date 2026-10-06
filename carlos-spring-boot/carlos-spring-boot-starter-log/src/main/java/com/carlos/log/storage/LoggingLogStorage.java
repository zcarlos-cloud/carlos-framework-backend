package com.carlos.log.storage;

import com.carlos.log.entity.OperationLog;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 基于 SLF4J 的日志存储器
 * <p>
 * 将操作日志输出到应用日志中，作为默认兜底方案
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
public class LoggingLogStorage implements LogStorage {

    @Override
    public boolean store(OperationLog operationLog) {
        if (operationLog == null) {
            return false;
        }
        log.info("[OPERATION_LOG] type={}, principal={}, target={}:{}, duration={}ms, state={}",
            operationLog.getLogType(),
            operationLog.getPrincipalId(),
            operationLog.getTargetType(),
            operationLog.getTargetId(),
            operationLog.getDurationMs(),
            operationLog.getLogStatus()
        );
        return true;
    }

    @Override
    public boolean storeBatch(List<OperationLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return true;
        }
        for (OperationLog operationLog : logs) {
            store(operationLog);
        }
        return true;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE; // 最低优先级，作为兜底
    }

    @Override
    public String getName() {
        return "LoggingLogStorage";
    }
}
