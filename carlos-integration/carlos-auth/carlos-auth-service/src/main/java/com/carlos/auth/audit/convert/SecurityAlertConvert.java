package com.carlos.auth.audit.convert;

import com.carlos.auth.audit.pojo.dto.SecurityAlertDTO;
import com.carlos.auth.audit.pojo.entity.SecurityAlert;
import com.carlos.auth.audit.pojo.enums.AlertSeverity;
import com.carlos.auth.audit.pojo.enums.AlertType;
import com.carlos.auth.audit.pojo.vo.SecurityAlertVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * <p>
 * 安全告警对象转换
 * </p>
 *
 * <p>Entity ↔ DTO ↔ VO 之间的转换</p>
 *
 * @author Carlos
 * @date 2026-04-08
 */
@Mapper(componentModel = "spring")
public interface SecurityAlertConvert {

    /**
     * Entity 转 DTO
     */
    SecurityAlertDTO entityToDto(SecurityAlert entity);

    /**
     * DTO 转 Entity
     */
    SecurityAlert dtoToEntity(SecurityAlertDTO dto);

    /**
     * Entity 转 VO
     */
    @Mapping(source = "alertType", target = "alertTypeName", qualifiedByName = "getAlertTypeName")
    @Mapping(source = "severity", target = "severityName", qualifiedByName = "getSeverityName")
    SecurityAlertVO entityToVo(SecurityAlert entity);

    /**
     * Entity 列表转 VO 列表
     */
    List<SecurityAlertVO> entityListToVoList(List<SecurityAlert> entityList);

    /**
     * DTO 转 VO
     */
    @Mapping(source = "alertType", target = "alertTypeName", qualifiedByName = "getAlertTypeName")
    @Mapping(source = "severity", target = "severityName", qualifiedByName = "getSeverityName")
    SecurityAlertVO dtoToVo(SecurityAlertDTO dto);

    /**
     * 获取告警类型名称
     */
    @Named("getAlertTypeName")
    default String getAlertTypeName(String alertType) {
        if (alertType == null) {
            return null;
        }
        try {
            return AlertType.fromCode(alertType).getName();
        } catch (IllegalArgumentException e) {
            return alertType;
        }
    }

    /**
     * 获取告警级别名称
     */
    @Named("getSeverityName")
    default String getSeverityName(String severity) {
        if (severity == null) {
            return null;
        }
        try {
            return AlertSeverity.valueOf(severity.toUpperCase()).getName();
        } catch (IllegalArgumentException e) {
            return severity;
        }
    }
}
