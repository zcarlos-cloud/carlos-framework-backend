package com.carlos.monitor.redis.manager.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamPage;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.monitor.redis.convert.BigkeyReportConvert;
import com.carlos.monitor.redis.manager.MonitorBigkeyReportItemManager;
import com.carlos.monitor.redis.mapper.MonitorBigkeyReportItemMapper;
import com.carlos.monitor.redis.pojo.entity.MonitorBigkeyReportItem;
import com.carlos.monitor.redis.pojo.vo.BigkeyReportItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>
 * Redis 大key/热key 报告明细 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MonitorBigkeyReportItemManagerImpl
    extends BaseServiceImpl<MonitorBigkeyReportItemMapper, MonitorBigkeyReportItem>
    implements MonitorBigkeyReportItemManager {

    @Override
    public void saveItems(List<MonitorBigkeyReportItem> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        saveBatch(items);
        log.debug("Insert 'MonitorBigkeyReportItem' data, reportId:{}, count:{}",
            items.getFirst().getReportId(), items.size());
    }

    @Override
    public Paging<BigkeyReportItemVO> getPageByReport(Long reportId, int current, int size) {
        LambdaQueryWrapper<MonitorBigkeyReportItem> wrapper = queryWrapper();
        wrapper.eq(MonitorBigkeyReportItem::getReportId, reportId);
        wrapper.orderByDesc(MonitorBigkeyReportItem::getMemoryBytes);

        ParamPage paramPage = new ParamPage();
        paramPage.setCurrent(current);
        paramPage.setSize(size);
        PageInfo<MonitorBigkeyReportItem> page = page(pageInfo(paramPage), wrapper);
        return MybatisPage.map(page, BigkeyReportConvert.INSTANCE::toVO);
    }
}
