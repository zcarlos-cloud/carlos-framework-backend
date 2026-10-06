package com.carlos.job.service;

import com.carlos.job.enums.JobErrorCode;
import com.carlos.job.manager.JobSchedulerNodeManager;
import com.carlos.job.pojo.entity.QrtzSchedulerState;
import com.carlos.job.pojo.vo.SchedulerMetaVO;
import com.carlos.job.pojo.vo.SchedulerNodeVO;
import com.carlos.job.pojo.vo.ServiceInstanceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 调度器运维 业务（集群节点视图、调度器元数据、standby/start、目标服务实例）
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobMonitorService {

    private final Scheduler scheduler;

    private final JobSchedulerNodeManager schedulerNodeManager;

    private final ObjectProvider<DiscoveryClient> discoveryClientProvider;

    /**
     * 调度器元数据（本实例）
     *
     * @return 元数据视图
     */
    public SchedulerMetaVO getSchedulerMeta() {
        try {
            SchedulerMetaData meta = scheduler.getMetaData();
            return new SchedulerMetaVO()
                .setSchedulerName(meta.getSchedulerName())
                .setInstanceId(meta.getSchedulerInstanceId())
                .setClustered(meta.isJobStoreClustered())
                .setStandby(scheduler.isInStandbyMode())
                .setShutdown(scheduler.isShutdown())
                .setRunningSince(toLocalDateTime(meta.getRunningSince()))
                .setThreadPoolSize(meta.getThreadPoolSize())
                .setExecutedJobCount(meta.getNumberOfJobsExecuted())
                .setRunningJobCount(scheduler.getCurrentlyExecutingJobs().size());
        } catch (SchedulerException e) {
            throw JobErrorCode.JOB_SCHEDULER_ERROR.exception("查询调度器元数据失败: " + e.getMessage());
        }
    }

    /**
     * 集群节点列表（读 QRTZ_SCHEDULER_STATE，按心跳判断存活）
     *
     * @return 节点列表
     */
    public List<SchedulerNodeVO> listNodes() {
        List<QrtzSchedulerState> states = schedulerNodeManager.listNodes();
        long now = System.currentTimeMillis();
        List<SchedulerNodeVO> nodes = new ArrayList<>();
        for (QrtzSchedulerState state : states) {
            long interval = state.getCheckinInterval() == null ? 0L : state.getCheckinInterval();
            long lastCheckin = state.getLastCheckinTime() == null ? 0L : state.getLastCheckinTime();
            // 心跳超过 2 倍间隔视为失联
            boolean alive = interval > 0 && (now - lastCheckin) <= interval * 2;
            nodes.add(new SchedulerNodeVO()
                .setSchedulerName(state.getSchedName())
                .setInstanceId(state.getInstanceName())
                .setLastCheckinTime(lastCheckin)
                .setCheckinInterval(interval)
                .setAlive(alive));
        }
        return nodes;
    }

    /**
     * 目标服务的在线实例列表（Nacos 服务发现）
     *
     * @param serviceName 服务名
     * @return 实例列表
     */
    public List<ServiceInstanceVO> listServiceInstances(String serviceName) {
        DiscoveryClient discoveryClient = discoveryClientProvider.getIfAvailable();
        if (discoveryClient == null) {
            log.warn("服务发现不可用，无法查询实例: {}", serviceName);
            return List.of();
        }
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        List<ServiceInstanceVO> result = new ArrayList<>();
        for (ServiceInstance instance : instances) {
            result.add(new ServiceInstanceVO()
                .setServiceName(serviceName)
                .setInstanceId(instance.getInstanceId())
                .setHost(instance.getHost())
                .setPort(instance.getPort())
                .setHealthy(true)
                .setAddress(instance.getHost() + ":" + instance.getPort()));
        }
        return result;
    }

    /**
     * 可调度目标服务清单（Nacos 中注册的全部服务）
     *
     * @return 服务名列表
     */
    public List<String> listServices() {
        DiscoveryClient discoveryClient = discoveryClientProvider.getIfAvailable();
        if (discoveryClient == null) {
            return List.of();
        }
        return discoveryClient.getServices();
    }

    /**
     * 调度器进入待机（暂停所有触发，不中断在跑任务）
     */
    public void standby() {
        try {
            scheduler.standby();
            log.info("Scheduler standby: {}", scheduler.getSchedulerName());
        } catch (SchedulerException e) {
            throw JobErrorCode.JOB_SCHEDULER_ERROR.exception("调度器待机失败: " + e.getMessage());
        }
    }

    /**
     * 调度器从待机恢复
     */
    public void start() {
        try {
            scheduler.start();
            log.info("Scheduler start: {}", scheduler.getSchedulerName());
        } catch (SchedulerException e) {
            throw JobErrorCode.JOB_SCHEDULER_ERROR.exception("调度器启动失败: " + e.getMessage());
        }
    }

    /**
     * 暂停全部任务触发
     */
    public void pauseAll() {
        try {
            scheduler.pauseAll();
            log.info("Scheduler pauseAll: {}", scheduler.getSchedulerName());
        } catch (SchedulerException e) {
            throw JobErrorCode.JOB_SCHEDULER_ERROR.exception("暂停全部任务失败: " + e.getMessage());
        }
    }

    /**
     * 恢复全部任务触发
     */
    public void resumeAll() {
        try {
            scheduler.resumeAll();
            log.info("Scheduler resumeAll: {}", scheduler.getSchedulerName());
        } catch (SchedulerException e) {
            throw JobErrorCode.JOB_SCHEDULER_ERROR.exception("恢复全部任务失败: " + e.getMessage());
        }
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

}
