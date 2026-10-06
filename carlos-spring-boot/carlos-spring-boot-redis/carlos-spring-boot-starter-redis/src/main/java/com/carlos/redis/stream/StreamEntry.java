package com.carlos.redis.stream;

import java.time.Duration;
import java.util.Map;

/**
 * 一次读取到的 Stream 条目（不可变）
 *
 * @param recordId    底层物理 id
 * @param fields      条目字段（不可变 Map）
 * @param consumerName 该条目的当前归属消费者（仅待处理查询填充；直接读取时为 null）
 * @param deliveryCount 累计投递次数（仅待处理查询填充；直接读取时为 0）
 * @param idle        距上次投递的空闲时长（仅待处理查询填充；直接读取时为 null）
 * @author Carlos
 * @since 3.0.0
 */
public record StreamEntry(
    String recordId,
    Map<String, String> fields,
    String consumerName,
    long deliveryCount,
    Duration idle
) {

    /**
     * 紧凑构造器：字段复制为不可变并兜住 null。
     */
    public StreamEntry {
        fields = fields == null ? Map.of() : Map.copyOf(fields);
    }

    /**
     * 直接读取到的条目（无投递元信息）
     *
     * @param recordId 物理 id
     * @param fields   字段
     * @return 条目
     */
    public static StreamEntry of(String recordId, Map<String, String> fields) {
        return new StreamEntry(recordId, fields, null, 0, null);
    }

    /**
     * 读取字段
     *
     * @param name 字段名
     * @return 字段值，不存在时为 null
     */
    public String field(String name) {
        return fields.get(name);
    }
}
