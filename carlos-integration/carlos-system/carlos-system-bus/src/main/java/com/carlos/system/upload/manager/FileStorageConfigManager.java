package com.carlos.system.upload.manager;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carlos.system.upload.pojo.dto.FileStorageConfigDTO;
import com.carlos.system.upload.pojo.param.FileStorageConfigPageParam;

import java.util.Set;

/**
 * 文件存储配置 Manager
 *
 * @author Carlos
 * @date 2026-05-30
 */
public interface FileStorageConfigManager {

    /**
     * 新增
     */
    boolean add(FileStorageConfigDTO dto);

    /**
     * 修改
     */
    boolean modify(FileStorageConfigDTO dto);

    /**
     * 删除
     */
    boolean delete(Long id);

    /**
     * 批量删除
     */
    boolean deleteBatch(Set<Long> ids);

    /**
     * 根据ID查询
     */
    FileStorageConfigDTO getDtoById(Long id);

    /**
     * 分页查询
     */
    IPage<FileStorageConfigDTO> pageDto(FileStorageConfigPageParam param);

    /**
     * 获取默认配置
     */
    FileStorageConfigDTO getDefaultConfig();

    /**
     * 取消所有默认配置
     */
    boolean clearDefault();

    /**
     * 设置默认配置
     */
    boolean setDefault(Long id);
}
