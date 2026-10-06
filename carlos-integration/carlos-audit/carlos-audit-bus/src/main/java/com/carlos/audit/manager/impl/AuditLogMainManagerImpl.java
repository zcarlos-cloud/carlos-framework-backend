package com.carlos.audit.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.audit.convert.AuditLogMainConvert;
import com.carlos.audit.manager.AuditLogMainManager;
import com.carlos.audit.mapper.AuditLogMainMapper;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.audit.pojo.entity.AuditLogMain;
import com.carlos.audit.pojo.param.AuditLogMainPageParam;
import com.carlos.audit.pojo.vo.AuditLogMainVO;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 审计日志宽主表（合并数据变更、技术上下文、标签、附件，保留7天热数据） 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AuditLogMainManagerImpl extends BaseServiceImpl<AuditLogMainMapper, AuditLogMain> implements AuditLogMainManager {

    @Override
    public boolean add(AuditLogMainDTO dto) {
        AuditLogMain entity = AuditLogMainConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'AuditLogMain' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'AuditLogMain' data: id:{}", entity.getId());
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
            log.warn("Remove 'AuditLogMain' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'AuditLogMain' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(AuditLogMainDTO dto) {
        AuditLogMain entity = AuditLogMainConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'AuditLogMain' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操作
        log.debug("Update 'AuditLogMain' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public AuditLogMainDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        AuditLogMain entity = getBaseMapper().selectById(id);
        return AuditLogMainConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<AuditLogMainVO> getPage(AuditLogMainPageParam param) {
        LambdaQueryWrapper<AuditLogMain> wrapper = queryWrapper();
        wrapper.select(

            AuditLogMain::getId,
            AuditLogMain::getServerTime,
            AuditLogMain::getEventDate,
            AuditLogMain::getClientTime,
            AuditLogMain::getEventTime,
            AuditLogMain::getDurationMs,
            AuditLogMain::getRetentionDeadline,
            AuditLogMain::getLogSchemaVersion,
            AuditLogMain::getCategory,
            AuditLogMain::getLogType,
            AuditLogMain::getRiskLevel,
            AuditLogMain::getPrincipalId,
            AuditLogMain::getPrincipalType,
            AuditLogMain::getPrincipalName,
            AuditLogMain::getTenantId,
            AuditLogMain::getDeptId,
            AuditLogMain::getDeptName,
            AuditLogMain::getDeptPath,
            AuditLogMain::getTargetType,
            AuditLogMain::getTargetId,
            AuditLogMain::getTargetName,
            AuditLogMain::getTargetSnapshot,
            AuditLogMain::getLogStatus,
            AuditLogMain::getResultCode,
            AuditLogMain::getResultMessage,
            AuditLogMain::getOperation,
            AuditLogMain::getApprovalComment,
            AuditLogMain::getClientIp,
            AuditLogMain::getClientPort,
            AuditLogMain::getServerIp,
            AuditLogMain::getUserAgent,
            AuditLogMain::getDeviceFingerprint,
            AuditLogMain::getLocationCountry,
            AuditLogMain::getLocationProvince,
            AuditLogMain::getLocationCity,
            AuditLogMain::getLocationLat,
            AuditLogMain::getLocationLon,
            AuditLogMain::getAuthType,
            AuditLogMain::getAuthProvider,
            AuditLogMain::getRoles,
            AuditLogMain::getPermissions,
            AuditLogMain::getBizChannel,
            AuditLogMain::getBizScene,
            AuditLogMain::getBizOrderNo,
            AuditLogMain::getRelatedBizIds,
            AuditLogMain::getMonetaryAmount,
            AuditLogMain::getProcessId,
            AuditLogMain::getBatchId,
            AuditLogMain::getBatchIndex,
            AuditLogMain::getBatchTotal,
            AuditLogMain::getTaskId,
            AuditLogMain::getApproverId,
            AuditLogMain::getHasDataChange,
            AuditLogMain::getEntityClass,
            AuditLogMain::getTableName,
            AuditLogMain::getChangeSummary,
            AuditLogMain::getChangedFieldCount,
            AuditLogMain::getOldData,
            AuditLogMain::getNewData,
            AuditLogMain::getOldDataCompressed,
            AuditLogMain::getNewDataCompressed,
            AuditLogMain::getTraceId,
            AuditLogMain::getSpanId,
            AuditLogMain::getParentSpanId,
            AuditLogMain::getTracePath,
            AuditLogMain::getDbQueryCount,
            AuditLogMain::getDbQueryTimeMs,
            AuditLogMain::getExternalCallCount,
            AuditLogMain::getExternalCallTimeMs,
            AuditLogMain::getCustomMetrics,
            AuditLogMain::getRequestPayloadRef,
            AuditLogMain::getResponsePayloadRef,
            AuditLogMain::getPayloadStorageType,
            AuditLogMain::getAppName,
            AuditLogMain::getAppVersion,
            AuditLogMain::getCluster,
            AuditLogMain::getHostName,
            AuditLogMain::getTagKeys,
            AuditLogMain::getTagValues,
            AuditLogMain::getAttachmentCount,
            AuditLogMain::getAttachmentTypes,
            AuditLogMain::getAttachmentTotalSize,
            AuditLogMain::getFirstAttachmentRef,
            AuditLogMain::getAttachmentRefs,
            AuditLogMain::getDynamicTags,
            AuditLogMain::getDynamicExtras,
            AuditLogMain::getCreatedTime,
            AuditLogMain::getUpdatedTime
        );
        PageInfo<AuditLogMain> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, AuditLogMainConvert.INSTANCE::toVO);
    }

    @Override
    public List<AuditLogMainDTO> getUserTrail(String principalId, LocalDateTime startTime, LocalDateTime endTime) {
        if (principalId == null || principalId.isEmpty()) {
            log.warn("principalId cannot be null or empty");
            return Collections.emptyList();
        }

        LambdaQueryWrapper<AuditLogMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLogMain::getPrincipalId, principalId);

        if (Objects.nonNull(startTime)) {
            wrapper.ge(AuditLogMain::getServerTime, startTime);
        }
        if (Objects.nonNull(endTime)) {
            wrapper.le(AuditLogMain::getServerTime, endTime);
        }

        wrapper.orderByAsc(AuditLogMain::getServerTime);

        List<AuditLogMain> entityList = getBaseMapper().selectList(wrapper);
        return AuditLogMainConvert.INSTANCE.toDTO(entityList);
    }

}
