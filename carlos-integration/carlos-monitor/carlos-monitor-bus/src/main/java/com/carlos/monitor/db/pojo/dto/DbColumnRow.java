package com.carlos.monitor.db.pojo.dto;

/**
 * <p>
 * 列元数据行
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
public record DbColumnRow(String columnName, String dataType, Boolean nullable,
                          String defaultValue, String comment, Boolean primaryKey) {
}
