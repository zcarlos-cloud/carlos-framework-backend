package com.carlos.audit.clickhouse;

import com.carlos.audit.api.pojo.enums.AuditErrorCode;
import com.carlos.audit.config.AuditProperties;
import com.carlos.core.exception.BusinessException;
import com.clickhouse.client.ClickHouseClient;
import com.clickhouse.client.ClickHouseException;
import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.client.ClickHouseRequest;
import com.clickhouse.client.ClickHouseResponse;
import com.clickhouse.data.ClickHouseFormat;
import com.clickhouse.data.ClickHouseValue;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ClickHouse 通用查询客户端
 *
 * <p>封装 ClickHouse 官方客户端的查询能力，支持参数化查询和 JSONEachRow 结果映射</p>
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClickHouseQueryClient {

    /**
     * JSONEachRow 每行一个 JSON 对象，由 Jackson 映射为行对象
     *
     * <p>注意：不能用 {@code response.records(Class)} —— JSONEachRow 响应没有列元数据，
     * 客户端会把整行 JSON 当成一个 String 值返回，反射映射后所有字段都是 null。</p>
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ClickHouseClient clickHouseClient;
    private final ClickHouseNode clickHouseNode;
    private final AuditProperties auditProperties;

    /**
     * 执行查询并返回列表
     *
     * @param sql     参数化 SQL（占位符 :name）
     * @param rowType 结果行类型
     * @param params  参数值（按 SQL 中参数出现顺序）
     * @param <T>     行类型
     * @return 结果列表；无结果返回空列表，查询失败抛出 {@link BusinessException}
     */
    public <T> List<T> queryForList(String sql, Class<T> rowType, ClickHouseValue... params) {
        String limitedSql = appendQuerySettings(sql);
        try (ClickHouseResponse response = execute(limitedSql, params);
             BufferedReader reader = new BufferedReader(
                 new InputStreamReader(response.getInputStream(), StandardCharsets.UTF_8))) {
            List<T> result = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    result.add(OBJECT_MAPPER.readValue(line, rowType));
                }
            }
            return result;
        } catch (Exception e) {
            log.error("ClickHouse query failed, sql:{}", sql, e);
            throw new BusinessException(AuditErrorCode.AUDIT_STORAGE_ERROR, "ClickHouse 查询失败", e);
        }
    }

    /**
     * 执行查询并返回单行
     *
     * @param sql     参数化 SQL
     * @param rowType 结果行类型
     * @param params  参数值
     * @param <T>     行类型
     * @return 首行结果；无结果返回 null，查询失败抛出 {@link BusinessException}
     */
    public <T> T queryForObject(String sql, Class<T> rowType, ClickHouseValue... params) {
        List<T> list = queryForList(sql, rowType, params);
        return list.isEmpty() ? null : list.get(0);
    }    /**
     * 执行查询并将原始响应流写入本地文件
     *
     * <p>适用于导出 CSV/Parquet 等场景，SQL 中需自行包含 FORMAT 子句</p>
     *
     * @param sql    参数化 SQL（需包含 FORMAT 子句）
     * @param path   目标文件路径
     * @param params 参数值
     * @return 写入文件的字节数
     */
    public long exportToFile(String sql, Path path, ClickHouseValue... params) {
        try (ClickHouseResponse response = executeExport(sql, params);
             InputStream in = response.getInputStream()) {
            return Files.copy(in, path);
        } catch (Exception e) {
            log.error("ClickHouse export failed, sql:{}", sql, e);
            throw new BusinessException(AuditErrorCode.AUDIT_STORAGE_ERROR, "ClickHouse 数据导出失败", e);
        }
    }

    private ClickHouseResponse executeExport(String sql, ClickHouseValue... params)
        throws ClickHouseException, InterruptedException {
        ClickHouseRequest<?> request = newRequest(sql, null, params);
        return request.executeAndWait();
    }

    /**
     * 执行查询，响应体为 JSONEachRow 文本（由调用方解析）
     */
    private ClickHouseResponse execute(String sql, ClickHouseValue... params)
        throws ClickHouseException, InterruptedException {
        return newRequest(sql, ClickHouseFormat.JSONEachRow, params).executeAndWait();
    }

    private ClickHouseRequest<?> newRequest(String sql, ClickHouseFormat format, ClickHouseValue... params) {
        ClickHouseRequest<?> request = clickHouseClient.read(clickHouseNode).query(sql);
        if (format != null) {
            request.format(format);
        }
        if (params != null && params.length > 0) {
            ClickHouseValue[] more = params.length > 1 ? Arrays.copyOfRange(params, 1, params.length)
                : new ClickHouseValue[0];
            request.params(params[0], more);
        }
        return request;
    }

    private String appendQuerySettings(String sql) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }
        AuditProperties.ClickHouse clickhouse = auditProperties.getClickhouse();
        return sql + " SETTINGS max_execution_time = " + clickhouse.getMaxExecutionTimeSeconds()
            + ", max_rows_to_read = " + clickhouse.getMaxRowsToRead();
    }
}
