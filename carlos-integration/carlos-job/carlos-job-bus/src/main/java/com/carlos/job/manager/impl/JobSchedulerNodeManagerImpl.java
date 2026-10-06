package com.carlos.job.manager.impl;

import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.job.mapper.QrtzSchedulerStateMapper;
import com.carlos.job.manager.JobSchedulerNodeManager;
import com.carlos.job.pojo.entity.QrtzSchedulerState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>
 * 调度器集群节点 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Slf4j
@Component
public class JobSchedulerNodeManagerImpl extends BaseServiceImpl<QrtzSchedulerStateMapper, QrtzSchedulerState>
    implements JobSchedulerNodeManager {

    @Override
    public List<QrtzSchedulerState> listNodes() {
        return list();
    }

}
