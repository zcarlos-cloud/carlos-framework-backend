package com.carlos.org.convert;

import com.carlos.org.api.pojo.ao.OrgUserDepartmentAO;
import com.carlos.org.pojo.dto.OrgUserDepartmentDTO;
import com.carlos.org.pojo.entity.OrgUserDepartment;
import com.carlos.org.pojo.param.OrgUserDepartmentCreateParam;
import com.carlos.org.pojo.param.OrgUserDepartmentUpdateParam;
import com.carlos.org.pojo.vo.OrgUserDepartmentVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 用户部门 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Mapper(uses = {CommonConvert.class})
public interface OrgUserDepartmentConvert {

    OrgUserDepartmentConvert INSTANCE = Mappers.getMapper(OrgUserDepartmentConvert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgUserDepartmentDTO toDTO(OrgUserDepartmentCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgUserDepartmentDTO toDTO(OrgUserDepartmentUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    List<OrgUserDepartmentDTO> toDTO(List<OrgUserDepartment> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgUserDepartmentDTO toDTO(OrgUserDepartment entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgUserDepartment toDO(OrgUserDepartmentDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgUserDepartmentVO toVO(OrgUserDepartmentDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    List<OrgUserDepartmentVO> toVO(List<OrgUserDepartment> dos);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param entity 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgUserDepartmentVO toVO(OrgUserDepartment entity);

    // ==================== DTO ↔ AO ====================

    /**
     * DTO to AO
     */
    @Mapping(target = "main", source = "mainDept")
    OrgUserDepartmentAO toAO(OrgUserDepartmentDTO dto);

    /**
     * DTO List to AO List
     */
    List<OrgUserDepartmentAO> toAOList(List<OrgUserDepartmentDTO> dtoList);
}
