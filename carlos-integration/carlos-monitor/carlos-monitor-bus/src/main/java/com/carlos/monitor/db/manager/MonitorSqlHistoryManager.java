package com.carlos.monitor.db.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.monitor.db.pojo.entity.MonitorSqlHistory;
import com.carlos.monitor.db.pojo.param.SqlHistoryPageParam;
import com.carlos.monitor.db.pojo.vo.SqlHistoryVO;

/**
 * <p>
 * SQL 控制台执行历史 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
public interface MonitorSqlHistoryManager extends BaseService<MonitorSqlHistory> {

    /**
     * 写入执行历史（脱敏后留档）
     *
     * @param history 历史实体
     */
    void addHistory(MonitorSqlHistory history);

    /**
     * 执行历史分页
     *
     * @param param 分页参数
     * @return 分页结果
     */
    Paging<SqlHistoryVO> getPage(SqlHistoryPageParam param);
}
