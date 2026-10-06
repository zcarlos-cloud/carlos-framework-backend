package com.carlos.audit.service;

import com.carlos.audit.manager.AuditLogArchiveRecordManager;
import com.carlos.audit.pojo.dto.AuditLogArchiveRecordDTO;
import com.carlos.audit.storage.AuditLogStorage;
import com.carlos.log.entity.OperationLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;


/**
 * <p>
 * 审计日志归档记录（管理冷数据归档） 业务
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogArchiveRecordService {

    private static final String LOG_TYPE = "AUDIT_ARCHIVE_RECORD_CHANGE";
    private static final String TARGET_TYPE = "AUDIT_ARCHIVE_RECORD";

    private final AuditLogArchiveRecordManager logArchiveRecordManager;
    private final AuditLogStorage auditLogStorage;

    /**
     * 新增审计日志归档记录（管理冷数据归档）
     *
     * @param dto 审计日志归档记录（管理冷数据归档）数据
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    public void addAuditLogArchiveRecord(AuditLogArchiveRecordDTO dto) {
        boolean success = logArchiveRecordManager.add(dto);
        if (!success) {
            // 保存失败的应对措施
            return;
        }
        Serializable id = dto.getId();
        log.info("Insert 'AuditLogArchiveRecord' data: id:{}", id);
        writeSelfAudit("新增审计日志归档记录", String.valueOf(id), dto.getArchiveId(), dto);
    }

    /**
     * 删除审计日志归档记录（管理冷数据归档）
     *
     * @param ids 审计日志归档记录（管理冷数据归档）id
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    public void deleteAuditLogArchiveRecord(Set<Long> ids) {
        for (Serializable id : ids) {
            boolean success = logArchiveRecordManager.delete(id);
            if (!success) {
                // 删除失败的措施
                continue;
            }
            log.info("Delete 'AuditLogArchiveRecord' data: id:{}", id);
            writeSelfAudit("删除审计日志归档记录", String.valueOf(id), null, null);
        }
    }

    /**
     * 修改审计日志归档记录（管理冷数据归档）信息
     *
     * @param dto 对象信息
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    public void updateAuditLogArchiveRecord(AuditLogArchiveRecordDTO dto) {
        boolean success = logArchiveRecordManager.modify(dto);
        if (!success) {
            // 修改失败操作
            return;
        }
        // 修改成功的后续操作
        log.info("Update 'AuditLogArchiveRecord' data: id:{}", dto.getId());
        writeSelfAudit("修改审计日志归档记录", String.valueOf(dto.getId()), dto.getArchiveId(), dto);
    }

    private void writeSelfAudit(String operation, String targetId, String targetName, AuditLogArchiveRecordDTO snapshot) {
        try {
            OperationLog selfLog = AuditSelfLogHelper.build(
                LOG_TYPE, operation, TARGET_TYPE, targetId, targetName, snapshot, null);
            auditLogStorage.storeWithoutSampling(selfLog);
        } catch (Exception e) {
            log.error("写入审计归档自审计日志失败, targetId:{}", targetId, e);
        }
    }

}
