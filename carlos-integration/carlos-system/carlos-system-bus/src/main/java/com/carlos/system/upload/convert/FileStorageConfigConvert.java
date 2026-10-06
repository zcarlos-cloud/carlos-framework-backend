package com.carlos.system.upload.convert;

import com.carlos.system.upload.pojo.dto.FileStorageConfigDTO;
import com.carlos.system.upload.pojo.entity.FileStorageConfig;
import com.carlos.system.upload.pojo.param.FileStorageConfigCreateParam;
import com.carlos.system.upload.pojo.param.FileStorageConfigUpdateParam;
import com.carlos.system.upload.pojo.vo.FileStorageConfigVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 文件存储配置 转换器
 *
 * @author Carlos
 * @date 2026-05-30
 */
@Mapper(uses = {CommonConvert.class})
public interface FileStorageConfigConvert {

    FileStorageConfigConvert INSTANCE = Mappers.getMapper(FileStorageConfigConvert.class);

    FileStorageConfigDTO toDTO(FileStorageConfigCreateParam param);

    FileStorageConfigDTO toDTO(FileStorageConfigUpdateParam param);

    FileStorageConfigDTO toDTO(FileStorageConfig entity);

    List<FileStorageConfigDTO> toDTO(List<FileStorageConfig> entities);

    FileStorageConfig toDO(FileStorageConfigDTO dto);

    FileStorageConfigVO toVO(FileStorageConfigDTO dto);

    List<FileStorageConfigVO> toVO(List<FileStorageConfigDTO> dtos);

    List<FileStorageConfigVO> toVOFromEntity(List<FileStorageConfig> entities);
}
