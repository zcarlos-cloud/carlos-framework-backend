package com.carlos.devkit.convert;

import com.carlos.devkit.pojo.dto.TemplateVersionDTO;
import com.carlos.devkit.pojo.entity.DevTemplateVersion;
import com.carlos.devkit.pojo.vo.TemplateVersionVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 模板版本历史 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Mapper
public interface TemplateVersionConvert {

    TemplateVersionConvert INSTANCE = Mappers.getMapper(TemplateVersionConvert.class);

    /**
     * 实体转 DTO
     *
     * @param entity 实体
     * @return DTO
     */
    TemplateVersionDTO toDTO(DevTemplateVersion entity);

    /**
     * 实体列表转 DTO 列表
     *
     * @param entities 实体列表
     * @return DTO 列表
     */
    List<TemplateVersionDTO> toDTO(List<DevTemplateVersion> entities);

    /**
     * DTO 转实体
     *
     * @param dto DTO
     * @return 实体
     */
    DevTemplateVersion toDO(TemplateVersionDTO dto);

    /**
     * DTO 转 VO（不含正文，供分页列表）
     *
     * @param dto DTO
     * @return VO
     */
    TemplateVersionVO toVO(TemplateVersionDTO dto);

    /**
     * DTO 转 VO（含正文，供 diff 对比）
     *
     * @param dto DTO
     * @return VO
     */
    TemplateVersionVO toDetailVO(TemplateVersionDTO dto);
}
