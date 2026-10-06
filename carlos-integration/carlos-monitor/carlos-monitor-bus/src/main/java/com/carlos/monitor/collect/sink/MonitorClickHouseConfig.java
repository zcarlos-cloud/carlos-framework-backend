package com.carlos.monitor.collect.sink;

import cn.hutool.core.util.StrUtil;
import com.carlos.monitor.config.MonitorProperties;
import com.clickhouse.client.ClickHouseClient;
import com.clickhouse.client.ClickHouseCredentials;
import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.client.ClickHouseProtocol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>
 * 监控指标 ClickHouse 配置（配置前缀 carlos.monitor.sink.clickhouse.*）
 * 从 JDBC url（jdbc:clickhouse://host:port/database）解析节点信息
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
@ConditionalOnProperty(prefix = "carlos.monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MonitorClickHouseConfig {

    private final MonitorProperties monitorProperties;

    /**
     * ClickHouse 节点配置（供 sink 写入与查询共用）
     */
    @Bean("monitorClickHouseNode")
    public ClickHouseNode monitorClickHouseNode() {
        MonitorProperties.Sink.ClickHouse clickHouse = monitorProperties.getSink().getClickhouse();
        String url = clickHouse.getUrl();
        String remainder = StrUtil.removePrefix(url, "jdbc:clickhouse://");
        String hostPort = StrUtil.subBefore(remainder, "/", false);
        String database = StrUtil.subAfter(remainder, "/", false);
        if (StrUtil.isBlank(hostPort)) {
            hostPort = "localhost:8123";
        }
        if (StrUtil.isBlank(database)) {
            database = "default";
        }
        // 剔除 URL 参数
        int queryIdx = database.indexOf('?');
        if (queryIdx >= 0) {
            database = database.substring(0, queryIdx);
        }
        String host = StrUtil.subBefore(hostPort, ":", false);
        int port = hostPort.contains(":")
            ? Integer.parseInt(StrUtil.subAfter(hostPort, ":", false))
            : 8123;
        log.info("初始化监控 ClickHouse 节点: http://{}:{}/{}", host, port, database);

        ClickHouseNode.Builder builder = ClickHouseNode.builder()
            .host(host)
            .port(ClickHouseProtocol.HTTP, port)
            .database(database);

        // 注意：必须用 credentials() 设置账号，addOption("user", ...) 不会生效
        if (StrUtil.isNotBlank(clickHouse.getUsername())) {
            builder.credentials(ClickHouseCredentials.fromUserAndPassword(
                clickHouse.getUsername(),
                clickHouse.getPassword() == null ? "" : clickHouse.getPassword()));
        }
        return builder.build();
    }

    /**
     * ClickHouse 客户端
     */
    @Bean(name = "monitorClickHouseClient", destroyMethod = "close")
    public ClickHouseClient monitorClickHouseClient() {
        log.info("初始化监控 ClickHouse 客户端");
        return ClickHouseClient.newInstance(ClickHouseProtocol.HTTP);
    }
}
