package com.carlos.org.convert;

import com.carlos.org.api.pojo.ao.OrgDepartmentRoleAO;
import com.carlos.org.pojo.dto.OrgDepartmentRoleDTO;
import com.carlos.org.pojo.entity.OrgDepartmentRole;
import com.carlos.org.pojo.param.OrgDepartmentRoleCreateParam;
import com.carlos.org.pojo.param.OrgDepartmentRoleUpdateParam;
import com.carlos.org.pojo.vo.OrgDepartmentRoleVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 部门角色 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Mapper(uses = {CommonConvert.class})
public interface OrgDepartmentRoleConvert {

    OrgDepartmentRoleConvert INSTANCE = Mappers.getMapper(OrgDepartmentRoleConvert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgDepartmentRoleDTO toDTO(OrgDepartmentRoleCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgDepartmentRoleDTO toDTO(OrgDepartmentRoleUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    List<OrgDepartmentRoleDTO> toDTO(List<OrgDepartmentRole> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgDepartmentRoleDTO toDTO(OrgDepartmentRole entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgDepartmentRole toDO(OrgDepartmentRoleDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgDepartmentRoleVO toVO(OrgDepartmentRoleDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    List<OrgDepartmentRoleVO> toVO(List<OrgDepartmentRole> dos);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param entity 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgDepartmentRoleVO toVO(OrgDepartmentRole entity);

    // ==================== DTO ↔ AO ====================

    /**
     * DTO to AO
     */
    OrgDepartmentRoleAO toAO(OrgDepartmentRoleDTO dto);

    /**
     * DTO List to AO List
     */
    List<OrgDepartmentRoleAO> toAOList(List<OrgDepartmentRoleDTO> dtoList);
}
