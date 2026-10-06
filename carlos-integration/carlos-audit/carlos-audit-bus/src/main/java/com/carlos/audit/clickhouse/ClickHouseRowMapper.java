package com.carlos.audit.clickhouse;

import com.carlos.audit.api.pojo.enums.AuditLogAuthProviderEnum;
import com.carlos.audit.api.pojo.enums.AuditLogAuthTypeEnum;
import com.carlos.audit.api.pojo.enums.AuditLogBizChannelEnum;
import com.carlos.audit.api.pojo.enums.AuditLogCategoryEnum;
import com.carlos.audit.pojo.enums.AuditLogPayloadStorageTypeEnum;
import com.carlos.audit.api.pojo.enums.AuditLogPrincipalTypeEnum;
import com.carlos.audit.api.pojo.enums.AuditLogStatusEnum;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.audit.pojo.vo.AuditLogStatsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ClickHouse 行对象与 DTO/VO 之间的转换器
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Slf4j
public class ClickHouseRowMapper {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private ClickHouseRowMapper() {
    }

    /**
     * 将 {@link ClickHouseQueryRow} 转换为 {@link AuditLogMainDTO}
     *
     * @param row ClickHouse 查询行
     * @return DTO
     */
    public static AuditLogMainDTO toDto(ClickHouseQueryRow row) {
        if (row == null) {
            return null;
        }
        AuditLogMainDTO dto = new AuditLogMainDTO();
        BeanUtils.copyProperties(row, dto);

        dto.setServerTime(parseDateTime(row.getServerTime()));
        dto.setEventDate(parseDate(row.getEventDate()));
        dto.setClientTime(parseDateTime(row.getClientTime()));
        dto.setEventTime(parseDateTime(row.getEventTime()));
        dto.setRetentionDeadline(parseDate(row.getRetentionDeadline()));
        dto.setCreatedTime(parseDateTime(row.getCreatedTime()));
        dto.setUpdatedTime(parseDateTime(row.getUpdatedTime()));

        dto.setCategory(parseEnum(row.getCategory(), AuditLogCategoryEnum.class));
        dto.setPrincipalType(parseEnum(row.getPrincipalType(), AuditLogPrincipalTypeEnum.class));
        dto.setLogStatus(parseEnum(row.getLogStatus(), AuditLogStatusEnum.class));
        dto.setAuthType(parseEnum(row.getAuthType(), AuditLogAuthTypeEnum.class));
        dto.setAuthProvider(parseEnum(row.getAuthProvider(), AuditLogAuthProviderEnum.class));
        dto.setBizChannel(parseEnum(row.getBizChannel(), AuditLogBizChannelEnum.class));
        dto.setPayloadStorageType(parseEnum(row.getPayloadStorageType(), AuditLogPayloadStorageTypeEnum.class));

        // BeanUtils 无法拷贝 List<String> -> String，数组列需显式拼接为逗号分隔字符串
        dto.setTagKeys(joinList(row.getTagKeys()));
        dto.setTagValues(joinList(row.getTagValues()));
        dto.setAttachmentTypes(joinList(row.getAttachmentTypes()));

        return dto;
    }

    /**
     * 将 ClickHouse Array 列值拼接为逗号分隔字符串（null 保留为 null）
     */
    private static String joinList(List<String> values) {
        return values == null ? null : String.join(",", values);
    }

    /**
     * 将统计行转换为 VO
     *
     * @param row 统计行
     * @return VO
     */
    public static AuditLogStatsVO toStatsVo(AuditLogStatsRow row) {
        if (row == null) {
            return new AuditLogStatsVO();
        }
        AuditLogStatsVO vo = new AuditLogStatsVO();
        BeanUtils.copyProperties(row, vo);
        vo.setDate(parseDate(row.getDate()));
        return vo;
    }

    private static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATETIME_FORMATTER);
        } catch (Exception e) {
            log.warn("Parse datetime failed:{}", value);
            return null;
        }
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("Parse date failed:{}", value);
            return null;
        }
    }

    private static <E extends Enum<E>> E parseEnum(String value, Class<E> enumType) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException e) {
            log.warn("Parse enum failed, type:{}, value:{}", enumType.getSimpleName(), value);
            return null;
        }
    }
}
