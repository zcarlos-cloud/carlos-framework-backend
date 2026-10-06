package com.carlos.org.convert;


import com.carlos.org.api.pojo.ao.OrgTenantAO;
import com.carlos.org.pojo.dto.OrgTenantDTO;
import com.carlos.org.pojo.entity.OrgTenant;
import com.carlos.org.pojo.param.OrgTenantCreateParam;
import com.carlos.org.pojo.param.OrgTenantUpdateParam;
import com.carlos.org.pojo.vo.OrgTenantVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;


/**
 * <p>
 * 租户 mapstruct
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Mapper(componentModel = "spring")
public interface OrgTenantConvert {

    OrgTenantConvert INSTANCE = Mappers.getMapper(OrgTenantConvert.class);

    // ==================== Entity ↔ DTO ====================

    /**
     * 转 do
     */
    OrgTenant toDO(OrgTenantDTO dto);

    /**
     * 转 DTO
     */
    OrgTenantDTO toDTO(OrgTenant entity);

    // ==================== Param ↔ DTO ====================

    /**
     * Param to DTO
     */
    OrgTenantDTO toDTO(OrgTenantCreateParam param);

    /**
     * UpdateParam to DTO
     */
    OrgTenantDTO toDTO(OrgTenantUpdateParam param);

    // ==================== DTO ↔ VO ====================

    /**
     * DTO to VO
     */
    @Mappings({
        @Mapping(target = "tenantStatus", expression = "java(dto.getTenantStatus() != null ? dto.getTenantStatus().getCode() : null)"),
        @Mapping(target = "tenantStatusName", expression = "java(dto.getTenantStatus() != null ? dto.getTenantStatus().getDesc() : null)")
    })
    OrgTenantVO toVO(OrgTenantDTO dto);

    /**
     * DTO List to VO List
     */
    List<OrgTenantVO> toVOList(List<OrgTenantDTO> dtoList);

    // ==================== DTO ↔ AO ====================

    /**
     * DTO to AO
     */
    @Mapping(target = "tenantStatus", expression = "java(dto.getTenantStatus() != null ? dto.getTenantStatus().getCode() : null)")
    OrgTenantAO toAO(OrgTenantDTO dto);

}
