package com.carlos.audit.service;

import com.carlos.audit.api.pojo.enums.AuditErrorCode;
import com.carlos.audit.clickhouse.AuditLogCountRow;
import com.carlos.audit.clickhouse.AuditLogDailyStatsRow;
import com.carlos.audit.clickhouse.AuditLogStatsRow;
import com.carlos.audit.clickhouse.ClickHouseQueryClient;
import com.carlos.audit.clickhouse.ClickHouseQueryRow;
import com.carlos.audit.manager.AuditLogMainManager;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.audit.pojo.param.AuditLogQueryPageParam;
import com.carlos.audit.pojo.vo.AuditLogDailyStatsVO;
import com.carlos.audit.pojo.vo.AuditLogStatsVO;
import com.carlos.core.exception.BusinessException;
import com.carlos.core.pagination.Paging;
import com.clickhouse.data.ClickHouseValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AuditLogQueryService} 单元测试
 *
 * @author Carlos
 * @date 2026-06-12
 */
@ExtendWith(MockitoExtension.class)
class AuditLogQueryServiceTest {

    @Mock
    private AuditLogMainManager auditLogMainManager;

    @Mock
    private ClickHouseQueryClient queryClient;

    @InjectMocks
    private AuditLogQueryService queryService;

    @Test
    @DisplayName("pageQuery：pageSize 超出上限应抛参数无效异常")
    void should_throw_when_page_size_too_large() {
        AuditLogQueryPageParam param = new AuditLogQueryPageParam();
        param.setSize(501);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> queryService.pageQuery("1", param));
        assertEquals(AuditErrorCode.AUDIT_QUERY_PARAM_INVALID.getCode(), ex.getErrorCode());
    }

    @Test
    @DisplayName("pageQuery：current 非法时应由 ParamPage 归一化为 1")
    void should_normalize_current_when_invalid() {
        AuditLogQueryPageParam param = new AuditLogQueryPageParam();
        param.setCurrent(0);
        param.setSize(20);
        when(queryClient.queryForObject(anyString(), any(Class.class), any(ClickHouseValue[].class)))
            .thenReturn(null);

        Paging<AuditLogMainDTO> paging = queryService.pageQuery("1", param);

        assertEquals(1, paging.getCurrent());
        assertEquals(20, paging.getSize());
    }

    @Test
    @DisplayName("pageQuery：总记录数为 0 时应返回空分页")
    void should_return_empty_paging_when_total_is_zero() {
        when(queryClient.queryForObject(anyString(), any(Class.class), any(ClickHouseValue[].class)))
            .thenReturn(null);

        AuditLogQueryPageParam param = new AuditLogQueryPageParam();
        param.setCurrent(1);
        param.setSize(20);

        Paging<AuditLogMainDTO> paging = queryService.pageQuery("1", param);

        assertNotNull(paging);
        assertEquals(0, paging.getTotal());
        assertTrue(paging.getRecords().isEmpty());
        assertEquals(1, paging.getCurrent());
        assertEquals(20, paging.getSize());
    }

    @Test
    @DisplayName("getById：应返回映射后的 DTO")
    void should_return_dto_when_get_by_id() {
        ClickHouseQueryRow row = new ClickHouseQueryRow();
        row.setId(100L);
        row.setServerTime("2026-06-12 10:00:00.000");
        row.setEventDate("2026-06-12");
        when(queryClient.queryForObject(anyString(), eq(ClickHouseQueryRow.class), any(ClickHouseValue[].class)))
            .thenReturn(row);

        AuditLogMainDTO dto = queryService.getById(100L);

        assertNotNull(dto);
        assertEquals(100L, dto.getId());
    }

    @Test
    @DisplayName("getById：无记录时应返回 null")
    void should_return_null_when_log_not_found() {
        when(queryClient.queryForObject(anyString(), eq(ClickHouseQueryRow.class), any(ClickHouseValue[].class)))
            .thenReturn(null);

        assertNull(queryService.getById(999L));
    }

    @Test
    @DisplayName("getRealtimeStats：应返回映射后的统计 VO")
    void should_return_stats_vo() {
        AuditLogStatsRow row = new AuditLogStatsRow();
        row.setDate("2026-06-12");
        row.setTotalCount(10L);
        row.setSuccessCount(8L);
        row.setFailCount(2L);
        when(queryClient.queryForObject(anyString(), eq(AuditLogStatsRow.class), any(ClickHouseValue[].class)))
            .thenReturn(row);

        AuditLogStatsVO vo = queryService.getRealtimeStats("1", LocalDate.of(2026, 6, 12));

        assertNotNull(vo);
        assertEquals(LocalDate.of(2026, 6, 12), vo.getDate());
        assertEquals(10L, vo.getTotalCount());
        assertEquals(8L, vo.getSuccessCount());
        assertEquals(2L, vo.getFailCount());
    }

    @Test
    @DisplayName("getUserTrail：应限制返回数量不超过 MAX_TRAIL_SIZE")
    void should_limit_user_trail_size() {
        List<AuditLogMainDTO> managerResult = Collections.nCopies(1500, new AuditLogMainDTO());
        when(auditLogMainManager.getUserTrail("user1", null, null)).thenReturn(managerResult);

        List<AuditLogMainDTO> result = queryService.getUserTrail("user1", null, null);

        assertEquals(1000, result.size());
        verify(auditLogMainManager).getUserTrail("user1", null, null);
    }

    @Test
    @DisplayName("listRiskEvents：应返回映射后的风险事件列表")
    void should_return_risk_events() {
        ClickHouseQueryRow row = new ClickHouseQueryRow();
        row.setId(200L);
        row.setServerTime("2026-06-12 10:00:00.000");
        row.setEventDate("2026-06-12");
        when(queryClient.queryForList(anyString(), eq(ClickHouseQueryRow.class), any(ClickHouseValue[].class)))
            .thenReturn(List.of(row));

        List<AuditLogMainDTO> result = queryService.listRiskEvents("1", 50, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(200L, result.get(0).getId());
    }

    @Test
    @DisplayName("getDailyStats：起始日期为空应抛时间范围无效异常")
    void should_throw_when_daily_stats_start_date_null() {
        BusinessException ex = assertThrows(BusinessException.class,
            () -> queryService.getDailyStats("1", null, LocalDate.of(2026, 6, 12)));
        assertEquals(AuditErrorCode.AUDIT_QUERY_TIME_RANGE_INVALID.getCode(), ex.getErrorCode());
    }

    @Test
    @DisplayName("getDailyStats：起始日期晚于结束日期应抛时间范围无效异常")
    void should_throw_when_daily_stats_range_reversed() {
        BusinessException ex = assertThrows(BusinessException.class,
            () -> queryService.getDailyStats("1", LocalDate.of(2026, 6, 13), LocalDate.of(2026, 6, 12)));
        assertEquals(AuditErrorCode.AUDIT_QUERY_TIME_RANGE_INVALID.getCode(), ex.getErrorCode());
    }

    @Test
    @DisplayName("getDailyStats：应返回按天聚合的统计 VO 列表")
    void should_return_daily_stats_list() {
        AuditLogDailyStatsRow row = new AuditLogDailyStatsRow();
        row.setDate("2026-06-12");
        row.setTotalCount(30L);
        row.setSuccessCount(28L);
        row.setFailCount(2L);
        row.setUniqueUsers(5L);
        when(queryClient.queryForList(anyString(), eq(AuditLogDailyStatsRow.class), any(ClickHouseValue[].class)))
            .thenReturn(List.of(row));

        List<AuditLogDailyStatsVO> result =
            queryService.getDailyStats("1", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2026, 6, 12), result.get(0).getDate());
        assertEquals(30L, result.get(0).getTotalCount());
        assertEquals(2L, result.get(0).getFailCount());
    }

    @Test
    @DisplayName("exportLogs：匹配记录数超过导出上限时应抛异常")
    void should_throw_when_export_record_count_exceeds_limit() {
        AuditLogCountRow countRow = new AuditLogCountRow();
        countRow.setTotalCount(50_001L);
        when(queryClient.queryForObject(anyString(), eq(AuditLogCountRow.class), any(ClickHouseValue[].class)))
            .thenReturn(countRow);

        AuditLogQueryPageParam param = new AuditLogQueryPageParam();
        MockHttpServletResponse response = new MockHttpServletResponse();

        BusinessException ex = assertThrows(BusinessException.class,
            () -> queryService.exportLogs("1", param, response));
        assertEquals(AuditErrorCode.AUDIT_QUERY_EXPORT_LIMIT_EXCEEDED.getCode(), ex.getErrorCode());
    }

    @Test
    @DisplayName("exportLogs：应写出 Excel 附件响应")
    void should_export_excel_attachment() throws Exception {
        AuditLogCountRow countRow = new AuditLogCountRow();
        countRow.setTotalCount(1L);
        when(queryClient.queryForObject(anyString(), eq(AuditLogCountRow.class), any(ClickHouseValue[].class)))
            .thenReturn(countRow);

        ClickHouseQueryRow row = new ClickHouseQueryRow();
        row.setId(300L);
        row.setServerTime("2026-06-12 10:00:00.000");
        row.setEventDate("2026-06-12");
        row.setOperation("用户登录");
        when(queryClient.queryForList(anyString(), eq(ClickHouseQueryRow.class), any(ClickHouseValue[].class)))
            .thenReturn(List.of(row));

        AuditLogQueryPageParam param = new AuditLogQueryPageParam();
        MockHttpServletResponse response = new MockHttpServletResponse();

        queryService.exportLogs("1", param, response);

        assertNotNull(response.getHeader("Content-disposition"));
        assertTrue(response.getContentAsByteArray().length > 0);
    }
}
