package com.carlos.monitor.db.pojo.dto;

/**
 * <p>
 * 库表容量行（每日快照采集）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
public record CapacityRow(String schemaName, String tableName, Long tableRows,
                          Long tableSizeBytes, Long indexSizeBytes) {
}
