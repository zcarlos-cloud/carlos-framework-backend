package com.carlos.monitor.db.pojo.dto;

/**
 * <p>
 * 数据库会话行
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
public record DbSessionRow(String sessionId, String username, String dbName, String state,
                           String query, Double durationSec, String clientAddr, String backendStart) {
}
