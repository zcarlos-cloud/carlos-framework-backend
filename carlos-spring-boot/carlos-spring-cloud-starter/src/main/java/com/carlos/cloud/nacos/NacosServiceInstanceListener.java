package com.carlos.cloud.nacos;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationListener;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * <p>
 * Nacos 服务实例变更监听器
 * </p>
 *
 * <p>
 * 监听服务实例的上线、下线、变更等事件
 * </p>
 *
 * @author carlos
 * @date 2024/01/15
 */
@Slf4j
@RequiredArgsConstructor
public class NacosServiceInstanceListener implements ApplicationListener<HeartbeatEvent> {

    private final NacosServiceManager nacosServiceManager;

    private final NacosDiscoveryProperties nacosDiscoveryProperties;

    private final NacosCloudProperties nacosCloudProperties;

    /**
     * 服务实例缓存
     */
    private final ConcurrentHashMap<String, Set<String>> serviceInstanceCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Nacos 服务实例变更监听器已启动");
    }

    @Override
    public void onApplicationEvent(HeartbeatEvent event) {
        if (!nacosCloudProperties.getSubscription().isLogChange()) {
            return;
        }

        try {
            // 获取 NamingService
            NamingService namingService = nacosServiceManager.getNamingService(
                nacosDiscoveryProperties.getNacosProperties());
            // 获取所有服务
            List<String> services = namingService.getServicesOfServer(1, Integer.MAX_VALUE).getData();
            for (String service : services) {
                List<Instance> instances = namingService.selectInstances(service, true);
                Set<String> currentInstances = instances.stream()
                    .map(i -> i.getIp() + ":" + i.getPort())
                    .collect(Collectors.toSet());

                Set<String> cachedInstances = serviceInstanceCache.get(service);
                if (cachedInstances == null) {
                    // 首次发现服务
                    serviceInstanceCache.put(service, currentInstances);
                    log.debug("发现新服务: {}, 实例数: {}", service, currentInstances.size());
                } else if (!cachedInstances.equals(currentInstances)) {
                    // 服务实例发生变化
                    Set<String> added = currentInstances.stream()
                        .filter(i -> !cachedInstances.contains(i))
                        .collect(Collectors.toSet());
                    Set<String> removed = cachedInstances.stream()
                        .filter(i -> !currentInstances.contains(i))
                        .collect(Collectors.toSet());

                    serviceInstanceCache.put(service, currentInstances);

                    if (!added.isEmpty()) {
                        log.info("服务 [{}] 新增实例: {}", service, added);
                    }
                    if (!removed.isEmpty()) {
                        log.info("服务 [{}] 移除实例: {}", service, removed);
                    }
                }
            }
        } catch (Exception e) {
            log.error("监听服务实例变更时发生错误", e);
        }
    }

    /**
     * 处理 Nacos 命名事件
     */
    public void onNamingEvent(NamingEvent event) {
        String serviceName = event.getServiceName();
        List<Instance> instances = event.getInstances();

        log.debug("收到 Nacos 命名事件 - 服务: {}, 当前实例数: {}",
            serviceName, instances.size());
    }

    /**
     * 获取服务缓存
     */
    public ConcurrentHashMap<String, Set<String>> getServiceInstanceCache() {
        return serviceInstanceCache;
    }
}
