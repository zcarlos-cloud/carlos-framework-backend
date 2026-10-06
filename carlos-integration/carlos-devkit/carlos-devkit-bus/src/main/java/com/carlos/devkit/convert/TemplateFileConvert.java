package com.carlos.devkit.convert;

import com.carlos.devkit.pojo.dto.TemplateFileDTO;
import com.carlos.devkit.pojo.entity.DevTemplate;
import com.carlos.devkit.pojo.param.TemplateFileCreateParam;
import com.carlos.devkit.pojo.vo.TemplateFileVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 模板文件 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Mapper
public interface TemplateFileConvert {

    TemplateFileConvert INSTANCE = Mappers.getMapper(TemplateFileConvert.class);

    /**
     * 新增参数转 DTO
     *
     * @param param 新增参数
     * @return DTO
     */
    TemplateFileDTO toDTO(TemplateFileCreateParam param);

    /**
     * 实体转 DTO
     *
     * @param entity 实体
     * @return DTO
     */
    TemplateFileDTO toDTO(DevTemplate entity);

    /**
     * 实体列表转 DTO 列表
     *
     * @param entities 实体列表
     * @return DTO 列表
     */
    List<TemplateFileDTO> toDTO(List<DevTemplate> entities);

    /**
     * DTO 转实体
     *
     * @param dto DTO
     * @return 实体
     */
    DevTemplate toDO(TemplateFileDTO dto);

    /**
     * DTO 转 VO（不含正文，供列表场景）
     *
     * @param dto DTO
     * @return VO
     */
    TemplateFileVO toVO(TemplateFileDTO dto);

    /**
     * DTO 转 VO（含正文，供在线编辑场景）
     *
     * @param dto DTO
     * @return VO
     */
    TemplateFileVO toDetailVO(TemplateFileDTO dto);
}
