package com.carlos.devkit.convert;

import com.carlos.devkit.pojo.dto.GenerateRecordDTO;
import com.carlos.devkit.pojo.entity.DevGenerateRecord;
import com.carlos.devkit.pojo.vo.GenerateRecordVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 生成记录 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Mapper
public interface GenerateRecordConvert {

    GenerateRecordConvert INSTANCE = Mappers.getMapper(GenerateRecordConvert.class);

    /**
     * 实体转 DTO
     *
     * @param entity 实体
     * @return DTO
     */
    GenerateRecordDTO toDTO(DevGenerateRecord entity);

    /**
     * 实体列表转 DTO 列表
     *
     * @param entities 实体列表
     * @return DTO 列表
     */
    List<GenerateRecordDTO> toDTO(List<DevGenerateRecord> entities);

    /**
     * DTO 转实体
     *
     * @param dto DTO
     * @return 实体
     */
    DevGenerateRecord toDO(GenerateRecordDTO dto);

    /**
     * DTO 转 VO
     *
     * @param dto DTO
     * @return VO
     */
    GenerateRecordVO toVO(GenerateRecordDTO dto);

    /**
     * DTO 列表转 VO 列表
     *
     * @param dtos DTO 列表
     * @return VO 列表
     */
    List<GenerateRecordVO> toVO(List<GenerateRecordDTO> dtos);
}
