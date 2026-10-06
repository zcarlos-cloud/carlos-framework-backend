package com.carlos.mq.listener;

import com.carlos.mq.error.MqErrorCode;
import com.carlos.mq.exception.MqException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 监听端点注册表
 * <p>
 * 启动期由 {@link MqListenerAnnotationBeanPostProcessor} 填充；运行期只读。
 * </p>
 * <p>
 * <b>同一 (topic, group) 只允许一个端点</b>：否则「消息由谁处理」不可预期。
 * 冲突在注册时就报错（启动失败），而不是运行期随机选一个。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
public class MqListenerRegistry {

    /** key = topic@group */
    private final Map<String, MqListenerEndpoint> endpoints = new LinkedHashMap<>();

    /**
     * 注册一个端点
     *
     * @param endpoint 端点
     * @throws MqException 同一 (topic, group) 已被占用时抛出
     */
    public synchronized void register(MqListenerEndpoint endpoint) {
        MqListenerEndpoint existing = endpoints.putIfAbsent(endpoint.subscriptionKey(), endpoint);
        if (existing != null) {
            throw new MqException(MqErrorCode.MQ_LISTENER_CONFIG_INVALID,
                "同一主题与消费组被多个 @MqListener 占用：%s 已由 %s 注册，%s 重复"
                    .formatted(endpoint.subscriptionKey(), existing.display(), endpoint.display()));
        }
    }

    /**
     * 按订阅键查找
     *
     * @param topic 主题
     * @param group 消费组
     * @return 端点
     */
    public synchronized Optional<MqListenerEndpoint> find(String topic, String group) {
        return Optional.ofNullable(endpoints.get(topic + "@" + group));
    }

    /**
     * 已注册的全部端点（不可变副本）
     *
     * @return 端点列表
     */
    public synchronized List<MqListenerEndpoint> all() {
        return List.copyOf(new ArrayList<>(endpoints.values()));
    }

    /**
     * 端点数量
     *
     * @return 数量
     */
    public synchronized int size() {
        return endpoints.size();
    }

    /**
     * 是否为空
     *
     * @return 无端点时返回 true
     */
    public synchronized boolean isEmpty() {
        return endpoints.isEmpty();
    }

    /**
     * 只读视图（诊断用）
     *
     * @return 不可变 Map
     */
    public synchronized Map<String, MqListenerEndpoint> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(endpoints));
    }
}
