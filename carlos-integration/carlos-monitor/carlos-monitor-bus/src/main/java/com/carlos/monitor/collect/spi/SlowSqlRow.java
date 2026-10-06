package com.carlos.monitor.collect.spi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * <p>
 * 慢 SQL 明细行（对齐 CK 表 monitor_db_slow_sql）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
public record SlowSqlRow(
    @JsonProperty("instance_id") Long instanceId,
    @JsonProperty("fingerprint") String fingerprint,
    @JsonProperty("sample_sql") String sampleSql,
    @JsonProperty("db_name") String dbName,
    @JsonProperty("exec_count") Long execCount,
    @JsonProperty("avg_ms") Double avgMs,
    @JsonProperty("max_ms") Double maxMs,
    @JsonProperty("last_exec_time") Instant lastExecTime,
    @JsonProperty("collect_time") Instant collectTime) {
}
