package com.carlos.monitor.db.service;

import com.carlos.core.pagination.Paging;
import com.carlos.monitor.db.pojo.dto.SqlExecResult;
import com.carlos.monitor.db.pojo.param.SqlHistoryPageParam;
import com.carlos.monitor.db.pojo.vo.SqlHistoryVO;

/**
 * <p>
 * SQL 控制台 业务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
public interface SqlConsoleService {

    /**
     * 只读执行（SELECT/EXPLAIN）
     *
     * @param instanceId 实例ID
     * @param sql        SQL（单语句）
     * @param clientIp   来源 IP
     * @return 执行结果（列 + 行，已脱敏）
     */
    SqlExecResult executeReadonly(Long instanceId, String sql, String clientIp);

    /**
     * 写执行（UPDATE/DELETE，高危：sql_write_enabled + confirm-token + 行数保护）
     *
     * @param instanceId   实例ID
     * @param sql          SQL（单语句）
     * @param confirmToken 高危操作确认令牌
     * @param clientIp     来源 IP
     * @return 影响行数
     */
    int executeWrite(Long instanceId, String sql, String confirmToken, String clientIp);

    /**
     * 构造写执行的 confirm-token 操作摘要（签发与消费必须一致）
     *
     * @param instanceId 实例ID
     * @param sql        SQL
     * @return 操作摘要
     */
    String buildWriteOperation(Long instanceId, String sql);

    /**
     * 执行历史分页
     */
    Paging<SqlHistoryVO> getHistoryPage(SqlHistoryPageParam param);
}
