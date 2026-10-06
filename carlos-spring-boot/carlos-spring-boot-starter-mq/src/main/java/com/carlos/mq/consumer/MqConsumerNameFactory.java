package com.carlos.mq.consumer;

import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 消费者名工厂 —— 生成<b>每实例唯一</b>的消费者名
 *
 * <h2>这修的正是既有实现的严重缺陷</h2>
 * <p>
 * 既有 {@code RedisStreamMessageQueue} 使用配置常量 {@code "message-consumer-1"} 作为
 * 消费者名。多副本部署时，多个进程以<b>同一个消费者名</b>加入同一消费组，
 * {@code XREADGROUP} 会把它们视作同一个消费者 —— 消息被瓜分、待处理队列归属混乱，
 * 最终表现为漏投与重复投递并存。
 * </p>
 *
 * <h2>设计要点</h2>
 * <ul>
 *   <li><b>进程内稳定</b>：名字在构造时算一次并缓存，重连/重订阅不会换名
 *       （否则会把自己变成「死消费者」，遗留一堆待处理条目）。</li>
 *   <li><b>进程间唯一</b>：包含主机名、进程 id 与随机串。</li>
 *   <li><b>重启后名字会变</b>：这是期望行为 —— 旧待处理条目归「死消费者」，
 *       由认领器按空闲时长接管，而不是被同一个名字继续背着。</li>
 * </ul>
 *
 * @author Carlos
 * @since 3.0.0
 */
public class MqConsumerNameFactory {

    /** 名字长度上限（便于日志与 XINFO CONSUMERS 输出可读） */
    private static final int MAX_LENGTH = 96;

    private final String base;

    public MqConsumerNameFactory(Environment environment) {
        String app = environment == null
            ? "app" : environment.getProperty("spring.application.name", "app");
        String host = resolveHost();
        long pid = ProcessHandle.current().pid();
        String random = Integer.toString(ThreadLocalRandom.current().nextInt(36 * 36 * 36 * 36), 36);
        this.base = truncate("%s:%s:%d:%s".formatted(app, host, pid, random));
    }

    /**
     * 单消费者场景的名字
     *
     * @return 消费者名
     */
    public String next() {
        return base;
    }

    /**
     * 并发消费者场景下第 index 个消费者的名字
     * <p>同组内多个消费者名会分摊消息——这正是并发消费想要的效果。</p>
     *
     * @param index 序号
     * @return 消费者名
     */
    public String next(int index) {
        return truncate(base + "#" + index);
    }

    /**
     * 基准名（不含并发后缀），用于日志
     *
     * @return 基准名
     */
    public String base() {
        return base;
    }

    private static String resolveHost() {
        String env = System.getenv("HOSTNAME");
        if (env != null && !env.isBlank()) {
            return env;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String truncate(String value) {
        return value.length() <= MAX_LENGTH ? value : value.substring(0, MAX_LENGTH);
    }
}
