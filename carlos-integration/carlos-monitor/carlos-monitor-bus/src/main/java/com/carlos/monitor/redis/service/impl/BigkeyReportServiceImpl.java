package com.carlos.monitor.redis.service.impl;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.monitor.collect.redis.BigKeyScanTask;
import com.carlos.monitor.enums.MonitorErrorCode;
import com.carlos.monitor.redis.manager.MonitorBigkeyReportItemManager;
import com.carlos.monitor.redis.manager.MonitorBigkeyReportManager;
import com.carlos.monitor.redis.pojo.param.BigkeyReportPageParam;
import com.carlos.monitor.redis.pojo.vo.BigkeyReportItemVO;
import com.carlos.monitor.redis.pojo.vo.BigkeyReportVO;
import com.carlos.monitor.redis.service.BigkeyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 大key/热key 报告 业务接口实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BigkeyReportServiceImpl implements BigkeyReportService {

    private final BigKeyScanTask bigKeyScanTask;

    private final MonitorBigkeyReportManager reportManager;

    private final MonitorBigkeyReportItemManager reportItemManager;

    @Override
    public Long triggerScan(Long instanceId, String reportType) {
        String type = StrUtil.nullToDefault(reportType, BigKeyScanTask.TYPE_BIGKEY).toUpperCase();
        if (!BigKeyScanTask.TYPE_BIGKEY.equals(type) && !BigKeyScanTask.TYPE_HOTKEY.equals(type)) {
            throw MonitorErrorCode.MONITOR_INSTANCE_TYPE_UNSUPPORTED
                .exception("不支持的报告类型：" + reportType);
        }
        return bigKeyScanTask.triggerScan(instanceId, type, BigKeyScanTask.TRIGGER_MANUAL);
    }

    @Override
    public Paging<BigkeyReportVO> getPage(BigkeyReportPageParam param) {
        return reportManager.getPage(param);
    }

    @Override
    public Paging<BigkeyReportItemVO> getItems(Long reportId, int current, int size) {
        if (reportManager.getReportById(reportId) == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception("报告不存在：" + reportId);
        }
        return reportItemManager.getPageByReport(reportId, current, size);
    }
}
