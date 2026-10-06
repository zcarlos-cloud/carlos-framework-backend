package com.carlos.audit.clickhouse;

import com.carlos.audit.config.AuditProperties;
import com.clickhouse.client.ClickHouseClient;
import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.client.ClickHouseRequest;
import com.clickhouse.client.ClickHouseResponse;
import com.clickhouse.data.ClickHouseFormat;
import com.clickhouse.data.ClickHouseInputStream;
import com.clickhouse.data.ClickHouseValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * {@link ClickHouseQueryClient} 单元测试
 *
 * @author Carlos
 * @date 2026-06-12
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClickHouseQueryClientTest {

    @Mock
    private ClickHouseClient clickHouseClient;

    @Mock
    private ClickHouseNode clickHouseNode;

    @Mock
    private ClickHouseRequest<?> request;

    @Mock
    private ClickHouseResponse response;

    private ClickHouseQueryClient queryClient;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        AuditProperties properties = new AuditProperties();
        properties.getClickhouse().setMaxExecutionTimeSeconds(10);
        properties.getClickhouse().setMaxRowsToRead(10_000_000L);
        queryClient = new ClickHouseQueryClient(clickHouseClient, clickHouseNode, properties);

        doReturn(request).when(clickHouseClient).read(clickHouseNode);
        doReturn(request).when(request).query(anyString());
        doReturn(request).when(request).format(any(ClickHouseFormat.class));
        doReturn(request).when(request).params(any(ClickHouseValue.class), any(ClickHouseValue[].class));
        doReturn(response).when(request).executeAndWait();
        // JSONEachRow 响应流：默认空结果集
        doReturn(ClickHouseInputStream.empty()).when(response).getInputStream();
    }

    private ClickHouseInputStream streamOf(String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return ClickHouseInputStream.wrap(new ByteArrayInputStream(bytes), 0, bytes.length, null);
    }

    @Test
    @DisplayName("queryForList：SELECT 查询应追加 SETTINGS 子句")
    void should_append_settings_to_select_query() {
        queryClient.queryForList("SELECT * FROM audit_log_main_local", ClickHouseQueryRow.class);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(request).query(sqlCaptor.capture());
        String executedSql = sqlCaptor.getValue();
        assertTrue(executedSql.contains("SETTINGS max_execution_time = 10, max_rows_to_read = 10000000"));
    }

    @Test
    @DisplayName("queryForObject：应将 JSONEachRow 行映射为行对象")
    void should_map_json_each_row_line_to_row_object() throws Exception {
        String json = "{\"total_count\":\"5\"}\n";
        doReturn(streamOf(json)).when(response).getInputStream();

        AuditLogCountRow row = queryClient.queryForObject(
            "SELECT count() AS total_count FROM audit_log_main_local", AuditLogCountRow.class);

        org.junit.jupiter.api.Assertions.assertEquals(5L, row.getTotalCount());
    }

    @Test
    @DisplayName("queryForObject：应委托给 queryForList 并返回首行")
    void should_return_first_row_for_query_for_object() {
        ClickHouseQueryRow row = queryClient.queryForObject(
            "SELECT * FROM audit_log_main_local LIMIT 1", ClickHouseQueryRow.class);

        assertTrue(row == null);
    }

    @Test
    @DisplayName("queryForList：执行异常时应抛出 BusinessException")
    void should_throw_business_exception_when_query_fails() {
        doThrow(new RuntimeException("connection refused")).when(request).query(anyString());

        assertThrows(RuntimeException.class,
            () -> queryClient.queryForList("SELECT 1", ClickHouseQueryRow.class));
    }
}
