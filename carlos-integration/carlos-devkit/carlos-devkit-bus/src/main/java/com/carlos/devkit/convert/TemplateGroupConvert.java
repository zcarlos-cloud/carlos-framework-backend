package com.carlos.devkit.convert;

import com.carlos.devkit.pojo.dto.TemplateGroupDTO;
import com.carlos.devkit.pojo.entity.DevTemplateGroup;
import com.carlos.devkit.pojo.param.TemplateGroupCreateParam;
import com.carlos.devkit.pojo.param.TemplateGroupUpdateParam;
import com.carlos.devkit.pojo.vo.TemplateGroupVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 模板组 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Mapper
public interface TemplateGroupConvert {

    TemplateGroupConvert INSTANCE = Mappers.getMapper(TemplateGroupConvert.class);

    /**
     * 新增参数转 DTO
     *
     * @param param 新增参数
     * @return DTO
     */
    TemplateGroupDTO toDTO(TemplateGroupCreateParam param);

    /**
     * 编辑参数转 DTO
     *
     * @param param 编辑参数
     * @return DTO
     */
    TemplateGroupDTO toDTO(TemplateGroupUpdateParam param);

    /**
     * 实体转 DTO
     *
     * @param entity 实体
     * @return DTO
     */
    TemplateGroupDTO toDTO(DevTemplateGroup entity);

    /**
     * 实体列表转 DTO 列表
     *
     * @param entities 实体列表
     * @return DTO 列表
     */
    List<TemplateGroupDTO> toDTO(List<DevTemplateGroup> entities);

    /**
     * DTO 转实体
     *
     * @param dto DTO
     * @return 实体
     */
    DevTemplateGroup toDO(TemplateGroupDTO dto);

    /**
     * DTO 转 VO
     *
     * @param dto DTO
     * @return VO
     */
    TemplateGroupVO toVO(TemplateGroupDTO dto);

    /**
     * DTO 列表转 VO 列表
     *
     * @param dtos DTO 列表
     * @return VO 列表
     */
    List<TemplateGroupVO> toVO(List<TemplateGroupDTO> dtos);
}
