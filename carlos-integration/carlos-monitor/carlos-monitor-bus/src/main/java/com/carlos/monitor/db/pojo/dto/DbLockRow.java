package com.carlos.monitor.db.pojo.dto;

/**
 * <p>
 * 数据库锁等待行
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
public record DbLockRow(String lockType, String mode, String relation, String sessionId,
                        Boolean granted, String blockingSessionId, String query) {
}
