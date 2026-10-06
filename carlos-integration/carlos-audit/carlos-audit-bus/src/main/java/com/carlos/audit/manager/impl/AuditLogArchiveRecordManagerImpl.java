package com.carlos.audit.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.audit.api.pojo.enums.AuditErrorCode;
import com.carlos.audit.convert.AuditLogArchiveRecordConvert;
import com.carlos.audit.manager.AuditLogArchiveRecordManager;
import com.carlos.audit.mapper.AuditLogArchiveRecordMapper;
import com.carlos.audit.pojo.dto.AuditLogArchiveRecordDTO;
import com.carlos.audit.pojo.entity.AuditLogArchiveRecord;
import com.carlos.audit.pojo.enums.AuditLogArchiveStateEnum;
import com.carlos.audit.pojo.enums.AuditLogStorageTypeEnum;
import com.carlos.audit.pojo.param.AuditLogArchiveRecordPageParam;
import com.carlos.audit.pojo.vo.AuditLogArchiveRecordVO;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Locale;

/**
 * <p>
 * 审计日志归档记录（管理冷数据归档） 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AuditLogArchiveRecordManagerImpl extends BaseServiceImpl<AuditLogArchiveRecordMapper, AuditLogArchiveRecord> implements AuditLogArchiveRecordManager {

    @Override
    public boolean add(AuditLogArchiveRecordDTO dto) {
        AuditLogArchiveRecord entity = AuditLogArchiveRecordConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'AuditLogArchiveRecord' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'AuditLogArchiveRecord' data: id:{}", entity.getId());
        return true;
    }

    @Override
    public boolean delete(Serializable id) {
        if (id == null) {
            log.warn("id can't be null");
            return false;
        }
        boolean success = removeById(id);
        if (!success) {
            log.warn("Remove 'AuditLogArchiveRecord' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'AuditLogArchiveRecord' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(AuditLogArchiveRecordDTO dto) {
        AuditLogArchiveRecord entity = AuditLogArchiveRecordConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'AuditLogArchiveRecord' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操作
        log.debug("Update 'AuditLogArchiveRecord' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public AuditLogArchiveRecordDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        AuditLogArchiveRecord entity = getBaseMapper().selectById(id);
        return AuditLogArchiveRecordConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<AuditLogArchiveRecordVO> getPage(AuditLogArchiveRecordPageParam param) {
        LambdaQueryWrapper<AuditLogArchiveRecord> wrapper = queryWrapper();
        wrapper.select(

            AuditLogArchiveRecord::getId,
            AuditLogArchiveRecord::getArchiveId,
            AuditLogArchiveRecord::getArchiveDate,
            AuditLogArchiveRecord::getStartTime,
            AuditLogArchiveRecord::getEndTime,
            AuditLogArchiveRecord::getRecordCount,
            AuditLogArchiveRecord::getFileSizeBytes,
            AuditLogArchiveRecord::getStoragePath,
            AuditLogArchiveRecord::getStorageType,
            AuditLogArchiveRecord::getVerifyChecksum,
            AuditLogArchiveRecord::getArchiveStatus,
            AuditLogArchiveRecord::getCreatedTime
        );
        wrapper.orderByDesc(AuditLogArchiveRecord::getCreatedTime);

        wrapper.like(StrUtil.isNotBlank(param.getArchiveId()), AuditLogArchiveRecord::getArchiveId,
            param.getArchiveId());
        wrapper.eq(null != param.getArchiveDate(), AuditLogArchiveRecord::getArchiveDate, param.getArchiveDate());
        wrapper.eq(StrUtil.isNotBlank(param.getStorageType()), AuditLogArchiveRecord::getStorageType,
            parseStorageType(param.getStorageType()));
        wrapper.eq(StrUtil.isNotBlank(param.getArchiveStatus()), AuditLogArchiveRecord::getArchiveStatus,
            parseArchiveStatus(param.getArchiveStatus()));
        // start / end 为「记录创建时间」区间，与归档数据自身的 startTime / endTime 不同
        wrapper.ge(null != param.getStart(), AuditLogArchiveRecord::getCreatedTime, param.getStart());
        wrapper.le(null != param.getEnd(), AuditLogArchiveRecord::getCreatedTime, param.getEnd());

        PageInfo<AuditLogArchiveRecord> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, AuditLogArchiveRecordConvert.INSTANCE::toVO);
    }

    /**
     * 将前端下发的枚举名解析为实体枚举，便于 MyBatis-Plus 按 @EnumValue 转换为库值
     *
     * @param storageType 存储类型枚举名
     * @return 枚举；入参为空时返回 null（调用方据此跳过该条件）
     */
    private AuditLogStorageTypeEnum parseStorageType(String storageType) {
        if (StrUtil.isBlank(storageType)) {
            return null;
        }
        try {
            return AuditLogStorageTypeEnum.valueOf(storageType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw AuditErrorCode.AUDIT_QUERY_PARAM_INVALID.exception("未知的存储类型：" + storageType);
        }
    }

    /**
     * 将前端下发的枚举名解析为实体枚举
     *
     * @param archiveStatus 归档状态枚举名
     * @return 枚举；入参为空时返回 null（调用方据此跳过该条件）
     */
    private AuditLogArchiveStateEnum parseArchiveStatus(String archiveStatus) {
        if (StrUtil.isBlank(archiveStatus)) {
            return null;
        }
        try {
            return AuditLogArchiveStateEnum.valueOf(archiveStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw AuditErrorCode.AUDIT_QUERY_PARAM_INVALID.exception("未知的归档状态：" + archiveStatus);
        }
    }

}
