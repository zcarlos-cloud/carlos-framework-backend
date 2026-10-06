package com.carlos.monitor.db.controller;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.monitor.audit.MonitorAudit;
import com.carlos.monitor.db.pojo.dto.SqlExecResult;
import com.carlos.monitor.db.pojo.param.SqlExecuteParam;
import com.carlos.monitor.db.pojo.param.SqlHistoryPageParam;
import com.carlos.monitor.db.pojo.vo.SqlExecResultVO;
import com.carlos.monitor.db.pojo.vo.SqlHistoryVO;
import com.carlos.monitor.db.service.SqlConsoleService;
import com.carlos.monitor.redis.pojo.vo.ConfirmTokenVO;
import com.carlos.monitor.config.MonitorProperties;
import com.carlos.monitor.redis.proxy.RedisWriteGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * SQL 控制台 rest服务接口（只读 + 受控写 + 执行历史）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/monitor/db")
@Tag(name = "SQL控制台")
public class SqlConsoleController {

    private final SqlConsoleService sqlConsoleService;

    private final RedisWriteGuard writeGuard;

    private final MonitorProperties monitorProperties;

    @MonitorAudit(action = "DB_SQL_READONLY", target = "#id", snapshot = false, riskLevel = 40)
    @PreAuthorize("hasPermission('monitor:db:sql', 'readonly')")
    @PostMapping("/{id}/sql-console/execute")
    @Operation(summary = "只读执行", description = "SELECT/EXPLAIN，自动包裹 LIMIT，结果列级脱敏")
    public Result<SqlExecResultVO> execute(@PathVariable("id") Long id,
                                           @RequestBody @Validated SqlExecuteParam param,
                                           HttpServletRequest request) {
        SqlExecResult result = sqlConsoleService.executeReadonly(id, param.getSql(), clientIp(request));
        return Result.success(toVO(result));
    }

    @MonitorAudit(action = "DB_SQL_WRITE", target = "#id", snapshot = false, riskLevel = 80)
    @PreAuthorize("hasPermission('monitor:db:sql', 'write')")
    @PostMapping("/{id}/sql-console/execute-write")
    @Operation(summary = "写执行", description = "高危：sql_write_enabled + X-Confirm-Token + 行数保护")
    public Result<SqlExecResultVO> executeWrite(@PathVariable("id") Long id,
                                                @RequestBody @Validated SqlExecuteParam param,
                                                @RequestHeader(value = "X-Confirm-Token", required = false)
                                                String confirmToken,
                                                HttpServletRequest request) {
        int affected = sqlConsoleService.executeWrite(id, param.getSql(), confirmToken, clientIp(request));
        SqlExecResultVO vo = new SqlExecResultVO();
        vo.setAffectedRows(affected);
        return Result.success(vo);
    }

    @PreAuthorize("hasPermission('monitor:db:sql', 'write')")
    @PostMapping("/{id}/sql-console/confirm-token")
    @Operation(summary = "签发写执行确认令牌", description = "入参 SQL 摘要，60s 一次性，绑定实例与 SQL 指纹")
    public Result<ConfirmTokenVO> confirmToken(@PathVariable("id") Long id,
                                               @RequestBody @Validated SqlExecuteParam param) {
        String operation = sqlConsoleService.buildWriteOperation(id, param.getSql());
        String token = writeGuard.issueConfirmToken(id, operation);
        ConfirmTokenVO vo = new ConfirmTokenVO();
        vo.setToken(token);
        vo.setTtlSeconds(monitorProperties.getRedis().getConfirmTokenTtlSec());
        return Result.success(vo);
    }

    @PreAuthorize("hasPermission('monitor:db', 'view')")
    @GetMapping("/{id}/sql-history/page")
    @Operation(summary = "执行历史分页")
    public Result<Paging<SqlHistoryVO>> historyPage(@PathVariable("id") Long id, SqlHistoryPageParam param) {
        param.setInstanceId(id);
        return Result.success(sqlConsoleService.getHistoryPage(param));
    }

    private SqlExecResultVO toVO(SqlExecResult result) {
        SqlExecResultVO vo = new SqlExecResultVO();
        vo.setColumns(result.columns());
        vo.setRows(result.rows());
        vo.setAffectedRows(result.affectedRows());
        vo.setTruncated(result.truncated());
        return vo;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
