package com.carlos.monitor.redis.service;

import com.carlos.core.pagination.Paging;
import com.carlos.monitor.collect.sink.MonitorCkQueryClient;
import com.carlos.monitor.redis.pojo.param.BigkeyReportPageParam;
import com.carlos.monitor.redis.pojo.vo.BigkeyReportItemVO;
import com.carlos.monitor.redis.pojo.vo.BigkeyReportVO;

/**
 * <p>
 * 大key/热key 报告 业务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
public interface BigkeyReportService {

    /**
     * 手动触发大 key/热 key 扫描（异步执行）
     *
     * @param instanceId 实例ID
     * @param reportType 报告类型：BIGKEY/HOTKEY
     * @return 报告ID
     */
    Long triggerScan(Long instanceId, String reportType);

    /**
     * 报告分页
     */
    Paging<BigkeyReportVO> getPage(BigkeyReportPageParam param);

    /**
     * 报告明细分页（按 memory_bytes 倒序）
     *
     * @param reportId 报告ID
     */
    Paging<BigkeyReportItemVO> getItems(Long reportId, int current, int size);
}
