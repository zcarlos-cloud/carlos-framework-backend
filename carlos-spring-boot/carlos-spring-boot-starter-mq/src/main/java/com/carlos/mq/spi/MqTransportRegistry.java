package com.carlos.mq.spi;

import com.carlos.mq.error.MqErrorCode;
import com.carlos.mq.exception.MqException;
import com.carlos.mq.support.MqType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * MQ 传输注册表
 * <p>
 * 取代旧的 {@code MqClientSelector}。旧实现的两个缺陷在此修正：
 * </p>
 * <ul>
 *   <li><b>探测顺序不确定</b>：旧实现遍历 {@code ConcurrentHashMap} 决定优先级，
 *       与设计文档承诺的顺序不符。本实现不再做类路径探测——传输实现由 Spring 容器
 *       以 bean 形式注册，选择依据是显式配置的 {@link MqType}。</li>
 *   <li><b>两套互相矛盾的探测逻辑</b>（{@code MqClientSelector} 与
 *       {@code OnMqTypeCondition}）：已随 S1 一并移除，本类是唯一入口。</li>
 * </ul>
 *
 * <h2>选择规则（确定性，无隐式优先级）</h2>
 * <ol>
 *   <li>配置了明确的 {@link MqType} ⇒ 取该实现；不存在则报
 *       {@link MqErrorCode#MQ_TRANSPORT_NOT_FOUND}</li>
 *   <li>配置为 {@link MqType#AUTO} 且仅注册了一个实现 ⇒ 取该实现</li>
 *   <li>配置为 {@link MqType#AUTO} 且有多个实现 ⇒ <b>报错并提示显式配置</b>，
 *       而不是猜一个（这正是旧实现静默选错类型的问题）</li>
 *   <li>无任何实现 ⇒ {@link #currentOrEmpty()} 为空，由上层决定是否 fail-fast</li>
 * </ol>
 *
 * @author Carlos
 * @since 3.0.0
 */
public class MqTransportRegistry {

    /** 按类型索引的传输实现（保持注册顺序，便于诊断日志可读） */
    private final Map<MqType, MqTransport> transports = new LinkedHashMap<>();

    /** 已解析的当前传输；为 null 表示当前无可用实现 */
    private final MqTransport current;

    /**
     * @param candidates     容器中全部传输实现
     * @param configuredType 配置的类型
     */
    public MqTransportRegistry(Iterable<MqTransport> candidates, MqType configuredType) {
        for (MqTransport transport : candidates) {
            transports.put(transport.getType(), transport);
        }
        this.current = resolve(configuredType);
    }

    /**
     * 解析当前生效的传输
     *
     * @param configuredType 配置的类型
     * @return 生效的传输；无可用实现时返回 null
     */
    private MqTransport resolve(MqType configuredType) {
        MqType wanted = configuredType == null ? MqType.AUTO : configuredType;
        // 显式声明「不启用传输」：返回空，由上层装配 NoOp 模板（而不是当作配置错误）
        if (wanted == MqType.NONE) {
            return null;
        }
        if (transports.isEmpty()) {
            return null;
        }
        if (wanted != MqType.AUTO) {
            MqTransport transport = transports.get(wanted);
            if (transport == null) {
                throw new MqException(MqErrorCode.MQ_TRANSPORT_NOT_FOUND,
                    "配置的 MQ 类型 [%s] 没有对应的传输实现，已注册：%s".formatted(
                        wanted.getCode(), transports.keySet()));
            }
            return transport;
        }
        if (transports.size() == 1) {
            return transports.values().iterator().next();
        }
        throw new MqException(MqErrorCode.MQ_TRANSPORT_NOT_FOUND,
            "注册了多个 MQ 传输实现 %s，类型为 auto 时无法确定使用哪个，请显式配置 carlos.mq.type"
                .formatted(transports.keySet()));
    }

    /**
     * 当前生效的传输
     *
     * @return 可选传输
     */
    public Optional<MqTransport> currentOrEmpty() {
        return Optional.ofNullable(current);
    }

    /**
     * 当前生效的传输，不存在则报错
     *
     * @return 传输实现
     * @throws MqException 无可用实现时抛出
     */
    public MqTransport current() {
        if (current == null) {
            throw new MqException(MqErrorCode.MQ_TRANSPORT_NOT_FOUND,
                "当前没有可用的 MQ 传输实现，已注册：%s".formatted(transports.keySet()));
        }
        return current;
    }

    /**
     * 按类型查找
     *
     * @param type 类型
     * @return 可选传输
     */
    public Optional<MqTransport> find(MqType type) {
        return Optional.ofNullable(transports.get(type));
    }

    /**
     * 已注册的全部传输
     *
     * @return 不可变视图
     */
    public Map<MqType, MqTransport> all() {
        return Collections.unmodifiableMap(transports);
    }

    /**
     * 已注册的类型集合
     *
     * @return 类型集合
     */
    public Set<MqType> availableTypes() {
        return Collections.unmodifiableSet(transports.keySet());
    }

    /**
     * 是否无任何可用实现
     *
     * @return 无实现时返回 true
     */
    public boolean isEmpty() {
        return transports.isEmpty();
    }
}
