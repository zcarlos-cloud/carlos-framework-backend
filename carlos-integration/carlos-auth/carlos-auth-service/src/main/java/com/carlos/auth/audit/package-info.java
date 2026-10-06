/**
 * carlos-auth 的审计日志投递包。
 * <p>
 * 本模块不存储审计日志，只负责投递：{@link com.carlos.auth.audit.AuditLogPublisher}
 * 是唯一出口，由 {@code carlos.auth.audit.mode} 决定走框架级 MQ（默认）还是回退到
 * Feign 同步调用。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
package com.carlos.auth.audit;
