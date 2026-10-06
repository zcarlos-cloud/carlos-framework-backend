package com.carlos.audit.service;

import com.carlos.audit.manager.AuditLogMainManager;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.disruptor.core.DisruptorTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 审计日志宽主表（合并数据变更、技术上下文、标签、附件，保留7天热数据） 业务
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogMainService {

    private final AuditLogMainManager logMainManager;
    private final DisruptorTemplate<AuditLogMainDTO> auditDisruptorTemplate;

    /**
     * 新增审计日志（通过 Disruptor 异步写入）
     *
     * @param dto 审计日志数据
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    public void addAuditLogMain(AuditLogMainDTO dto) {
        // 使用 Disruptor 异步写入
        auditDisruptorTemplate.publishEvent(dto);
        log.debug("AuditLogMain has been published to Disruptor");
    }

    /**
     * 同步新增审计日志（重要日志使用）
     *
     * @param dto 审计日志数据
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否写入成功
     */
    public boolean addAuditLogMainSync(AuditLogMainDTO dto, long timeoutMs) {
        // 使用带超时的发布
        return auditDisruptorTemplate.tryPublishEvent(dto, timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 批量新增审计日志
     *
     * @param dtos 审计日志数据列表
     */
    public void batchAddAuditLogMain(Iterable<AuditLogMainDTO> dtos) {
        for (AuditLogMainDTO dto : dtos) {
            auditDisruptorTemplate.publishEvent(dto);
        }
        log.debug("Batch AuditLogMain has been published to Disruptor");
    }

    /**
     * 删除审计日志（仅供管理后台使用，业务层一般不删除）
     *
     * @param ids 审计日志id集合
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    public void deleteAuditLogMain(Set<Long> ids) {
        for (Serializable id : ids) {
            boolean success = logMainManager.delete(id);
            if (!success) {
                log.warn("Delete AuditLogMain failed, id: {}", id);
                continue;
            }
            log.info("Delete 'AuditLogMain' data: id:{}", id);
        }
    }

    /**
     * 修改审计日志信息（仅供管理后台使用）
     *
     * @param dto 对象信息
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    public void updateAuditLogMain(AuditLogMainDTO dto) {
        boolean success = logMainManager.modify(dto);
        if (!success) {
            log.warn("Update AuditLogMain failed, id: {}", dto.getId());
            return;
        }
        log.info("Update 'AuditLogMain' data: id:{}", dto.getId());
    }

    /**
     * 获取 Disruptor 队列状态
     *
     * @return 队列剩余容量
     */
    public long getQueueRemainingCapacity() {
        return auditDisruptorTemplate.remainingCapacity();
    }
}
