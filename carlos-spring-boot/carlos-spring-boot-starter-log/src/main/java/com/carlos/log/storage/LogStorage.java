package com.carlos.log.storage;

import com.carlos.log.entity.OperationLog;

import java.util.List;

/**
 * 操作日志存储接口
 * <p>
 * 应用可以实现此接口来自定义日志存储方式，如：
 * - 数据库存储（MySQL、PostgreSQL）
 * - 列式存储（ClickHouse、Doris）
 * - 搜索引擎（Elasticsearch）
 * - 消息队列（Kafka、RocketMQ）
 * - 文件存储
 *
 * @author carlos
 * @since 3.0.0
 */
public interface LogStorage {

    /**
     * 存储单条日志
     *
     * @param log 操作日志
     * @return 是否存储成功
     */
    boolean store(OperationLog log);

    /**
     * 批量存储日志
     *
     * @param logs 操作日志列表
     * @return 是否存储成功
     */
    boolean storeBatch(List<OperationLog> logs);

    /**
     * 存储优先级，数字越小优先级越高
     * 当存在多个存储实现时，按优先级顺序调用
     *
     * @return 优先级，默认 100
     */
    default int getPriority() {
        return 100;
    }

    /**
     * 是否支持批量存储
     *
     * @return true 表示支持批量存储
     */
    default boolean supportBatch() {
        return true;
    }

    /**
     * 存储器名称
     *
     * @return 存储器标识名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}
