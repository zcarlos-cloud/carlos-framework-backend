package com.carlos.audit.clickhouse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * ClickHouse 计数查询结果行
 *
 * @author Carlos
 * @date 2026-09-17
 */
@Data
public class AuditLogCountRow {

    @JsonProperty("total_count")
    private Long totalCount;
}
