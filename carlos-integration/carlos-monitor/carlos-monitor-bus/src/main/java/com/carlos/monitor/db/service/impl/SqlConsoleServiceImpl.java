package com.carlos.monitor.db.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.druid.DbType;
import com.carlos.core.exception.BusinessException;
import com.carlos.core.pagination.Paging;
import com.carlos.monitor.collect.spi.SqlFingerprintUtil;
import com.carlos.monitor.db.manager.MonitorSqlHistoryManager;
import com.carlos.monitor.db.pojo.dto.SqlExecResult;
import com.carlos.monitor.db.pojo.entity.MonitorSqlHistory;
import com.carlos.monitor.db.pojo.param.SqlHistoryPageParam;
import com.carlos.monitor.db.pojo.vo.SqlHistoryVO;
import com.carlos.monitor.db.proxy.DbDataProxy;
import com.carlos.monitor.db.proxy.SqlGuard;
import com.carlos.monitor.db.service.SqlConsoleService;
import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.enums.MonitorErrorCode;
import com.carlos.monitor.instance.manager.MonitorInstanceManager;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import com.carlos.monitor.redis.proxy.RedisWriteGuard;
import com.carlos.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * SQL 控制台 业务接口实现类
 * 执行编排：实例校验（manage_enabled/sql_write_enabled）→ SqlGuard → 执行
 * → 结果列级脱敏（DbDataProxy 内）→ monitor_sql_history 落库（BLOCKED 同样留档）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlConsoleServiceImpl implements SqlConsoleService {

    private static final String STATUS_SUCCESS = "SUCCESS";

    private static final String STATUS_FAILED = "FAILED";

    private static final String STATUS_BLOCKED = "BLOCKED";

    private final MonitorInstanceManager instanceManager;

    private final MonitorSqlHistoryManager sqlHistoryManager;

    private final SqlGuard sqlGuard;

    private final DbDataProxy dbDataProxy;

    private final RedisWriteGuard writeGuard;

    @Override
    public SqlExecResult executeReadonly(Long instanceId, String sql, String clientIp) {
        MonitorInstanceDTO instance = checkInstance(instanceId);
        long start = System.currentTimeMillis();
        SqlGuard.GuardResult guardResult = sqlGuard.check(sql, toDruidDbType(instance), SqlGuard.SqlMode.READONLY);
        if (!guardResult.pass()) {
            saveHistory(instanceId, sql, null, STATUS_BLOCKED, guardResult.blockReason(), null, null,
                System.currentTimeMillis() - start, clientIp);
            throw MonitorErrorCode.MONITOR_SQL_BLOCKED.exception(guardResult.blockReason());
        }
        try {
            SqlExecResult result = dbDataProxy.executeReadonly(instanceId, sql, guardResult.sqlType());
            saveHistory(instanceId, sql, guardResult.sqlType().name(), STATUS_SUCCESS, null, null,
                result.rows().size(), System.currentTimeMillis() - start, clientIp);
            return result;
        } catch (BusinessException e) {
            saveHistory(instanceId, sql, guardResult.sqlType().name(), STATUS_FAILED,
                StrUtil.maxLength(e.getMessage(), 500), null, null,
                System.currentTimeMillis() - start, clientIp);
            throw e;
        }
    }

    @Override
    public int executeWrite(Long instanceId, String sql, String confirmToken, String clientIp) {
        MonitorInstanceDTO instance = checkInstance(instanceId);
        if (Boolean.FALSE.equals(instance.getSqlWriteEnabled())) {
            throw MonitorErrorCode.MONITOR_SQL_WRITE_DISABLED.exception();
        }
        long start = System.currentTimeMillis();
        SqlGuard.GuardResult guardResult = sqlGuard.check(sql, toDruidDbType(instance), SqlGuard.SqlMode.WRITE);
        if (!guardResult.pass()) {
            saveHistory(instanceId, sql, null, STATUS_BLOCKED, guardResult.blockReason(), null, null,
                System.currentTimeMillis() - start, clientIp);
            throw MonitorErrorCode.MONITOR_SQL_BLOCKED.exception(guardResult.blockReason());
        }
        if (guardResult.sqlType() == SqlGuard.SqlType.SELECT || guardResult.sqlType() == SqlGuard.SqlType.EXPLAIN) {
            throw MonitorErrorCode.MONITOR_SQL_BLOCKED.exception("写执行入口仅接受 UPDATE/DELETE");
        }
        // 高危确认令牌（绑定实例 + SQL 指纹，防篡改）
        writeGuard.consumeConfirmToken(instanceId, buildWriteOperation(instanceId, sql), confirmToken);
        try {
            int affected = dbDataProxy.executeWrite(instanceId, sql);
            saveHistory(instanceId, sql, guardResult.sqlType().name(), STATUS_SUCCESS, null, affected, null,
                System.currentTimeMillis() - start, clientIp);
            return affected;
        } catch (BusinessException e) {
            saveHistory(instanceId, sql, guardResult.sqlType().name(), STATUS_FAILED,
                StrUtil.maxLength(e.getMessage(), 500), null, null,
                System.currentTimeMillis() - start, clientIp);
            throw e;
        }
    }

    @Override
    public String buildWriteOperation(Long instanceId, String sql) {
        MonitorInstanceDTO instance = checkInstance(instanceId);
        return "sql-console:write:" + SqlFingerprintUtil.fingerprint(sql, toDruidDbType(instance));
    }

    @Override
    public Paging<SqlHistoryVO> getHistoryPage(SqlHistoryPageParam param) {
        return sqlHistoryManager.getPage(param);
    }

    private MonitorInstanceDTO checkInstance(Long instanceId) {
        MonitorInstanceDTO instance = instanceManager.getDtoById(instanceId);
        if (instance == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception();
        }
        if (Boolean.FALSE.equals(instance.getManageEnabled())) {
            throw MonitorErrorCode.MONITOR_MANAGE_DISABLED.exception();
        }
        return instance;
    }

    /**
     * 执行历史落库（SQL 参数占位化脱敏，截断 5000；失败不阻断主流程）
     */
    private void saveHistory(Long instanceId, String sql, String sqlType, String execStatus,
                             String blockReason, Integer affectedRows, Integer resultRows,
                             long durationMs, String clientIp) {
        try {
            MonitorSqlHistory history = new MonitorSqlHistory();
            history.setInstanceId(instanceId);
            Long userId = SecurityUtils.getCurrentUserId();
            history.setExecuteBy(userId == null ? 0L : userId);
            history.setSqlText(StrUtil.maxLength(SqlFingerprintUtil.normalize(sql, null), 5000));
            history.setSqlType(StrUtil.nullToDefault(sqlType, "UNKNOWN"));
            history.setExecStatus(execStatus);
            history.setBlockReason(blockReason);
            history.setAffectedRows(affectedRows);
            history.setResultRows(resultRows);
            history.setDurationMs(durationMs);
            history.setClientIp(clientIp);
            history.setExecuteTime(LocalDateTime.now());
            sqlHistoryManager.addHistory(history);
        } catch (Exception e) {
            log.warn("SQL 执行历史落库失败（已容忍）, instanceId:{}, error:{}", instanceId, e.getMessage());
        }
    }

    private DbType toDruidDbType(MonitorInstanceDTO instance) {
        InstanceTypeEnum type = instance.getInstanceType();
        return switch (type == null ? InstanceTypeEnum.POSTGRESQL : type) {
            case POSTGRESQL -> DbType.postgresql;
            case MYSQL -> DbType.mysql;
            case DM -> DbType.dm;
            case CLICKHOUSE -> DbType.clickhouse;
            default -> DbType.postgresql;
        };
    }
}
