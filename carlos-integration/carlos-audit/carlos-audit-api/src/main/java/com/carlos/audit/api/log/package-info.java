/**
 * 跨服务操作日志桥接包。
 * <p>
 * 把 {@code @Log} 注解产生的审计日志经 MQ 投递给 carlos-audit —— 补上「注解只在自己进程内能落库」
 * 这一缺口。与 Feign 契约无关，仅在 {@code starter-log} + {@code starter-mq} 同时存在时生效。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
package com.carlos.audit.api.log;
