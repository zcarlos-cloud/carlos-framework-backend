package com.carlos.monitor.instance.connection;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.druid.pool.DruidDataSource;
import com.carlos.core.exception.BusinessException;
import com.carlos.monitor.config.MonitorProperties;
import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.enums.MonitorErrorCode;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 受管连接工厂
 * 按实例类型构建 Redis（Lettuce standalone/sentinel/cluster）或 DB（Druid 小池）连接；
 * 同时服务于注册中心（长驻连接）与连接测试（一次性连接）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ManagedConnectionFactory {

    /**
     * 默认端口
     */
    private static final int DEFAULT_REDIS_PORT = 6379;

    private static final int DEFAULT_PG_PORT = 5432;

    private static final int DEFAULT_MYSQL_PORT = 3306;

    private static final int DEFAULT_DM_PORT = 5236;

    private static final int DEFAULT_CK_PORT = 8123;

    /**
     * ext_config JSON 中 sentinel 主节点名 key
     */
    private static final String EXT_MASTER_NAME = "masterName";

    private final MonitorProperties properties;

    /**
     * 构建受管连接（调用方负责 close）
     *
     * @param dto           实例 DTO（含超时等配置）
     * @param plainPassword 明文密码（由 CredentialCipher 在包内解密，可为空）
     * @return 受管连接
     */
    public ManagedConnection create(MonitorInstanceDTO dto, String plainPassword) {
        return create(dto, plainPassword, null);
    }

    /**
     * 构建受管连接（调用方负责 close）
     *
     * @param dto             实例 DTO（含超时等配置）
     * @param plainPassword   明文密码（由 CredentialCipher 在包内解密，可为空）
     * @param dbIndexOverride Redis 逻辑库覆盖（数据管理按需选库；null 用实例注册默认；cluster 忽略）
     * @return 受管连接
     */
    public ManagedConnection create(MonitorInstanceDTO dto, String plainPassword, Integer dbIndexOverride) {
        InstanceTypeEnum type = dto.getInstanceType();
        if (type == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_TYPE_UNSUPPORTED.exception("实例类型不能为空");
        }
        return switch (type) {
            case REDIS_STANDALONE, REDIS_SENTINEL, REDIS_CLUSTER ->
                createRedisConnection(dto, plainPassword, dbIndexOverride);
            case POSTGRESQL, MYSQL, DM, CLICKHOUSE -> createDbConnection(dto, plainPassword);
            case MICROSERVICE -> throw MonitorErrorCode.MONITOR_INSTANCE_TYPE_UNSUPPORTED
                .exception("微服务实例不经过连接注册中心（actuator 直拉）");
        };
    }

    /**
     * 是否 Redis 类型实例
     *
     * @param type 实例类型
     * @return true=Redis 类型
     */
    public static boolean isRedisType(InstanceTypeEnum type) {
        return type == InstanceTypeEnum.REDIS_STANDALONE
            || type == InstanceTypeEnum.REDIS_CLUSTER
            || type == InstanceTypeEnum.REDIS_SENTINEL;
    }

    /**
     * 是否 DB 类型实例
     *
     * @param type 实例类型
     * @return true=DB 类型
     */
    public static boolean isDbType(InstanceTypeEnum type) {
        return type != null && !isRedisType(type);
    }

    private ManagedConnection createRedisConnection(MonitorInstanceDTO dto, String plainPassword,
                                                    Integer dbIndexOverride) {
        Integer dbIndex = dbIndexOverride != null ? dbIndexOverride : dto.getDbIndex();
        List<HostPort> nodes = parseAddresses(dto.getAddresses(), DEFAULT_REDIS_PORT);
        if (nodes.isEmpty()) {
            throw MonitorErrorCode.MONITOR_CONNECTION_UNAVAILABLE.exception("实例连接地址为空");
        }
        int socketTimeout = dto.getSocketTimeout() == null ? 5000 : dto.getSocketTimeout();
        try {
            if (dto.getInstanceType() == InstanceTypeEnum.REDIS_CLUSTER) {
                List<RedisURI> uris = nodes.stream()
                    .map(node -> redisUri(node, dbIndex, plainPassword, socketTimeout, false))
                    .toList();
                RedisClusterClient clusterClient = RedisClusterClient.create(uris);
                StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();
                return new RedisManagedConnection(dto.getId(), clusterClient, connection);
            }

            RedisURI uri;
            if (dto.getInstanceType() == InstanceTypeEnum.REDIS_SENTINEL) {
                String masterName = parseMasterName(dto.getExtConfig());
                if (StrUtil.isBlank(masterName)) {
                    throw MonitorErrorCode.MONITOR_CONNECTION_UNAVAILABLE
                        .exception("哨兵模式缺少 extConfig.masterName 配置");
                }
                RedisURI.Builder builder = RedisURI.Builder
                    .sentinel(nodes.getFirst().host(), nodes.getFirst().port(), masterName);
                for (int i = 1; i < nodes.size(); i++) {
                    builder.withSentinel(nodes.get(i).host(), nodes.get(i).port());
                }
                if (StrUtil.isNotBlank(plainPassword)) {
                    builder.withPassword(plainPassword.toCharArray());
                }
                if (dbIndex != null) {
                    builder.withDatabase(dbIndex);
                }
                builder.withTimeout(Duration.ofMillis(socketTimeout));
                uri = builder.build();
            } else {
                uri = redisUri(nodes.getFirst(), dbIndex, plainPassword, socketTimeout, true);
            }
            RedisClient client = RedisClient.create(uri);
            StatefulRedisConnection<String, String> connection = client.connect();
            return new RedisManagedConnection(dto.getId(), client, connection);
        } catch (Exception e) {
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw MonitorErrorCode.MONITOR_CONNECTION_UNAVAILABLE
                .exception("Redis 连接创建失败：" + StrUtil.nullToEmpty(e.getMessage()));
        }
    }

    private RedisURI redisUri(HostPort node, Integer dbIndex, String plainPassword, int socketTimeout, boolean useDbIndex) {
        RedisURI.Builder builder = RedisURI.Builder.redis(node.host(), node.port());
        if (StrUtil.isNotBlank(plainPassword)) {
            builder.withPassword(plainPassword.toCharArray());
        }
        if (useDbIndex && dbIndex != null) {
            builder.withDatabase(dbIndex);
        }
        builder.withTimeout(Duration.ofMillis(socketTimeout));
        return builder.build();
    }

    private ManagedConnection createDbConnection(MonitorInstanceDTO dto, String plainPassword) {
        HostPort node = parseAddresses(dto.getAddresses(), defaultDbPort(dto.getInstanceType())).stream()
            .findFirst()
            .orElseThrow(() -> MonitorErrorCode.MONITOR_CONNECTION_UNAVAILABLE.exception("实例连接地址为空"));
        if (StrUtil.isBlank(dto.getDbName()) && dto.getInstanceType() != InstanceTypeEnum.DM) {
            throw MonitorErrorCode.MONITOR_CONNECTION_UNAVAILABLE.exception("DB 类型实例缺少数据库名");
        }
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(jdbcUrl(dto.getInstanceType(), node, dto.getDbName()));
        dataSource.setUsername(dto.getUsername());
        dataSource.setPassword(plainPassword);
        dataSource.setDriverClassName(driverClass(dto.getInstanceType()));
        // 监控专用小池：上限取 carlos.monitor.connection.db-pool-max-active，不对生产池造成压力
        dataSource.setMaxActive(properties.getConnection().getDbPoolMaxActive());
        dataSource.setInitialSize(1);
        dataSource.setMinIdle(1);
        dataSource.setMaxWait(dto.getConnectTimeout() == null ? 3000 : dto.getConnectTimeout());
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestOnBorrow(true);
        dataSource.setTestWhileIdle(true);
        return new DbManagedConnection(dto.getId(), dataSource);
    }

    private String jdbcUrl(InstanceTypeEnum type, HostPort node, String dbName) {
        return switch (type) {
            case POSTGRESQL -> "jdbc:postgresql://" + node.host() + ":" + node.port() + "/" + dbName;
            case MYSQL -> "jdbc:mysql://" + node.host() + ":" + node.port() + "/" + dbName
                + "?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8";
            case DM -> "jdbc:dm://" + node.host() + ":" + node.port()
                + (StrUtil.isNotBlank(dbName) ? "?schema=" + dbName : "");
            case CLICKHOUSE -> "jdbc:clickhouse://" + node.host() + ":" + node.port() + "/" + dbName;
            default -> throw MonitorErrorCode.MONITOR_INSTANCE_TYPE_UNSUPPORTED
                .exception("不支持的实例类型：" + type);
        };
    }

    private String driverClass(InstanceTypeEnum type) {
        return switch (type) {
            case POSTGRESQL -> "org.postgresql.Driver";
            case MYSQL -> "com.mysql.cj.jdbc.Driver";
            case DM -> "dm.jdbc.driver.DmDriver";
            case CLICKHOUSE -> "com.clickhouse.jdbc.ClickHouseDriver";
            default -> throw MonitorErrorCode.MONITOR_INSTANCE_TYPE_UNSUPPORTED
                .exception("不支持的实例类型：" + type);
        };
    }

    private int defaultDbPort(InstanceTypeEnum type) {
        return switch (type) {
            case POSTGRESQL -> DEFAULT_PG_PORT;
            case MYSQL -> DEFAULT_MYSQL_PORT;
            case DM -> DEFAULT_DM_PORT;
            case CLICKHOUSE -> DEFAULT_CK_PORT;
            default -> DEFAULT_REDIS_PORT;
        };
    }

    private String parseMasterName(String extConfig) {
        if (StrUtil.isBlank(extConfig) || !JSONUtil.isTypeJSON(extConfig)) {
            return null;
        }
        return JSONUtil.parseObj(extConfig).getStr(EXT_MASTER_NAME);
    }

    /**
     * 解析逗号分隔的 host:port 地址列表
     */
    private List<HostPort> parseAddresses(String addresses, int defaultPort) {
        List<HostPort> nodes = new ArrayList<>();
        if (StrUtil.isBlank(addresses)) {
            return nodes;
        }
        for (String item : addresses.split(",")) {
            String address = item.trim();
            if (address.isEmpty()) {
                continue;
            }
            String host = address;
            int port = defaultPort;
            int idx = address.lastIndexOf(':');
            if (idx > 0 && idx < address.length() - 1) {
                host = address.substring(0, idx);
                try {
                    port = Integer.parseInt(address.substring(idx + 1));
                } catch (NumberFormatException e) {
                    throw MonitorErrorCode.MONITOR_CONNECTION_UNAVAILABLE
                        .exception("连接地址端口非法：" + address);
                }
            }
            nodes.add(new HostPort(host, port));
        }
        return nodes;
    }

    /**
     * 地址节点
     */
    public record HostPort(String host, int port) {
    }
}
