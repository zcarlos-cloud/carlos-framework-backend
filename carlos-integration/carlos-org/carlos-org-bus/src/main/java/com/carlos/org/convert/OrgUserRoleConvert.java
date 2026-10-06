package com.carlos.org.convert;

import com.carlos.org.api.pojo.ao.OrgUserRoleAO;
import com.carlos.org.pojo.dto.OrgUserRoleDTO;
import com.carlos.org.pojo.entity.OrgUserRole;
import com.carlos.org.pojo.param.OrgUserRoleCreateParam;
import com.carlos.org.pojo.param.OrgUserRoleUpdateParam;
import com.carlos.org.pojo.vo.OrgUserRoleVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 用户角色 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Mapper(uses = {CommonConvert.class})
public interface OrgUserRoleConvert {

    OrgUserRoleConvert INSTANCE = Mappers.getMapper(OrgUserRoleConvert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgUserRoleDTO toDTO(OrgUserRoleCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgUserRoleDTO toDTO(OrgUserRoleUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    List<OrgUserRoleDTO> toDTO(List<OrgUserRole> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgUserRoleDTO toDTO(OrgUserRole entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgUserRole toDO(OrgUserRoleDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgUserRoleVO toVO(OrgUserRoleDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    List<OrgUserRoleVO> toVO(List<OrgUserRole> dos);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param entity 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgUserRoleVO toVO(OrgUserRole entity);

    // ==================== DTO ↔ AO ====================

    /**
     * DTO to AO
     */
    OrgUserRoleAO toAO(OrgUserRoleDTO dto);

    /**
     * DTO List to AO List
     */
    List<OrgUserRoleAO> toAOList(List<OrgUserRoleDTO> dtoList);
}
