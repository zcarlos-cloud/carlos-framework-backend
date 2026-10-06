package com.carlos.monitor.db.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.monitor.db.manager.MonitorDbCapacitySnapshotManager;
import com.carlos.monitor.db.mapper.MonitorDbCapacitySnapshotMapper;
import com.carlos.monitor.db.pojo.entity.MonitorDbCapacitySnapshot;
import com.carlos.monitor.db.pojo.vo.CapacitySnapshotVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 数据库表容量快照 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MonitorDbCapacitySnapshotManagerImpl
    extends BaseServiceImpl<MonitorDbCapacitySnapshotMapper, MonitorDbCapacitySnapshot>
    implements MonitorDbCapacitySnapshotManager {

    @Override
    public void upsertSnapshot(MonitorDbCapacitySnapshot snapshot) {
        LambdaQueryWrapper<MonitorDbCapacitySnapshot> wrapper = queryWrapper();
        wrapper.eq(MonitorDbCapacitySnapshot::getInstanceId, snapshot.getInstanceId());
        wrapper.eq(MonitorDbCapacitySnapshot::getSchemaName, snapshot.getSchemaName());
        wrapper.eq(MonitorDbCapacitySnapshot::getTableName, snapshot.getTableName());
        wrapper.eq(MonitorDbCapacitySnapshot::getSnapshotDate, snapshot.getSnapshotDate());
        MonitorDbCapacitySnapshot existing = getBaseMapper().selectOne(wrapper);
        if (existing != null) {
            // 同日重跑幂等覆盖
            snapshot.setId(existing.getId());
            updateById(snapshot);
        } else {
            save(snapshot);
        }
    }

    @Override
    public List<CapacitySnapshotVO> listTopCapacity(Long instanceId, LocalDate snapshotDate, int topN) {
        LocalDate effectiveDate = snapshotDate;
        if (effectiveDate == null) {
            effectiveDate = latestSnapshotDate(instanceId);
            if (effectiveDate == null) {
                return List.of();
            }
        }
        LambdaQueryWrapper<MonitorDbCapacitySnapshot> wrapper = queryWrapper();
        wrapper.eq(MonitorDbCapacitySnapshot::getInstanceId, instanceId);
        wrapper.eq(MonitorDbCapacitySnapshot::getSnapshotDate, effectiveDate);
        wrapper.orderByDesc(MonitorDbCapacitySnapshot::getTableSizeBytes);
        wrapper.last("LIMIT " + Math.min(Math.max(topN, 1), 100));
        List<MonitorDbCapacitySnapshot> entities = getBaseMapper().selectList(wrapper);
        return entities.stream().map(this::toVO).toList();
    }

    private LocalDate latestSnapshotDate(Long instanceId) {
        LambdaQueryWrapper<MonitorDbCapacitySnapshot> wrapper = queryWrapper();
        wrapper.select(MonitorDbCapacitySnapshot::getSnapshotDate);
        wrapper.eq(MonitorDbCapacitySnapshot::getInstanceId, instanceId);
        wrapper.orderByDesc(MonitorDbCapacitySnapshot::getSnapshotDate);
        wrapper.last("LIMIT 1");
        MonitorDbCapacitySnapshot latest = getBaseMapper().selectOne(wrapper);
        return latest == null ? null : latest.getSnapshotDate();
    }

    private CapacitySnapshotVO toVO(MonitorDbCapacitySnapshot entity) {
        CapacitySnapshotVO vo = new CapacitySnapshotVO();
        vo.setSnapshotDate(entity.getSnapshotDate());
        vo.setSchemaName(entity.getSchemaName());
        vo.setTableName(entity.getTableName());
        vo.setTableRows(entity.getTableRows());
        vo.setTableSizeBytes(entity.getTableSizeBytes());
        vo.setIndexSizeBytes(entity.getIndexSizeBytes());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
