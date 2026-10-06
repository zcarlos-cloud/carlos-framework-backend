package com.carlos.log.storage;

import com.carlos.log.entity.OperationLog;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;

/**
 * 组合日志存储器
 * <p>
 * 支持同时向多个存储器写入日志
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
public class CompositeLogStorage implements LogStorage {

    private final List<LogStorage> storages;

    public CompositeLogStorage(List<LogStorage> storages) {
        // 按优先级排序
        this.storages = storages.stream()
            .sorted(Comparator.comparingInt(LogStorage::getPriority))
            .toList();
    }

    @Override
    public boolean store(OperationLog operationLog) {
        boolean allSuccess = true;
        for (LogStorage storage : storages) {
            try {
                boolean success = storage.store(operationLog);
                if (!success) {
                    log.warn("日志存储失败，存储器: {}", storage.getName());
                    allSuccess = false;
                }
            } catch (Exception e) {
                log.error("日志存储异常，存储器: {}", storage.getName(), e);
                allSuccess = false;
            }
        }
        return allSuccess;
    }

    @Override
    public boolean storeBatch(List<OperationLog> logs) {
        boolean allSuccess = true;
        for (LogStorage storage : storages) {
            try {
                boolean success = storage.storeBatch(logs);
                if (!success) {
                    log.warn("批量日志存储失败，存储器: {}", storage.getName());
                    allSuccess = false;
                }
            } catch (Exception e) {
                log.error("批量日志存储异常，存储器: {}", storage.getName(), e);
                allSuccess = false;
            }
        }
        return allSuccess;
    }

    @Override
    public boolean supportBatch() {
        return storages.stream().anyMatch(LogStorage::supportBatch);
    }

    @Override
    public String getName() {
        return "CompositeLogStorage[" + storages.size() + "]";
    }
}
