package com.carlos.monitor.db.schedule;

import com.carlos.job.client.annotation.AutoRegisterJob;
import com.carlos.job.client.annotation.ScheduleJob;
import com.carlos.monitor.collect.db.adapter.DbTypeAdapter;
import com.carlos.monitor.db.manager.MonitorDbCapacitySnapshotManager;
import com.carlos.monitor.db.pojo.dto.CapacityRow;
import com.carlos.monitor.db.pojo.entity.MonitorDbCapacitySnapshot;
import com.carlos.monitor.instance.connection.DbManagedConnection;
import com.carlos.monitor.instance.connection.InstanceConnectionRegistry;
import com.carlos.monitor.instance.connection.ManagedConnection;
import com.carlos.monitor.instance.connection.ManagedConnectionFactory;
import com.carlos.monitor.instance.manager.MonitorInstanceManager;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 库表容量每日快照采集任务
 * Pg：pg_relation_size/pg_indexes_size/reltuples；MySQL：information_schema.tables
 * data_length+index_length；同日重跑幂等覆盖（先查 (instance,schema,table,date) 存在则更新）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CapacitySnapshotTask {

    private final MonitorInstanceManager instanceManager;

    private final InstanceConnectionRegistry connectionRegistry;

    private final MonitorDbCapacitySnapshotManager capacitySnapshotManager;

    private final List<DbTypeAdapter> dbTypeAdapters;

    /**
     * 每日 03:20 采集全部 DB 实例容量快照（cron 以 job_info 为唯一真相源）
     */
    @ScheduleJob("monitorCapacitySnapshotHandler")
    @AutoRegisterJob(cron = "0 20 3 * * ?", description = "数据库表容量每日快照采集", author = "carlos")
    public void snapshotAll() {
        LocalDate today = LocalDate.now();
        for (MonitorInstanceDTO instance : instanceManager.listCollectEnabled()) {
            if (!ManagedConnectionFactory.isDbType(instance.getInstanceType())) {
                continue;
            }
            try {
                snapshotInstance(instance, today);
            } catch (Exception e) {
                // 单实例失败不影响其他实例
                log.warn("容量快照采集失败, instanceId:{}, error:{}", instance.getId(), e.getMessage());
            }
        }
    }

    /**
     * 单实例容量快照（手动触发亦可调用）
     */
    public void snapshotInstance(MonitorInstanceDTO instance, LocalDate snapshotDate) {
        ManagedConnection connection = connectionRegistry.getConnection(instance.getId());
        if (!(connection instanceof DbManagedConnection dbConnection)) {
            return;
        }
        DbTypeAdapter adapter = adapterOf(instance);
        if (adapter == null) {
            return;
        }
        try (Connection jdbcConnection = dbConnection.getDataSource().getConnection()) {
            List<CapacityRow> rows = adapter.collectCapacity(instance.getId(), jdbcConnection);
            for (CapacityRow row : rows) {
                MonitorDbCapacitySnapshot snapshot = new MonitorDbCapacitySnapshot();
                snapshot.setInstanceId(instance.getId());
                snapshot.setSchemaName(row.schemaName());
                snapshot.setTableName(row.tableName());
                snapshot.setTableRows(row.tableRows());
                snapshot.setTableSizeBytes(row.tableSizeBytes());
                snapshot.setIndexSizeBytes(row.indexSizeBytes());
                snapshot.setSnapshotDate(snapshotDate);
                capacitySnapshotManager.upsertSnapshot(snapshot);
            }
            log.info("容量快照采集完成, instanceId:{}, date:{}, tables:{}", instance.getId(), snapshotDate, rows.size());
        } catch (Exception e) {
            log.warn("容量快照采集失败, instanceId:{}, error:{}", instance.getId(), e.getMessage());
        }
    }

    private DbTypeAdapter adapterOf(MonitorInstanceDTO instance) {
        for (DbTypeAdapter adapter : dbTypeAdapters) {
            if (adapter.support(instance.getInstanceType())) {
                return adapter;
            }
        }
        return null;
    }
}
