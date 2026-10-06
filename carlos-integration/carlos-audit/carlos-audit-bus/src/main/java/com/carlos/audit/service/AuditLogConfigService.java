package com.carlos.audit.service;

import com.carlos.audit.manager.AuditLogConfigManager;
import com.carlos.audit.pojo.dto.AuditLogConfigDTO;
import com.carlos.audit.storage.AuditLogStorage;
import com.carlos.log.entity.OperationLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;


/**
 * <p>
 * 审计日志配置（动态TTL与采样策略） 业务
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogConfigService {

    private static final String LOG_TYPE = "AUDIT_CONFIG_CHANGE";
    private static final String TARGET_TYPE = "AUDIT_CONFIG";

    private final AuditLogConfigManager logConfigManager;
    private final AuditLogStorage auditLogStorage;

    /**
     * 新增审计日志配置（动态TTL与采样策略）
     *
     * @param dto 审计日志配置（动态TTL与采样策略）数据
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    public void addAuditLogConfig(AuditLogConfigDTO dto) {
        boolean success = logConfigManager.add(dto);
        if (!success) {
            // 保存失败的应对措施
            return;
        }
        Serializable id = dto.getId();
        log.info("Insert 'AuditLogConfig' data: id:{}", id);
        writeSelfAudit("新增审计日志配置", String.valueOf(id), dto.getLogType(), dto);
    }

    /**
     * 删除审计日志配置（动态TTL与采样策略）
     *
     * @param ids 审计日志配置（动态TTL与采样策略）id
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    public void deleteAuditLogConfig(Set<Long> ids) {
        for (Serializable id : ids) {
            boolean success = logConfigManager.delete(id);
            if (!success) {
                // 删除失败的措施
                continue;
            }
            log.info("Delete 'AuditLogConfig' data: id:{}", id);
            writeSelfAudit("删除审计日志配置", String.valueOf(id), null, null);
        }
    }

    /**
     * 修改审计日志配置（动态TTL与采样策略）信息
     *
     * @param dto 对象信息
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    public void updateAuditLogConfig(AuditLogConfigDTO dto) {
        boolean success = logConfigManager.modify(dto);
        if (!success) {
            // 修改失败操作
            return;
        }
        // 修改成功的后续操作
        log.info("Update 'AuditLogConfig' data: id:{}", dto.getId());
        writeSelfAudit("修改审计日志配置", String.valueOf(dto.getId()), dto.getLogType(), dto);
    }

    private void writeSelfAudit(String operation, String targetId, String targetName, AuditLogConfigDTO snapshot) {
        try {
            OperationLog selfLog = AuditSelfLogHelper.build(
                LOG_TYPE, operation, TARGET_TYPE, targetId, targetName, snapshot,
                snapshot == null ? null : snapshot.getTenantId());
            auditLogStorage.storeWithoutSampling(selfLog);
        } catch (Exception e) {
            log.error("写入审计配置自审计日志失败, targetId:{}", targetId, e);
        }
    }

}
