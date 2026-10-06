package com.carlos.audit.clickhouse;

import com.carlos.audit.api.pojo.enums.AuditLogAuthProviderEnum;
import com.carlos.audit.api.pojo.enums.AuditLogAuthTypeEnum;
import com.carlos.audit.api.pojo.enums.AuditLogBizChannelEnum;
import com.carlos.audit.api.pojo.enums.AuditLogCategoryEnum;
import com.carlos.audit.api.pojo.enums.AuditLogPrincipalTypeEnum;
import com.carlos.audit.api.pojo.enums.AuditLogStatusEnum;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.audit.pojo.enums.AuditLogPayloadStorageTypeEnum;
import com.carlos.audit.pojo.vo.AuditLogStatsVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link ClickHouseRowMapper} 单元测试
 *
 * @author Carlos
 * @date 2026-06-12
 */
class ClickHouseRowMapperTest {

    @Test
    @DisplayName("toDto：空行应返回 null")
    void should_return_null_when_row_is_null() {
        assertNull(ClickHouseRowMapper.toDto(null));
    }

    @Test
    @DisplayName("toDto：完整查询行应正确转换为 DTO")
    void should_map_full_row_to_dto() {
        ClickHouseQueryRow row = new ClickHouseQueryRow();
        row.setId(123L);
        row.setServerTime("2026-06-12 10:20:30.123");
        row.setEventDate("2026-06-12");
        row.setClientTime("2026-06-12 10:20:30.000");
        row.setEventTime("2026-06-12 10:21:00.000");
        row.setRetentionDeadline("2026-06-19");
        row.setCreatedTime("2026-06-12 10:20:30.456");
        row.setUpdatedTime("2026-06-12 10:20:31.789");
        row.setCategory(AuditLogCategoryEnum.SECURITY.name());
        row.setPrincipalType(AuditLogPrincipalTypeEnum.USER.name());
        row.setLogStatus(AuditLogStatusEnum.SUCCESS.name());
        row.setAuthType(AuditLogAuthTypeEnum.PASSWORD.name());
        row.setAuthProvider(AuditLogAuthProviderEnum.LOCAL.name());
        row.setBizChannel(AuditLogBizChannelEnum.WEB.name());
        row.setPayloadStorageType(AuditLogPayloadStorageTypeEnum.OSS.name());

        AuditLogMainDTO dto = ClickHouseRowMapper.toDto(row);

        assertNotNull(dto);
        assertEquals(123L, dto.getId());
        assertEquals(LocalDateTime.of(2026, 6, 12, 10, 20, 30, 123_000_000), dto.getServerTime());
        assertEquals(LocalDate.of(2026, 6, 12), dto.getEventDate());
        assertEquals(LocalDate.of(2026, 6, 19), dto.getRetentionDeadline());
        assertEquals(AuditLogCategoryEnum.SECURITY, dto.getCategory());
        assertEquals(AuditLogPrincipalTypeEnum.USER, dto.getPrincipalType());
        assertEquals(AuditLogStatusEnum.SUCCESS, dto.getLogStatus());
        assertEquals(AuditLogAuthTypeEnum.PASSWORD, dto.getAuthType());
        assertEquals(AuditLogAuthProviderEnum.LOCAL, dto.getAuthProvider());
        assertEquals(AuditLogBizChannelEnum.WEB, dto.getBizChannel());
        assertEquals(AuditLogPayloadStorageTypeEnum.OSS, dto.getPayloadStorageType());
    }

    @Test
    @DisplayName("toDto：非法枚举值应返回 null 而不是抛异常")
    void should_return_null_enum_when_value_invalid() {
        ClickHouseQueryRow row = new ClickHouseQueryRow();
        row.setCategory("NOT_EXISTS");

        AuditLogMainDTO dto = ClickHouseRowMapper.toDto(row);

        assertNotNull(dto);
        assertNull(dto.getCategory());
    }

    @Test
    @DisplayName("toStatsVo：空行应返回空 VO")
    void should_return_empty_vo_when_stats_row_is_null() {
        AuditLogStatsVO vo = ClickHouseRowMapper.toStatsVo(null);
        assertNotNull(vo);
        assertNull(vo.getDate());
    }

    @Test
    @DisplayName("toStatsVo：统计行应正确转换为 VO")
    void should_map_stats_row_to_vo() {
        AuditLogStatsRow row = new AuditLogStatsRow();
        row.setDate("2026-06-12");
        row.setTotalCount(100L);
        row.setSuccessCount(90L);
        row.setFailCount(10L);
        row.setUniqueUsers(5L);
        row.setUniqueTargets(8L);
        row.setTotalDuration(5000L);
        row.setAvgDuration(new BigDecimal("50.5"));
        row.setMaxRiskLevel(3);
        row.setTotalAttachments(2L);
        row.setTotalAmount(new BigDecimal("199.99"));

        AuditLogStatsVO vo = ClickHouseRowMapper.toStatsVo(row);

        assertNotNull(vo);
        assertEquals(LocalDate.of(2026, 6, 12), vo.getDate());
        assertEquals(100L, vo.getTotalCount());
        assertEquals(90L, vo.getSuccessCount());
        assertEquals(10L, vo.getFailCount());
        assertEquals(5L, vo.getUniqueUsers());
        assertEquals(8L, vo.getUniqueTargets());
        assertEquals(5000L, vo.getTotalDuration());
        assertEquals(new BigDecimal("50.5"), vo.getAvgDuration());
        assertEquals(3, vo.getMaxRiskLevel());
        assertEquals(2L, vo.getTotalAttachments());
        assertEquals(new BigDecimal("199.99"), vo.getTotalAmount());
    }
}
