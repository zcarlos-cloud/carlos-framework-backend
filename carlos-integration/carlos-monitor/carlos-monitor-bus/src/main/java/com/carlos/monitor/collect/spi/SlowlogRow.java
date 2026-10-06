package com.carlos.monitor.collect.spi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * <p>
 * Redis 慢日志明细行（对齐 CK 表 monitor_redis_slowlog）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
public record SlowlogRow(
    @JsonProperty("instance_id") Long instanceId,
    @JsonProperty("node") String node,
    @JsonProperty("slowlog_id") Long slowlogId,
    @JsonProperty("ts") Instant ts,
    @JsonProperty("duration_us") Long durationUs,
    @JsonProperty("command") String command,
    @JsonProperty("client_addr") String clientAddr) {
}
