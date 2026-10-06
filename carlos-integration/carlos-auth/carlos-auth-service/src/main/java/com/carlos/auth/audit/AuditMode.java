package com.carlos.auth.audit;

/**
 * 审计日志的投递方式
 * <p>
 * 提供 {@link #FEIGN} 是为了<b>保留回退路径</b>：MQ 链路出现问题时，
 * 一次配置切换即可回到改造前的同步 Feign 行为，无需回滚代码。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
public enum AuditMode {

    /**
     * 经框架级 MQ 投递（默认）
     * <p>相对 Feign 的收益：一次 Redis 写入（亚毫秒）与一次跨进程 HTTP 往返的差别；
     * 且投递与业务解耦，审计侧不可用不再影响登录链路。</p>
     */
    MQ,

    /**
     * 经 Feign 同步调用 carlos-audit（改造前的行为，保留用于回退）
     */
    FEIGN
}
