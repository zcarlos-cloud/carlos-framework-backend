package com.carlos.org.convert;


import com.carlos.org.api.pojo.ao.OrgRoleAO;
import com.carlos.org.pojo.dto.OrgRoleDTO;
import com.carlos.org.pojo.entity.OrgRole;
import com.carlos.org.pojo.param.OrgRoleCreateParam;
import com.carlos.org.pojo.param.OrgRoleUpdateParam;
import com.carlos.org.pojo.vo.OrgRoleDetailVO;
import com.carlos.org.pojo.vo.OrgRoleVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;


/**
 * <p>
 * 角色 mapstruct
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Mapper(componentModel = "spring")
public interface OrgRoleConvert {

    OrgRoleConvert INSTANCE = Mappers.getMapper(OrgRoleConvert.class);

    // ==================== Entity ↔ DTO ====================

    /**
     * 转 do
     */
    OrgRole toDO(OrgRoleDTO dto);

    /**
     * 转 DTO
     */
    OrgRoleDTO toDTO(OrgRole entity);

    // ==================== Param ↔ DTO ====================

    /**
     * Param to DTO
     */
    OrgRoleDTO toDTO(OrgRoleCreateParam param);

    /**
     * UpdateParam to DTO
     */
    OrgRoleDTO toDTO(OrgRoleUpdateParam param);

    // ==================== DTO ↔ VO ====================

    /**
     * DTO to VO
     */
    @Mappings({
        @Mapping(target = "roleType", expression = "java(dto.getRoleType() != null ? dto.getRoleType().getCode() : null)"),
        @Mapping(target = "roleTypeName", expression = "java(dto.getRoleType() != null ? dto.getRoleType().getDesc() : null)"),
        @Mapping(target = "dataScope", expression = "java(dto.getDataScope() != null ? dto.getDataScope().getCode() : null)"),
        @Mapping(target = "dataScopeName", expression = "java(dto.getDataScope() != null ? dto.getDataScope().getDesc() : null)"),
        @Mapping(target = "roleStatus", expression = "java(dto.getRoleStatus() != null ? dto.getRoleStatus().getCode() : null)"),
        @Mapping(target = "roleStatusName", expression = "java(dto.getRoleStatus() != null ? dto.getRoleStatus().getDesc() : null)")
    })
    OrgRoleVO toVO(OrgRoleDTO dto);

    /**
     * DTO List to VO List
     */
    List<OrgRoleVO> toVOList(List<OrgRoleDTO> dtoList);

    /**
     * DTO to DetailVO
     */
    OrgRoleDetailVO toDetailVO(OrgRoleDTO dto);

    // ==================== DTO ↔ AO ====================

    /**
     * DTO to AO
     */
    @Mappings({
        @Mapping(target = "roleType", expression = "java(dto.getRoleType() != null ? dto.getRoleType().getCode() : null)"),
        @Mapping(target = "dataScope", expression = "java(dto.getDataScope() != null ? dto.getDataScope().getCode() : null)"),
        @Mapping(target = "roleStatus", expression = "java(dto.getRoleStatus() != null ? dto.getRoleStatus().getCode() : null)")
    })
    OrgRoleAO toAO(OrgRoleDTO dto);

    /**
     * DTO List to AO List
     */
    List<OrgRoleAO> toAOList(List<OrgRoleDTO> dtoList);

}
