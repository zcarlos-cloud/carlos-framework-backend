package com.carlos.monitor.redis.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.monitor.redis.pojo.entity.MonitorBigkeyReportItem;
import com.carlos.monitor.redis.pojo.vo.BigkeyReportItemVO;

import java.util.List;

/**
 * <p>
 * Redis 大key/热key 报告明细 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
public interface MonitorBigkeyReportItemManager extends BaseService<MonitorBigkeyReportItem> {

    /**
     * 批量写入报告明细
     *
     * @param items 明细列表
     */
    void saveItems(List<MonitorBigkeyReportItem> items);

    /**
     * 报告明细分页（按 memory_bytes 倒序）
     *
     * @param reportId 报告ID
     * @param current  页码
     * @param size     页大小
     * @return 分页结果
     */
    Paging<BigkeyReportItemVO> getPageByReport(Long reportId, int current, int size);
}
