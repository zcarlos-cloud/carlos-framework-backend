package com.carlos.monitor.collect.db;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.druid.DbType;
import com.carlos.monitor.collect.db.adapter.DbTypeAdapter;
import com.carlos.monitor.collect.spi.CollectorContext;
import com.carlos.monitor.collect.spi.SlowSqlRow;
import com.carlos.monitor.collect.spi.SqlFingerprintUtil;
import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.instance.connection.DbManagedConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 慢 SQL 采集器
 * 优先 Druid StatViewServlet 拉取（被监控应用同属本框架且可达时，ext_config.druidStatUrl 可达才用）；
 * 不可达/未开启时降级 DbTypeAdapter（pg_stat_statements / performance_schema digest）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlowSqlCollector {

    /**
     * ext_config JSON 中 Druid StatViewServlet 数据源地址 key
     */
    private static final String EXT_DRUID_STAT_URL = "druidStatUrl";

    /**
     * Druid stat 拉取超时（毫秒）
     */
    private static final int DRUID_STAT_TIMEOUT_MS = 2000;

    /**
     * Druid stat 单次拉取慢 SQL 上限
     */
    private static final int DRUID_STAT_LIMIT = 100;

    private final DbMetricCollector dbMetricCollector;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(DRUID_STAT_TIMEOUT_MS))
        .build();

    /**
     * 支持 DB 类型实例
     */
    public boolean support(InstanceTypeEnum instanceType) {
        return instanceType == InstanceTypeEnum.POSTGRESQL
            || instanceType == InstanceTypeEnum.MYSQL
            || instanceType == InstanceTypeEnum.DM;
    }

    /**
     * 采集慢 SQL（失败返回空列表，不影响指标点采集）
     *
     * @param ctx 采集上下文
     * @return 慢 SQL 行
     */
    public List<SlowSqlRow> collectSlowSql(CollectorContext ctx) {
        if (!(ctx.connection() instanceof DbManagedConnection dbConnection)) {
            return List.of();
        }
        Long instanceId = ctx.instanceId();
        InstanceTypeEnum instanceType = ctx.instance().getInstanceType();

        // 1. 优先 Druid StatViewServlet（需运维侧开启并在 ext_config.druidStatUrl 配置地址）
        String druidStatUrl = parseDruidStatUrl(ctx.instance().getExtConfig());
        if (StrUtil.isNotBlank(druidStatUrl)) {
            List<SlowSqlRow> rows = collectFromDruidStat(instanceId, ctx.instance().getDbName(),
                druidStatUrl, instanceType);
            if (rows != null) {
                return rows;
            }
            log.debug("Druid StatViewServlet 不可达，降级 DbTypeAdapter, instanceId:{}", instanceId);
        }

        // 2. 降级 DbTypeAdapter 系统视图
        DbTypeAdapter adapter = dbMetricCollector.findAdapter(instanceType);
        if (adapter == null) {
            return List.of();
        }
        try (Connection connection = dbConnection.getDataSource().getConnection()) {
            return adapter.collectSlowSql(instanceId, ctx.instance().getDbName(), connection);
        } catch (Exception e) {
            log.warn("慢 SQL 采集失败, instanceId:{}, error:{}", instanceId, e.getMessage());
            return List.of();
        }
    }

    /**
     * 从 Druid StatViewServlet 拉取慢 SQL（返回 null 表示不可达，调用方降级）
     * 响应结构：[{SQL, ExecuteCount, SlowCount, MaxTimespan, LastTime, DbType, ...}]
     */
    private List<SlowSqlRow> collectFromDruidStat(Long instanceId, String dbName,
                                                  String druidStatUrl, InstanceTypeEnum instanceType) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(druidStatUrl + (druidStatUrl.contains("?") ? "&" : "?")
                    + "orderBy=SQL&orderType=desc&page=1&perPageCount=" + DRUID_STAT_LIMIT))
                .timeout(Duration.ofMillis(DRUID_STAT_TIMEOUT_MS))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || !JSONUtil.isTypeJSON(response.body())) {
                return null;
            }
            DbType dbType = toDruidDbType(instanceType);
            List<SlowSqlRow> rows = new ArrayList<>();
            for (Object item : JSONUtil.parseArray(response.body())) {
                JSONObject json = (JSONObject) item;
                String sql = json.getStr("SQL");
                if (StrUtil.isBlank(sql)) {
                    continue;
                }
                String sampleSql = SqlFingerprintUtil.sampleSql(sql, dbType);
                rows.add(new SlowSqlRow(instanceId,
                    SqlFingerprintUtil.fingerprint(sampleSql, dbType), sampleSql, dbName,
                    json.getLong("ExecuteCount", 0L),
                    json.getDouble("MaxTimespan", 0D), json.getDouble("MaxTimespan", 0D),
                    Instant.now(), Instant.now()));
            }
            return rows;
        } catch (Exception e) {
            log.debug("Druid StatViewServlet 拉取失败, url:{}, error:{}", druidStatUrl, e.getMessage());
            return null;
        }
    }

    private String parseDruidStatUrl(String extConfig) {
        if (StrUtil.isBlank(extConfig) || !JSONUtil.isTypeJSON(extConfig)) {
            return null;
        }
        return JSONUtil.parseObj(extConfig).getStr(EXT_DRUID_STAT_URL);
    }

    private DbType toDruidDbType(InstanceTypeEnum instanceType) {
        return switch (instanceType) {
            case POSTGRESQL -> DbType.postgresql;
            case MYSQL -> DbType.mysql;
            case DM -> DbType.dm;
            default -> null;
        };
    }
}
