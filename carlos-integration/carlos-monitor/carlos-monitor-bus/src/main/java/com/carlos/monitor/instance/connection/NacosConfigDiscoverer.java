package com.carlos.monitor.instance.connection;

import cn.hutool.core.util.StrUtil;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.enums.MonitorErrorCode;
import com.carlos.monitor.instance.pojo.dto.InstanceDraftDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * <p>
 * Nacos 配置自动发现辅助
 * 读取目标应用的 Nacos 配置（应用自身 {appName}.yml 与共享 common/redis/db.yml），
 * 解析 spring.data.redis 与 spring.datasource.dynamic.datasource 生成实例草稿（不含密码）。
 * 解析失败单项降级跳过，不阻断其他草稿生成
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Slf4j
@Component
public class NacosConfigDiscoverer {

    /**
     * 拉取配置超时（毫秒）
     */
    private static final long CONFIG_TIMEOUT_MS = 5000L;

    private final Environment environment;

    private volatile ConfigService configService;

    public NacosConfigDiscoverer(Environment environment) {
        this.environment = environment;
    }

    /**
     * 自动发现目标应用的中间件/数据库连接配置，生成实例草稿
     *
     * @param appName 目标应用名（spring.application.name）
     * @param group   Nacos 配置分组
     * @return 实例草稿列表（不含密码）
     */
    public List<InstanceDraftDTO> discover(String appName, String group) {
        if (StrUtil.isBlank(appName)) {
            throw MonitorErrorCode.MONITOR_NACOS_DISCOVERY_FAILED.exception("应用名不能为空");
        }
        String effectiveGroup = StrUtil.isBlank(group) ? "DEFAULT_GROUP" : group;
        List<InstanceDraftDTO> drafts = new ArrayList<>();
        // 应用自身配置 + 共享配置，前者优先（同名配置以应用自身为准）
        List<String> dataIds = List.of(appName + ".yml", "redis.yml", "db.yml", "common.yml");
        for (String dataId : dataIds) {
            String content = getConfig(dataId, effectiveGroup);
            if (StrUtil.isBlank(content)) {
                continue;
            }
            try {
                Map<String, Object> yaml = new Yaml().load(content);
                drafts.addAll(parseRedisDrafts(appName, dataId, yaml));
                drafts.addAll(parseDatasourceDrafts(appName, dataId, yaml));
            } catch (Exception e) {
                // 单个 dataId 解析失败降级跳过，不影响其他配置
                log.warn("Parse nacos config fail, dataId:{}, error:{}", dataId, e.getMessage());
            }
        }
        return drafts;
    }

    /**
     * 解析 spring.data.redis 配置
     */
    @SuppressWarnings("unchecked")
    private List<InstanceDraftDTO> parseRedisDrafts(String appName, String dataId, Map<String, Object> yaml) {
        Map<String, Object> redis = path(yaml, "spring", "data", "redis");
        if (redis == null) {
            return List.of();
        }
        List<InstanceDraftDTO> drafts = new ArrayList<>();
        Map<String, Object> cluster = (Map<String, Object>) redis.get("cluster");
        Map<String, Object> sentinel = (Map<String, Object>) redis.get("sentinel");
        if (cluster != null && cluster.get("nodes") != null) {
            InstanceDraftDTO draft = new InstanceDraftDTO();
            draft.setInstanceName(appName + "-redis-cluster");
            draft.setInstanceType(InstanceTypeEnum.REDIS_CLUSTER);
            draft.setAddresses(joinNodes(cluster.get("nodes")));
            draft.setSourceConfig(dataId + "#spring.data.redis.cluster");
            drafts.add(draft);
        } else if (sentinel != null && sentinel.get("nodes") != null) {
            InstanceDraftDTO draft = new InstanceDraftDTO();
            draft.setInstanceName(appName + "-redis-sentinel");
            draft.setInstanceType(InstanceTypeEnum.REDIS_SENTINEL);
            draft.setAddresses(joinNodes(sentinel.get("nodes")));
            draft.setSourceConfig(dataId + "#spring.data.redis.sentinel");
            drafts.add(draft);
        } else if (redis.get("host") != null) {
            InstanceDraftDTO draft = new InstanceDraftDTO();
            draft.setInstanceName(appName + "-redis");
            draft.setInstanceType(InstanceTypeEnum.REDIS_STANDALONE);
            String host = String.valueOf(redis.get("host"));
            String port = redis.get("port") == null ? "6379" : String.valueOf(redis.get("port"));
            draft.setAddresses(host + ":" + port);
            if (redis.get("database") != null) {
                try {
                    draft.setDbIndex(Integer.parseInt(String.valueOf(redis.get("database"))));
                } catch (NumberFormatException e) {
                    log.warn("Redis database index parse fail, dataId:{}", dataId);
                }
            }
            if (redis.get("username") != null) {
                draft.setUsername(String.valueOf(redis.get("username")));
            }
            draft.setSourceConfig(dataId + "#spring.data.redis");
            drafts.add(draft);
        }
        return drafts;
    }

