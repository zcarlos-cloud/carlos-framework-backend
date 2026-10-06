package com.carlos.monitor.db.pojo.dto;

/**
 * <p>
 * 表元数据行
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
public record DbTableRow(String schemaName, String tableName, String tableType,
                         String comment, Long estimatedRows) {
}
