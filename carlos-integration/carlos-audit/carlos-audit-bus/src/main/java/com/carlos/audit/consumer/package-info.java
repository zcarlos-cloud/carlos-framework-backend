/**
 * 审计模块的 MQ 消费端包。
 * <p>
 * 承接由 carlos-auth 等生产端通过框架级 MQ 投递的审计日志，转交给既有的写入链路
 * （{@code AuditLogMainService} → Disruptor → ClickHouse）。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
package com.carlos.audit.consumer;
