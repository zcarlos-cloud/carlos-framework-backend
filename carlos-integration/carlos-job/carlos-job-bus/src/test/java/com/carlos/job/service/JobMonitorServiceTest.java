package com.carlos.job.service;

import com.carlos.job.manager.JobSchedulerNodeManager;
import com.carlos.job.pojo.entity.QrtzSchedulerState;
import com.carlos.job.pojo.vo.SchedulerNodeVO;
import com.carlos.job.pojo.vo.ServiceInstanceVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Scheduler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 调度器运维业务单元测试
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@ExtendWith(MockitoExtension.class)
class JobMonitorServiceTest {

    @Mock
    private Scheduler scheduler;

    @Mock
    private JobSchedulerNodeManager schedulerNodeManager;

    @Mock
    private ObjectProvider<DiscoveryClient> discoveryClientProvider;

    private JobMonitorService monitorService;

    @BeforeEach
    void setUp() {
        monitorService = new JobMonitorService(scheduler, schedulerNodeManager, discoveryClientProvider);
    }

    @Test
    @DisplayName("应该按心跳判定节点存活状态")
    void should_mark_node_alive_by_checkin() {
        long now = System.currentTimeMillis();
        QrtzSchedulerState alive = new QrtzSchedulerState()
            .setSchedName("carlosJobScheduler").setInstanceName("node1")
            .setLastCheckinTime(now - 5000).setCheckinInterval(10000L);
        QrtzSchedulerState dead = new QrtzSchedulerState()
            .setSchedName("carlosJobScheduler").setInstanceName("node2")
            .setLastCheckinTime(now - 100000).setCheckinInterval(10000L);
        when(schedulerNodeManager.listNodes()).thenReturn(List.of(alive, dead));

        List<SchedulerNodeVO> nodes = monitorService.listNodes();

        assertEquals(2, nodes.size());
        assertTrue(nodes.get(0).getAlive());
        assertFalse(nodes.get(1).getAlive());
    }

    @Test
    @DisplayName("应该返回空实例列表当服务发现不可用时")
    void should_return_empty_when_discovery_unavailable() {
        when(discoveryClientProvider.getIfAvailable()).thenReturn(null);

        List<ServiceInstanceVO> result = monitorService.listServiceInstances("carlos-message");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("应该返回服务实例列表当服务发现可用时")
    void should_return_instances_when_discovery_available() {
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        ServiceInstance instance = mock(ServiceInstance.class);
        lenient().when(instance.getInstanceId()).thenReturn("inst-1");
        lenient().when(instance.getHost()).thenReturn("192.168.3.40");
        lenient().when(instance.getPort()).thenReturn(9511);
        when(discoveryClientProvider.getIfAvailable()).thenReturn(discoveryClient);
        when(discoveryClient.getInstances("carlos-message")).thenReturn(List.of(instance));

        List<ServiceInstanceVO> result = monitorService.listServiceInstances("carlos-message");

        assertEquals(1, result.size());
        assertEquals("192.168.3.40:9511", result.get(0).getAddress());
    }

}
