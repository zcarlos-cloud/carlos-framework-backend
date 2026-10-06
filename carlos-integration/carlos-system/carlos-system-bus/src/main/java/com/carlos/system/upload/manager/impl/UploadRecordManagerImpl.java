package com.carlos.system.upload.manager.impl;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.system.upload.convert.UploadRecordConvert;
import com.carlos.system.upload.manager.UploadRecordManager;
import com.carlos.system.upload.mapper.UploadRecordMapper;
import com.carlos.system.upload.pojo.dto.UploadRecordDTO;
import com.carlos.system.upload.pojo.entity.UploadRecord;
import com.carlos.system.upload.pojo.param.UploadRecordPageParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 文件上传记录 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2022-2-7 15:22:31
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class UploadRecordManagerImpl extends BaseServiceImpl<UploadRecordMapper, UploadRecord> implements UploadRecordManager {

    @Override
    public boolean add(UploadRecordDTO dto) {
        UploadRecord entity = UploadRecordConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'UploadRecord' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'UploadRecord' data: id:{}", entity.getId());
        return true;
    }

    @Override
    public boolean delete(Serializable id) {
        if (id == null) {
            log.warn("id can't be null");
            return false;
        }
        UploadRecord entity = new UploadRecord();
        entity.setId(Long.valueOf(id.toString()));
        entity.setIsDeleted(1);
        entity.setDeletedTime(LocalDateTime.now());
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Remove 'UploadRecord' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'UploadRecord' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(UploadRecordDTO dto) {
        UploadRecord entity = UploadRecordConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'UploadRecord' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操作
        log.debug("Update 'UploadRecord' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public UploadRecordDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        UploadRecord entity = getBaseMapper().selectById(id);
        return UploadRecordConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public List<UploadRecordDTO> getDtoByGroupId(String groupId) {
        if (StringUtils.isBlank(groupId)) {
            log.warn("groupId is null");
            return null;
        }
        List<UploadRecord> uploadRecords = lambdaQuery().eq(UploadRecord::getGroupId, groupId).list();
        return UploadRecordConvert.INSTANCE.toDTO(uploadRecords);
    }

    @Override
    public List<UploadRecordDTO> getDtoByIds(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            log.warn("ids is null");
            return null;
        }
        List<UploadRecord> uploadRecords = lambdaQuery().in(UploadRecord::getId, ids).list();
        return UploadRecordConvert.INSTANCE.toDTO(uploadRecords);
    }

    @Override
    public IPage<UploadRecordDTO> pageDto(UploadRecordPageParam param) {
        Page<UploadRecord> page = new Page<>(param.getCurrent(), param.getSize());
        var query = lambdaQuery();
        if (StringUtils.isNotBlank(param.getOriginalName())) {
            query.like(UploadRecord::getOriginalName, param.getOriginalName());
        }
        if (StringUtils.isNotBlank(param.getUses())) {
            query.eq(UploadRecord::getUses, param.getUses());
        }
        if (param.getCreateBy() != null) {
            query.eq(UploadRecord::getCreateBy, param.getCreateBy());
        }
        if (StringUtils.isNotBlank(param.getFileExtension())) {
            query.eq(UploadRecord::getFileExtension, param.getFileExtension());
        }
        if (StringUtils.isNotBlank(param.getStorageType())) {
            query.eq(UploadRecord::getStorageType, param.getStorageType());
        }
        if (param.getStartTime() != null) {
            query.ge(UploadRecord::getCreateTime, param.getStartTime());
        }
        if (param.getEndTime() != null) {
            query.le(UploadRecord::getCreateTime, param.getEndTime());
        }
        query.orderByDesc(UploadRecord::getCreateTime);
        IPage<UploadRecord> recordPage = query.page(page);
        List<UploadRecordDTO> dtoList = UploadRecordConvert.INSTANCE.toDTO(recordPage.getRecords());
        Page<UploadRecordDTO> result = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        result.setRecords(dtoList);
        return result;
    }

    @Override
    public boolean deleteBatch(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            log.warn("ids can't be null");
            return false;
        }
        boolean success = lambdaUpdate()
            .in(UploadRecord::getId, ids)
            .set(UploadRecord::getIsDeleted, 1)
            .set(UploadRecord::getDeletedTime, LocalDateTime.now())
            .update();
        if (!success) {
            log.warn("Batch remove 'UploadRecord' data fail, ids:{}", ids);
            return false;
        }
        log.debug("Batch remove 'UploadRecord' data by ids:{}", ids);
        return true;
    }
}
