package com.carlos.job.manager;

import com.carlos.datasource.base.BaseService;
import com.carlos.job.pojo.entity.QrtzSchedulerState;

import java.util.List;

/**
 * <p>
 * 调度器集群节点 查询封装接口（只读 QRTZ_SCHEDULER_STATE）
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
public interface JobSchedulerNodeManager extends BaseService<QrtzSchedulerState> {

    /**
     * 查询全部集群节点
     *
     * @return 节点列表
     */
    List<QrtzSchedulerState> listNodes();

}
