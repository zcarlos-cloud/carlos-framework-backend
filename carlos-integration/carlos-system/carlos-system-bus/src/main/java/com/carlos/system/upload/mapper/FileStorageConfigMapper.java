package com.carlos.system.upload.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.system.upload.pojo.entity.FileStorageConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件存储配置 Mapper
 *
 * @author Carlos
 * @date 2026-05-30
 */
@Mapper
public interface FileStorageConfigMapper extends BaseMapper<FileStorageConfig> {
}