    /**
     * 解析 spring.datasource.dynamic.datasource 配置
     */
    @SuppressWarnings("unchecked")
    private List<InstanceDraftDTO> parseDatasourceDrafts(String appName, String dataId, Map<String, Object> yaml) {
        Map<String, Object> datasources = path(yaml, "spring", "datasource", "dynamic", "datasource");
        if (datasources == null) {
            return List.of();
        }
        List<InstanceDraftDTO> drafts = new ArrayList<>();
        for (Map.Entry<String, Object> entry : datasources.entrySet()) {
            if (!(entry.getValue() instanceof Map)) {
                continue;
            }
            Map<String, Object> ds = (Map<String, Object>) entry.getValue();
            Object url = ds.get("url");
            if (url == null) {
                continue;
            }
            InstanceDraftDTO draft = parseJdbcUrl(appName, entry.getKey(), String.valueOf(url));
            if (draft == null) {
                continue;
            }
            if (ds.get("username") != null) {
                draft.setUsername(String.valueOf(ds.get("username")));
            }
            draft.setSourceConfig(dataId + "#spring.datasource.dynamic.datasource." + entry.getKey());
            drafts.add(draft);
        }
        return drafts;
    }

    /**
     * 解析 JDBC URL 生成 DB 实例草稿（支持 postgresql/mysql/dm/clickhouse）
     */
    private InstanceDraftDTO parseJdbcUrl(String appName, String dsName, String url) {
        InstanceTypeEnum type;
        String prefix;
        if (url.startsWith("jdbc:postgresql:")) {
            type = InstanceTypeEnum.POSTGRESQL;
            prefix = "jdbc:postgresql://";
        } else if (url.startsWith("jdbc:mysql:")) {
            type = InstanceTypeEnum.MYSQL;
            prefix = "jdbc:mysql://";
        } else if (url.startsWith("jdbc:dm:")) {
            type = InstanceTypeEnum.DM;
            prefix = "jdbc:dm://";
        } else if (url.startsWith("jdbc:clickhouse:")) {
            type = InstanceTypeEnum.CLICKHOUSE;
            prefix = "jdbc:clickhouse://";
        } else {
            log.debug("Unsupported jdbc url, skip: {}", url);
            return null;
        }
        String remainder = url.substring(prefix.length());
        int queryIdx = remainder.indexOf('?');
        if (queryIdx >= 0) {
            remainder = remainder.substring(0, queryIdx);
        }
        String address = remainder;
        String dbName = null;
        int slashIdx = remainder.indexOf('/');
        if (slashIdx >= 0) {
            address = remainder.substring(0, slashIdx);
            dbName = remainder.substring(slashIdx + 1);
        }
        if (StrUtil.isBlank(address)) {
            return null;
        }
        InstanceDraftDTO draft = new InstanceDraftDTO();
        draft.setInstanceName(appName + "-" + dsName);
        draft.setInstanceType(type);
        draft.setAddresses(address);
        draft.setDbName(StrUtil.blankToDefault(dbName, null));
        return draft;
    }

    /**
     * 多级路径提取 Map（任一节点缺失返回 null）
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> path(Map<String, Object> yaml, String... keys) {
        Map<String, Object> current = yaml;
        for (String key : keys) {
            if (current == null) {
                return null;
            }
            Object next = current.get(key);
            if (!(next instanceof Map)) {
                return null;
            }
            current = (Map<String, Object>) next;
        }
        return current;
    }

    private String joinNodes(Object nodes) {
        if (nodes instanceof List<?> list) {
            return StrUtil.join(",", list);
        }
        return String.valueOf(nodes);
    }

    private String getConfig(String dataId, String group) {
        try {
            return configService().getConfig(dataId, group, CONFIG_TIMEOUT_MS);
        } catch (NacosException e) {
            log.warn("Get nacos config fail, dataId:{}, group:{}, error:{}", dataId, group, e.getErrMsg());
            return null;
        }
    }

    /**
     * 惰性构建 Nacos ConfigService（复用本服务自身的 Nacos 连接配置）
     */
    private ConfigService configService() {
        if (configService == null) {
            synchronized (this) {
                if (configService == null) {
                    try {
                        Properties properties = new Properties();
                        properties.setProperty("serverAddr",
                            environment.getProperty("spring.cloud.nacos.config.server-addr", "localhost:8848"));
                        putIfNotBlank(properties, "namespace",
                            environment.getProperty("spring.cloud.nacos.config.namespace"));
                        putIfNotBlank(properties, "username",
                            environment.getProperty("spring.cloud.nacos.username"));
                        putIfNotBlank(properties, "password",
                            environment.getProperty("spring.cloud.nacos.password"));
                        configService = NacosFactory.createConfigService(properties);
                    } catch (NacosException e) {
                        throw MonitorErrorCode.MONITOR_NACOS_DISCOVERY_FAILED
                            .exception("Nacos 配置服务初始化失败：" + e.getErrMsg());
                    }
                }
            }
        }
        return configService;
    }

    private void putIfNotBlank(Properties properties, String key, String value) {
        if (StrUtil.isNotBlank(value)) {
            properties.setProperty(key, value);
        }
    }
}
