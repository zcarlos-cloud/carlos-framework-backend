package com.carlos.system.upload.manager.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.system.upload.convert.FileStorageConfigConvert;
import com.carlos.system.upload.manager.FileStorageConfigManager;
import com.carlos.system.upload.mapper.FileStorageConfigMapper;
import com.carlos.system.upload.pojo.dto.FileStorageConfigDTO;
import com.carlos.system.upload.pojo.entity.FileStorageConfig;
import com.carlos.system.upload.pojo.param.FileStorageConfigPageParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 文件存储配置 Manager 实现
 *
 * @author Carlos
 * @date 2026-05-30
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class FileStorageConfigManagerImpl extends BaseServiceImpl<FileStorageConfigMapper, FileStorageConfig> implements FileStorageConfigManager {

    @Override
    public boolean add(FileStorageConfigDTO dto) {
        FileStorageConfig entity = FileStorageConfigConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'FileStorageConfig' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        log.debug("Insert 'FileStorageConfig' data: id:{}", entity.getId());
        return true;
    }

    @Override
    public boolean modify(FileStorageConfigDTO dto) {
        FileStorageConfig entity = FileStorageConfigConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'FileStorageConfig' data fail, entity:{}", entity);
            return false;
        }
        log.debug("Update 'FileStorageConfig' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public boolean delete(Long id) {
        if (id == null) {
            log.warn("id can't be null");
            return false;
        }
        FileStorageConfig entity = new FileStorageConfig();
        entity.setId(id);
        entity.setIsDeleted(1);
        entity.setDeletedTime(LocalDateTime.now());
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Remove 'FileStorageConfig' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'FileStorageConfig' data by id:{}", id);
        return true;
    }

    @Override
    public boolean deleteBatch(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            log.warn("ids can't be null");
            return false;
        }
        boolean success = lambdaUpdate()
                .in(FileStorageConfig::getId, ids)
                .set(FileStorageConfig::getIsDeleted, 1)
                .set(FileStorageConfig::getDeletedTime, LocalDateTime.now())
                .update();
        if (!success) {
            log.warn("Batch remove 'FileStorageConfig' data fail, ids:{}", ids);
            return false;
        }
        log.debug("Batch remove 'FileStorageConfig' data by ids:{}", ids);
        return true;
    }

    @Override
    public FileStorageConfigDTO getDtoById(Long id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        FileStorageConfig entity = getBaseMapper().selectById(id);
        return FileStorageConfigConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public IPage<FileStorageConfigDTO> pageDto(FileStorageConfigPageParam param) {
        Page<FileStorageConfig> page = new Page<>(param.getCurrent(), param.getSize());
        var query = lambdaQuery();
        if (StringUtils.isNotBlank(param.getConfigName())) {
            query.like(FileStorageConfig::getConfigName, param.getConfigName());
        }
        if (StringUtils.isNotBlank(param.getStorageType())) {
            query.eq(FileStorageConfig::getStorageType, param.getStorageType());
        }
        if (param.getConfigStatus() != null) {
            query.eq(FileStorageConfig::getConfigStatus, param.getConfigStatus());
        }
        query.orderByDesc(FileStorageConfig::getCreateTime);
        IPage<FileStorageConfig> recordPage = query.page(page);
        return recordPage.convert(FileStorageConfigConvert.INSTANCE::toDTO);
    }

    @Override
    public FileStorageConfigDTO getDefaultConfig() {
        FileStorageConfig entity = lambdaQuery()
                .eq(FileStorageConfig::getIsDefault, true)
                .eq(FileStorageConfig::getConfigStatus, 1)
                .eq(FileStorageConfig::getIsDeleted, 0)
                .one();
        return FileStorageConfigConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public boolean clearDefault() {
        boolean success = lambdaUpdate()
                .eq(FileStorageConfig::getIsDefault, true)
                .set(FileStorageConfig::getIsDefault, false)
                .update();
        if (!success) {
            log.warn("Clear default storage config failed");
            return false;
        }
        log.debug("Cleared all default storage configs");
        return true;
    }

    @Override
    public boolean setDefault(Long id) {
        boolean success = lambdaUpdate()
                .eq(FileStorageConfig::getId, id)
                .set(FileStorageConfig::getIsDefault, true)
                .update();
        if (!success) {
            log.warn("Set default storage config failed, id:{}", id);
            return false;
        }
        log.debug("Set default storage config, id:{}", id);
        return true;
    }
}
