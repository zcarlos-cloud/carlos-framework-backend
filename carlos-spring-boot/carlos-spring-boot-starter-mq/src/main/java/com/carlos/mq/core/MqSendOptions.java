package com.carlos.mq.core;

import java.util.HashMap;
import java.util.Map;

/**
 * 发送选项（不可变）
 * <p>
 * 作为 {@link MqTemplate#send(String, Object, MqSendOptions)} 的可选参数载体。
 * </p>
 *
 * <p>
 * 不可变性由语言保证（record）；每个 {@code withXxx} 方法返回<b>新实例</b>，
 * 不修改原对象。
 * </p>
 *
 * <h2>为什么只有两个字段</h2>
 * <p>
 * 收敛前本类还有 {@code maxLength} 与 {@code deliverAt} 两个字段，均已被删除——
 * 它们的共同点是<b>声明了却做不到</b>，正是本模块明令禁止的失败模式：
 * </p>
 * <ul>
 *   <li><b>{@code maxLength}</b>：从未被传输读取。长度上限是<b>按主题</b>的物理量，
 *       配置入口是 {@code carlos.mq.topics.<topic>.max-length}；做成逐消息选项既无法生效，
 *       又要求把 {@code MAXLEN} 这个 Redis Stream 概念塞进传输中立的
 *       {@link MqMessage}，与「传输中立」相悖。</li>
 *   <li><b>{@code deliverAt}</b>：同样从未被读取，且后果比前者严重——
 *       调用方指定「明天投递」会得到<b>立即投递</b>（{@code send} 只读 key 与 headers），
 *       是静默的正确性错误。需要延时投递请用
 *       {@link MqTemplate#sendDelayed(String, Object, java.time.Duration)}。</li>
 * </ul>
 *
 * @param key     业务键（用于分区、去重、查询定位；可为 null）
 * @param headers 自定义消息头（不可变 Map）
 * @author Carlos
 * @since 3.0.0
 */
public record MqSendOptions(
    String key,
    Map<String, String> headers
) {

    /**
     * 紧凑构造器：把可变 Map 复制为不可变，并兜住 null。
     */
    public MqSendOptions {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    /**
     * 空选项（等价于不带选项的 send）
     *
     * @return 空选项实例
     */
    public static MqSendOptions none() {
        return new MqSendOptions(null, null);
    }

    /**
     * 以业务键构造选项
     *
     * @param key 业务键
     * @return 选项实例
     */
    public static MqSendOptions ofKey(String key) {
        return new MqSendOptions(key, null);
    }

    public MqSendOptions withKey(String newKey) {
        return new MqSendOptions(newKey, headers);
    }

    public MqSendOptions withHeader(String name, String value) {
        Map<String, String> merged = new HashMap<>(headers);
        merged.put(name, value);
        return new MqSendOptions(key, merged);
    }
}
