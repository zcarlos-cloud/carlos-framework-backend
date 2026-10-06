package com.carlos.monitor.db.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.monitor.db.manager.MonitorSqlHistoryManager;
import com.carlos.monitor.db.mapper.MonitorSqlHistoryMapper;
import com.carlos.monitor.db.pojo.entity.MonitorSqlHistory;
import com.carlos.monitor.db.pojo.param.SqlHistoryPageParam;
import com.carlos.monitor.db.pojo.vo.SqlHistoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <p>
 * SQL 控制台执行历史 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MonitorSqlHistoryManagerImpl extends BaseServiceImpl<MonitorSqlHistoryMapper, MonitorSqlHistory>
    implements MonitorSqlHistoryManager {

    @Override
    public void addHistory(MonitorSqlHistory history) {
        boolean success = save(history);
        if (!success) {
            log.warn("Insert 'MonitorSqlHistory' data fail, instanceId:{}", history.getInstanceId());
        }
    }

    @Override
    public Paging<SqlHistoryVO> getPage(SqlHistoryPageParam param) {
        LambdaQueryWrapper<MonitorSqlHistory> wrapper = queryWrapper();
        wrapper.eq(MonitorSqlHistory::getInstanceId, param.getInstanceId());
        wrapper.eq(StrUtil.isNotBlank(param.getSqlType()), MonitorSqlHistory::getSqlType, param.getSqlType());
        wrapper.eq(StrUtil.isNotBlank(param.getExecStatus()), MonitorSqlHistory::getExecStatus, param.getExecStatus());
        wrapper.eq(param.getExecuteBy() != null, MonitorSqlHistory::getExecuteBy, param.getExecuteBy());
        wrapper.ge(param.getStart() != null, MonitorSqlHistory::getExecuteTime, param.getStart());
        wrapper.le(param.getEnd() != null, MonitorSqlHistory::getExecuteTime, param.getEnd());
        wrapper.orderByDesc(MonitorSqlHistory::getExecuteTime);

        PageInfo<MonitorSqlHistory> page = page(pageInfo(param), wrapper);
        return MybatisPage.map(page, this::toVO);
    }

    private SqlHistoryVO toVO(MonitorSqlHistory entity) {
        SqlHistoryVO vo = new SqlHistoryVO();
        vo.setId(entity.getId());
        vo.setInstanceId(entity.getInstanceId());
        vo.setExecuteBy(entity.getExecuteBy());
        vo.setSqlText(entity.getSqlText());
        vo.setSqlType(entity.getSqlType());
        vo.setExecStatus(entity.getExecStatus());
        vo.setBlockReason(entity.getBlockReason());
        vo.setAffectedRows(entity.getAffectedRows());
        vo.setResultRows(entity.getResultRows());
        vo.setDurationMs(entity.getDurationMs());
        vo.setClientIp(entity.getClientIp());
        vo.setExecuteTime(entity.getExecuteTime());
        return vo;
    }
}
