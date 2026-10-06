package com.carlos.monitor.servicecollector.nacos;

import cn.hutool.core.util.StrUtil;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.carlos.monitor.enums.MonitorErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Properties;

/**
 * <p>
 * Nacos 服务发现封装（微服务监控域）
 * 复用本服务自身的 Nacos 连接配置（spring.cloud.nacos.*），惰性初始化
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@Component
public class MonitorNacosDiscovery {

    private final Environment environment;

    private volatile NamingService namingService;

    public MonitorNacosDiscovery(Environment environment) {
        this.environment = environment;
    }

    /**
     * 全部服务名
     *
     * @return 服务名列表
     */
    public List<String> listServices() {
        try {
            ListView<String> services = namingService().getServicesOfServer(1, 500);
            return services.getData();
        } catch (NacosException e) {
            throw MonitorErrorCode.MONITOR_NACOS_DISCOVERY_FAILED
                .exception("拉取 Nacos 服务列表失败：" + e.getErrMsg());
        }
    }

    /**
     * 服务的全部实例（含不健康）
     *
     * @param serviceName 服务名
     * @return 实例列表
     */
    public List<Instance> getAllInstances(String serviceName) {
        try {
            return namingService().getAllInstances(serviceName);
        } catch (NacosException e) {
            log.warn("拉取 Nacos 实例列表失败, service:{}, error:{}", serviceName, e.getErrMsg());
            return List.of();
        }
    }

    /**
     * 惰性构建 NamingService
     */
    private NamingService namingService() {
        if (namingService == null) {
            synchronized (this) {
                if (namingService == null) {
                    try {
                        Properties properties = new Properties();
                        properties.setProperty("serverAddr",
                            environment.getProperty("spring.cloud.nacos.discovery.server-addr",
                                environment.getProperty("spring.cloud.nacos.config.server-addr", "localhost:8848")));
                        putIfNotBlank(properties, "namespace",
                            environment.getProperty("spring.cloud.nacos.discovery.namespace",
                                environment.getProperty("spring.cloud.nacos.config.namespace")));
                        putIfNotBlank(properties, "username",
                            environment.getProperty("spring.cloud.nacos.username"));
                        putIfNotBlank(properties, "password",
                            environment.getProperty("spring.cloud.nacos.password"));
                        namingService = NacosFactory.createNamingService(properties);
                    } catch (NacosException e) {
                        throw MonitorErrorCode.MONITOR_NACOS_DISCOVERY_FAILED
                            .exception("Nacos 命名服务初始化失败：" + e.getErrMsg());
                    }
                }
            }
        }
        return namingService;
    }

    private void putIfNotBlank(Properties properties, String key, String value) {
        if (StrUtil.isNotBlank(value)) {
            properties.setProperty(key, value);
        }
    }
}
