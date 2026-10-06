package com.carlos.audit.api.constant;

/**
 * 审计模块的 MQ 主题常量
 * <p>
 * <b>放在 api 模块、由生产端与消费端共用</b>：这样「主题叫什么」只有一处定义，
 * 不存在两端字符串各写一遍而漂移的可能。漂移的后果是消息发出去没人消费、
 * 且双方都不报错——属于最难排查的一类问题。
 * </p>
 * <p>
 * 命名遵循项目 Redis Key 的 {@code {模块}:{业务}} 约定（本主题为 {@code audit:log}），
 * 最终物理键由 MQ 框架加前缀构成（默认 {@code framework:mq:stream:audit:log}）。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
public final class AuditMqTopics {

    /** 审计日志主题 */
    public static final String AUDIT_LOG = "audit:log";

    /** 审计日志消费组（语义命名：写审计日志的人） */
    public static final String AUDIT_LOG_GROUP = "audit:log:writer";

    private AuditMqTopics() {
    }
}
