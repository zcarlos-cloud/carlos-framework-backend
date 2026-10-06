package com.carlos.monitor.db.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.monitor.db.pojo.entity.MonitorDbCapacitySnapshot;
import com.carlos.monitor.db.pojo.vo.CapacitySnapshotVO;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 数据库表容量快照 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
public interface MonitorDbCapacitySnapshotManager extends BaseService<MonitorDbCapacitySnapshot> {

    /**
     * 幂等写入快照：同 (instance,schema,table,date) 已存在则更新，否则插入
     *
     * @param snapshot 快照实体
     */
    void upsertSnapshot(MonitorDbCapacitySnapshot snapshot);

    /**
     * 查询实例某日快照 Top N（按 table_size_bytes 倒序）
     *
     * @param instanceId   实例ID
     * @param snapshotDate 快照日期（null 取最新日期）
     * @param topN         条数
     * @return 快照列表（含快照日期）
     */
    List<CapacitySnapshotVO> listTopCapacity(Long instanceId, LocalDate snapshotDate, int topN);
}
