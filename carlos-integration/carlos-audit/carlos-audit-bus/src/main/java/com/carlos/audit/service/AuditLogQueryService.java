package com.carlos.audit.service;

import cn.hutool.core.util.StrUtil;
import com.carlos.audit.api.pojo.enums.AuditErrorCode;
import com.carlos.audit.clickhouse.AuditLogCountRow;
import com.carlos.audit.clickhouse.AuditLogDailyStatsRow;
import com.carlos.audit.clickhouse.AuditLogStatsRow;
import com.carlos.audit.clickhouse.ClickHouseQueryClient;
import com.carlos.audit.clickhouse.ClickHouseQueryRow;
import com.carlos.audit.clickhouse.ClickHouseRowMapper;
import com.carlos.audit.manager.AuditLogMainManager;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.audit.pojo.excel.AuditLogExcel;
import com.carlos.audit.pojo.param.AuditLogQueryPageParam;
import com.carlos.audit.pojo.vo.AuditLogDailyStatsVO;
import com.carlos.audit.pojo.vo.AuditLogStatsVO;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.pagination.Paging;
import com.carlos.util.easyexcel.ExcelUtil;
import com.clickhouse.data.value.ClickHouseLongValue;
import com.clickhouse.data.value.ClickHouseStringValue;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 审计日志查询服务实现
 * </p>
 *
 * <p>基于 ClickHouse 热数据表 {@code audit_log_main_local} 提供分页检索、详情、实时统计、
 * 按天趋势统计与导出能力。</p>
 *
 * @author Carlos
 * @date 2026-03-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private static final DateTimeFormatter DATETIME_PARAM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final int MAX_TRAIL_SIZE = 1000;
    private static final int MAX_RISK_SIZE = 1000;
    private static final int MAX_PAGE_SIZE = 500;

    /**
     * 单次导出的最大行数，避免一次性拉取过多数据压垮服务
     */
    private static final int MAX_EXPORT_SIZE = 50_000;

    /**
     * 分页 OFFSET 上限，防止 current 过大导致 int 溢出
     */
    private static final long MAX_OFFSET = 10_000_000L;

    /**
     * 导出文件名称前缀
     */
    private static final String EXPORT_FILE_NAME = "审计日志";

    /**
     * 导出投影列
     *
     * <p>与 {@link AuditLogExcel} 的字段一一对应。**必须显式列举**而非 {@code SELECT *}：
     * 宽表含 old_data / new_data / target_snapshot / trace_path 等大文本 JSON，
     * 导出上限 5 万行时整行拉取会造成数百 MB 的内存尖峰，足以拖垮审计服务。</p>
     */
    private static final String EXPORT_COLUMNS = "id, server_time, category, log_type, risk_level, log_status, "
        + "operation, principal_id, principal_name, tenant_id, target_type, target_id, target_name, "
        + "client_ip, biz_channel, duration_ms, result_code, result_message, trace_id, app_name";

    /**
     * ClickHouse 热数据表
     */
    private static final String TABLE_NAME = "audit_log_main_local";

    private final AuditLogMainManager auditLogMainManager;
    private final ClickHouseQueryClient queryClient;

    /**
     * 分页查询审计日志
     *
     * @param tenantId 租户ID
     * @param param    查询参数（分页 + 多维度筛选）
     * @return 分页结果
     */
    public Paging<AuditLogMainDTO> pageQuery(String tenantId, AuditLogQueryPageParam param) {
        int current = param.getCurrent();
        int size = param.getSize();
        log.debug("分页查询审计日志，tenantId={}, current={}, size={}", tenantId, current, size);
        validatePageParam(current, size);

        SqlBuilder sql = buildFilterQuery(tenantId, param);

        Long total = queryTotal(sql);
        if (total == null || total == 0L) {
            return emptyPaging(current, size);
        }

        int offset = (current - 1) * size;
        sql.orderBy("server_time DESC").limit(size).offset(offset);
        List<ClickHouseQueryRow> rows = queryClient.queryForList(
            sql.buildSelect("*"), ClickHouseQueryRow.class, sql.params());
        List<AuditLogMainDTO> records = rows.stream().map(ClickHouseRowMapper::toDto).toList();

        Paging<AuditLogMainDTO> paging = new Paging<>();
        paging.setTotal(total.intValue());
        paging.setRecords(records);
        paging.setCurrent(current);
        paging.setSize(size);
        paging.setPages((int) Math.ceil((double) total / size));
        return paging;
    }

    /**
     * 查询单条日志详情
     *
     * @param id 日志ID
     * @return 日志DTO，不存在时返回 null
     */
    public AuditLogMainDTO getById(Long id) {
        log.debug("查询审计日志详情，id={}", id);
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE id = :id LIMIT 1";
        ClickHouseQueryRow row = queryClient.queryForObject(sql,
            ClickHouseQueryRow.class, ClickHouseLongValue.of(id));
        return ClickHouseRowMapper.toDto(row);
    }

    /**
     * 获取实时统计
     *
     * @param tenantId 租户ID
     * @param date     统计日期
     * @return 统计VO
     */
    public AuditLogStatsVO getRealtimeStats(String tenantId, LocalDate date) {
        log.debug("获取实时统计，tenantId={}, date={}", tenantId, date);
        String sql = """
            SELECT
              event_date AS date,
              count() AS total_count,
              countIf(log_status = 'SUCCESS') AS success_count,
              countIf(log_status = 'FAIL') AS fail_count,
              count(DISTINCT principal_id) AS unique_users,
              count(DISTINCT target_id) AS unique_targets,
              sum(duration_ms) AS total_duration,
              avg(duration_ms) AS avg_duration,
              max(risk_level) AS max_risk_level,
              sum(attachment_count) AS total_attachments,
              sum(monetary_amount) AS total_amount
            FROM audit_log_main_local
            WHERE tenant_id = :tenantId AND event_date = :date
            GROUP BY event_date
            """;
        AuditLogStatsRow row = queryClient.queryForObject(sql, AuditLogStatsRow.class,
            ClickHouseStringValue.of(tenantId), ClickHouseStringValue.of(date.toString()));
        return ClickHouseRowMapper.toStatsVo(row);
    }

    /**
     * 获取按天聚合统计（趋势图数据源）
     *
     * @param tenantId  租户ID
     * @param startDate 开始日期（含）
     * @param endDate   结束日期（含）
     * @return 按天聚合的统计列表，按日期升序
     */
    public List<AuditLogDailyStatsVO> getDailyStats(String tenantId, LocalDate startDate, LocalDate endDate) {
        log.debug("获取按天统计，tenantId={}, startDate={}, endDate={}", tenantId, startDate, endDate);
        if (startDate == null || endDate == null) {
            throw AuditErrorCode.AUDIT_QUERY_TIME_RANGE_INVALID.exception("startDate 与 endDate 不能为空");
        }
        if (startDate.isAfter(endDate)) {
            throw AuditErrorCode.AUDIT_QUERY_TIME_RANGE_INVALID.exception("startDate 不能晚于 endDate");
        }

        String sql = """
            SELECT
              event_date AS date,
              count() AS total_count,
              countIf(log_status = 'SUCCESS') AS success_count,
              countIf(log_status = 'FAIL') AS fail_count,
              count(DISTINCT principal_id) AS unique_users,
              avg(duration_ms) AS avg_duration,
              max(risk_level) AS max_risk_level
            FROM audit_log_main_local
            WHERE tenant_id = :tenantId
              AND event_date >= toDate(:startDate)
              AND event_date <= toDate(:endDate)
            GROUP BY event_date
            ORDER BY event_date ASC
            """;

        List<AuditLogDailyStatsRow> rows = queryClient.queryForList(sql, AuditLogDailyStatsRow.class,
            ClickHouseStringValue.of(tenantId),
            ClickHouseStringValue.of(startDate.toString()),
            ClickHouseStringValue.of(endDate.toString()));

        List<AuditLogDailyStatsVO> result = new ArrayList<>(rows.size());
        for (AuditLogDailyStatsRow row : rows) {
            result.add(toDailyStatsVo(row));
        }
        return result;
    }

    /**
     * 查询用户行为轨迹
     *
     * @param principalId 操作主体ID
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @return 轨迹列表
     */
    public List<AuditLogMainDTO> getUserTrail(String principalId, LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("查询用户行为轨迹，principalId={}", principalId);
        return auditLogMainManager.getUserTrail(principalId, startTime, endTime).stream()
            .limit(MAX_TRAIL_SIZE).toList();
    }

    /**
     * 查询风险事件
     *
     * @param tenantId     租户ID
     * @param minRiskLevel 最小风险等级
     * @param startTime    开始时间
     * @param endTime      结束时间
     * @return 风险事件列表
     */
    public List<AuditLogMainDTO> listRiskEvents(String tenantId, int minRiskLevel,
                                                LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("查询风险事件，tenantId={}, minRiskLevel={}", tenantId, minRiskLevel);
        SqlBuilder sql = new SqlBuilder(TABLE_NAME);
        sql.where("tenant_id = :tenantId").param(ClickHouseStringValue.of(tenantId))
            .where("risk_level >= :minRiskLevel").param(ClickHouseStringValue.of(String.valueOf(minRiskLevel)));
        if (startTime != null) {
            sql.where("server_time >= parseDateTimeBestEffort(:startTime)")
                .param(ClickHouseStringValue.of(formatDateTime(startTime)));
        }
        if (endTime != null) {
            sql.where("server_time <= parseDateTimeBestEffort(:endTime)")
                .param(ClickHouseStringValue.of(formatDateTime(endTime)));
        }
        sql.orderBy("risk_level DESC, server_time DESC").limit(MAX_RISK_SIZE);

        List<ClickHouseQueryRow> rows = queryClient.queryForList(
            sql.buildSelect("*"), ClickHouseQueryRow.class, sql.params());
        return rows.stream().map(ClickHouseRowMapper::toDto).toList();
    }

    /**
     * 按当前筛选条件导出审计日志为 Excel
     *
     * @param tenantId 租户ID
     * @param param    查询参数（分页字段被忽略）
     * @param response HTTP 响应
     */
    public void exportLogs(String tenantId, AuditLogQueryPageParam param, HttpServletResponse response) {
        log.info("导出审计日志，tenantId={}", tenantId);
        List<AuditLogMainDTO> records = listForExport(tenantId, param);

        List<AuditLogExcel> data = new ArrayList<>(records.size());
        for (AuditLogMainDTO dto : records) {
            data.add(toExcel(dto));
        }

        try {
            ExcelUtil.download(response, buildExportFileName(), AuditLogExcel.class, data);
        } catch (Exception e) {
            log.error("导出审计日志失败", e);
            throw AuditErrorCode.AUDIT_QUERY_EXPORT_FAILED.exception();
        }
    }

    /**
     * 查询导出数据（不带分页，受 {@link #MAX_EXPORT_SIZE} 限制）
     *
     * @param tenantId 租户ID
     * @param param    查询参数
     * @return 待导出数据
     */
    private List<AuditLogMainDTO> listForExport(String tenantId, AuditLogQueryPageParam param) {
        SqlBuilder sql = buildFilterQuery(tenantId, param);

        Long total = queryTotal(sql);
        if (total != null && total > MAX_EXPORT_SIZE) {
            throw AuditErrorCode.AUDIT_QUERY_EXPORT_LIMIT_EXCEEDED.exception(
                "匹配记录数为 " + total + "，超过单次导出上限 " + MAX_EXPORT_SIZE + " 条，请缩小查询范围");
        }

        sql.orderBy("server_time DESC").limit(MAX_EXPORT_SIZE);
        List<ClickHouseQueryRow> rows = queryClient.queryForList(
            sql.buildSelect(EXPORT_COLUMNS), ClickHouseQueryRow.class, sql.params());
        return rows.stream().map(ClickHouseRowMapper::toDto).toList();
    }

    /**
     * 构建带筛选条件的查询构造器
     *
     * @param tenantId 租户ID
     * @param param    查询参数
     * @return SQL 构造器
     */
    private SqlBuilder buildFilterQuery(String tenantId, AuditLogQueryPageParam param) {
        SqlBuilder sql = new SqlBuilder(TABLE_NAME);
        sql.where("tenant_id = :tenantId").param(ClickHouseStringValue.of(tenantId));

        if (param.getStartTime() != null) {
            sql.where("server_time >= parseDateTimeBestEffort(:startTime)")
                .param(ClickHouseStringValue.of(formatDateTime(param.getStartTime())));
        }
        if (param.getEndTime() != null) {
            sql.where("server_time <= parseDateTimeBestEffort(:endTime)")
                .param(ClickHouseStringValue.of(formatDateTime(param.getEndTime())));
        }

        appendEquals(sql, "category", "category", param.getCategory());
        appendEquals(sql, "log_type", "logType", param.getLogType());
        appendEquals(sql, "log_status", "logStatus", param.getLogStatus());
        appendEquals(sql, "principal_id", "principalId", param.getPrincipalId());
        appendEquals(sql, "principal_type", "principalType", param.getPrincipalType());
        appendEquals(sql, "target_id", "targetId", param.getTargetId());
        appendEquals(sql, "target_type", "targetType", param.getTargetType());
        appendEquals(sql, "biz_channel", "bizChannel", param.getBizChannel());
        appendEquals(sql, "client_ip", "clientIp", param.getClientIp());
        appendEquals(sql, "auth_type", "authType", param.getAuthType());
        appendEquals(sql, "result_code", "resultCode", param.getResultCode());

        if (StrUtil.isNotBlank(param.getPrincipalName())) {
            sql.where("positionCaseInsensitive(principal_name, :principalName) > 0")
                .param(ClickHouseStringValue.of(param.getPrincipalName().trim()));
        }
        if (param.getRiskLevelMin() != null) {
            sql.where("risk_level >= :riskLevelMin")
                .param(ClickHouseLongValue.of(param.getRiskLevelMin().longValue()));
        }
        if (param.getRiskLevelMax() != null) {
            sql.where("risk_level <= :riskLevelMax")
                .param(ClickHouseLongValue.of(param.getRiskLevelMax().longValue()));
        }

        return sql;
    }

    /**
     * 追加等值筛选条件（值为空时忽略）
     *
     * @param sql       SQL 构造器
     * @param column    数据库列名
     * @param paramName 参数占位符名称
     * @param value     筛选值
     */
    private void appendEquals(SqlBuilder sql, String column, String paramName, String value) {
        if (StrUtil.isNotBlank(value)) {
            sql.where(column + " = :" + paramName).param(ClickHouseStringValue.of(value.trim()));
        }
    }

    private Long queryTotal(SqlBuilder sql) {
        AuditLogCountRow row = queryClient.queryForObject(sql.buildCount(), AuditLogCountRow.class, sql.params());
        return row != null ? row.getTotalCount() : 0L;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATETIME_PARAM_FORMATTER);
    }

    private void validatePageParam(int current, int size) {
        if (current < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw AuditErrorCode.AUDIT_QUERY_PARAM_INVALID.exception(
                "current 必须大于 0，size 必须在 1-" + MAX_PAGE_SIZE + " 之间");
        }
        // offset 以 int 参与运算：过大的 current 会溢出为负数，使 OFFSET 子句被跳过而静默返回第一页
        if ((long) (current - 1) * size > MAX_OFFSET) {
            throw AuditErrorCode.AUDIT_QUERY_PARAM_INVALID.exception(
                "current 过大，请缩小页码或改用时间范围筛选");
        }
    }

    private Paging<AuditLogMainDTO> emptyPaging(int current, int size) {
        Paging<AuditLogMainDTO> paging = new Paging<>();
        paging.setTotal(0);
        paging.setRecords(Collections.emptyList());
        paging.setCurrent(current);
        paging.setSize(size);
        paging.setPages(0);
        return paging;
    }

    private AuditLogDailyStatsVO toDailyStatsVo(AuditLogDailyStatsRow row) {
        if (row == null) {
            return null;
        }
        AuditLogDailyStatsVO vo = new AuditLogDailyStatsVO();
        vo.setDate(StrUtil.isBlank(row.getDate()) ? null : LocalDate.parse(row.getDate()));
        vo.setTotalCount(row.getTotalCount());
        vo.setSuccessCount(row.getSuccessCount());
        vo.setFailCount(row.getFailCount());
        vo.setUniqueUsers(row.getUniqueUsers());
        vo.setAvgDuration(row.getAvgDuration());
        vo.setMaxRiskLevel(row.getMaxRiskLevel());
        return vo;
    }

    private AuditLogExcel toExcel(AuditLogMainDTO dto) {
        AuditLogExcel excel = new AuditLogExcel();
        excel.setId(dto.getId());
        excel.setServerTime(dto.getServerTime() == null ? null : dto.getServerTime().format(DATETIME_PARAM_FORMATTER));
        excel.setCategory(descOf(dto.getCategory()));
        excel.setLogType(dto.getLogType());
        excel.setRiskLevel(dto.getRiskLevel());
        excel.setLogStatus(descOf(dto.getLogStatus()));
        excel.setOperation(dto.getOperation());
        excel.setPrincipalId(dto.getPrincipalId());
        excel.setPrincipalName(dto.getPrincipalName());
        excel.setTenantId(dto.getTenantId());
        excel.setTargetType(dto.getTargetType());
        excel.setTargetId(dto.getTargetId());
        excel.setTargetName(dto.getTargetName());
        excel.setClientIp(dto.getClientIp());
        excel.setBizChannel(descOf(dto.getBizChannel()));
        excel.setDurationMs(dto.getDurationMs());
        excel.setResultCode(dto.getResultCode());
        excel.setResultMessage(dto.getResultMessage());
        excel.setTraceId(dto.getTraceId());
        excel.setAppName(dto.getAppName());
        return excel;
    }

    private String descOf(BaseEnum<?> baseEnum) {
        return baseEnum == null ? null : baseEnum.getDesc();
    }

    private String buildExportFileName() {
        return EXPORT_FILE_NAME + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * 简易 SQL 构建器（仅支持本类需要的简单拼接）
     */
    private static class SqlBuilder {
        private final String table;
        private final List<String> conditions = new ArrayList<>();
        private final List<com.clickhouse.data.ClickHouseValue> params = new ArrayList<>();
        private String orderBy;
        private int limit;
        private int offset;

        SqlBuilder(String table) {
            this.table = table;
        }

        SqlBuilder where(String condition) {
            conditions.add(condition);
            return this;
        }

        SqlBuilder param(com.clickhouse.data.ClickHouseValue value) {
            params.add(value);
            return this;
        }

        SqlBuilder orderBy(String orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        SqlBuilder limit(int limit) {
            this.limit = limit;
            return this;
        }

        SqlBuilder offset(int offset) {
            this.offset = offset;
            return this;
        }

        String buildSelect(String columns) {
            StringBuilder sb = new StringBuilder("SELECT ").append(columns).append(" FROM ").append(table);
            appendWhere(sb);
            if (orderBy != null && !orderBy.isBlank()) {
                sb.append(" ORDER BY ").append(orderBy);
            }
            if (limit > 0) {
                sb.append(" LIMIT ").append(limit);
            }
            if (offset > 0) {
                sb.append(" OFFSET ").append(offset);
            }
            return sb.toString();
        }

        String buildCount() {
            StringBuilder sb = new StringBuilder("SELECT count() AS total_count FROM ").append(table);
            appendWhere(sb);
            return sb.toString();
        }

        private void appendWhere(StringBuilder sb) {
            if (!conditions.isEmpty()) {
                sb.append(" WHERE ");
                for (int i = 0; i < conditions.size(); i++) {
                    if (i > 0) {
                        sb.append(" AND ");
                    }
                    sb.append(conditions.get(i));
                }
            }
        }

        com.clickhouse.data.ClickHouseValue[] params() {
            return params.toArray(new com.clickhouse.data.ClickHouseValue[0]);
        }
    }
}
