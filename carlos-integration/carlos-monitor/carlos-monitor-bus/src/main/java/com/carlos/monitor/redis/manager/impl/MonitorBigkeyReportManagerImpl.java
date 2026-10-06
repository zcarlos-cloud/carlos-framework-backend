package com.carlos.monitor.redis.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.monitor.redis.convert.BigkeyReportConvert;
import com.carlos.monitor.redis.manager.MonitorBigkeyReportManager;
import com.carlos.monitor.redis.mapper.MonitorBigkeyReportMapper;
import com.carlos.monitor.redis.pojo.entity.MonitorBigkeyReport;
import com.carlos.monitor.redis.pojo.param.BigkeyReportPageParam;
import com.carlos.monitor.redis.pojo.vo.BigkeyReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * Redis 大key/热key 报告 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MonitorBigkeyReportManagerImpl extends BaseServiceImpl<MonitorBigkeyReportMapper, MonitorBigkeyReport>
    implements MonitorBigkeyReportManager {

    private static final String STATUS_SUCCESS = "SUCCESS";

    private static final String STATUS_FAILED = "FAILED";

    @Override
    public boolean createReport(MonitorBigkeyReport report) {
        boolean success = save(report);
        if (!success) {
            log.warn("Insert 'MonitorBigkeyReport' data fail, instanceId:{}", report.getInstanceId());
        }
        return success;
    }

    @Override
    public Paging<BigkeyReportVO> getPage(BigkeyReportPageParam param) {
        LambdaQueryWrapper<MonitorBigkeyReport> wrapper = queryWrapper();
        wrapper.eq(MonitorBigkeyReport::getInstanceId, param.getInstanceId());
        wrapper.eq(StrUtil.isNotBlank(param.getReportType()), MonitorBigkeyReport::getReportType, param.getReportType());
        wrapper.eq(StrUtil.isNotBlank(param.getScanStatus()), MonitorBigkeyReport::getScanStatus, param.getScanStatus());
        wrapper.ge(param.getStart() != null, MonitorBigkeyReport::getCreateTime, param.getStart());
        wrapper.le(param.getEnd() != null, MonitorBigkeyReport::getCreateTime, param.getEnd());
        wrapper.orderByDesc(MonitorBigkeyReport::getCreateTime);

        PageInfo<MonitorBigkeyReport> page = page(pageInfo(param), wrapper);
        return MybatisPage.map(page, BigkeyReportConvert.INSTANCE::toVO);
    }

    @Override
    public MonitorBigkeyReport getReportById(Serializable id) {
        if (id == null) {
            return null;
        }
        return getBaseMapper().selectById(id);
    }

    @Override
    public void markSuccess(Serializable id, long scannedKeys) {
        LambdaUpdateWrapper<MonitorBigkeyReport> wrapper = updateWrapper();
        wrapper.eq(MonitorBigkeyReport::getId, id);
        wrapper.set(MonitorBigkeyReport::getScanStatus, STATUS_SUCCESS);
        wrapper.set(MonitorBigkeyReport::getScannedKeys, scannedKeys);
        wrapper.set(MonitorBigkeyReport::getEndTime, LocalDateTime.now());
        update(wrapper);
    }

    @Override
    public void markFailed(Serializable id, String errorMsg) {
        LambdaUpdateWrapper<MonitorBigkeyReport> wrapper = updateWrapper();
        wrapper.eq(MonitorBigkeyReport::getId, id);
        wrapper.set(MonitorBigkeyReport::getScanStatus, STATUS_FAILED);
        wrapper.set(MonitorBigkeyReport::getErrorMsg, StrUtil.maxLength(errorMsg, 1000));
        wrapper.set(MonitorBigkeyReport::getEndTime, LocalDateTime.now());
        update(wrapper);
    }
}
