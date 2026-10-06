package com.carlos.disruptor.core;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Disruptor 事件包装类
 * <p>
 * 封装业务数据和事件元数据，支持泛型业务数据
 * </p>
 *
 * @author Carlos
 * @date 2026-03-14
 */
@Data
public class DisruptorEvent<T> {

    /**
     * 事件唯一标识
     */
    private String eventId;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件发生时间
     */
    private LocalDateTime eventTime;

    /**
     * 业务数据
     */
    private T data;

    /**
     * 扩展属性
     */
    private Map<String, Object> attributes;

    /**
     * 创建时间戳（用于延迟计算）
     */
    private long createTimestamp;

    /**
     * 默认构造器
     */
    public DisruptorEvent() {
        this.eventId = UUID.randomUUID().toString().replace("-", "");
        this.eventTime = LocalDateTime.now();
        this.createTimestamp = System.currentTimeMillis();
        this.attributes = new HashMap<>();
    }

    /**
     * 带业务数据的构造器
     *
     * @param data 业务数据
     */
    public DisruptorEvent(T data) {
        this();
        this.data = data;
    }

    /**
     * 带事件类型和业务数据的构造器
     *
     * @param eventType 事件类型
     * @param data      业务数据
     */
    public DisruptorEvent(String eventType, T data) {
        this(data);
        this.eventType = eventType;
    }

    /**
     * 添加扩展属性
     *
     * @param key   属性名
     * @param value 属性值
     * @return 当前事件对象
     */
    public DisruptorEvent<T> addAttribute(String key, Object value) {
        this.attributes.put(key, value);
        return this;
    }

    /**
     * 获取扩展属性
     *
     * @param key 属性名
     * @return 属性值
     */
    @SuppressWarnings("unchecked")
    public <V> V getAttribute(String key) {
        return (V) this.attributes.get(key);
    }

    /**
     * 获取事件处理延迟（毫秒）
     *
     * @return 延迟毫秒数
     */
    public long getDelayMillis() {
        return System.currentTimeMillis() - this.createTimestamp;
    }

    /**
     * 清空事件数据（用于对象池复用）
     */
    public void clear() {
        this.eventId = null;
        this.eventType = null;
        this.eventTime = null;
        this.data = null;
        this.attributes.clear();
        this.createTimestamp = 0;
    }
}
