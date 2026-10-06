package com.carlos.org.manager.impl;

import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.org.manager.OrgRoleOperationManager;
import com.carlos.org.mapper.OrgRoleOperationMapper;
import com.carlos.org.pojo.entity.OrgRoleOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色-操作权限关联 查询封装实现类
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Slf4j
@Component
public class OrgRoleOperationManagerImpl extends BaseServiceImpl<OrgRoleOperationMapper, OrgRoleOperation> implements OrgRoleOperationManager {

    @Override
    public List<Long> listOperationIdsByRoleId(Long roleId) {
        if (roleId == null) {
            return List.of();
        }
        return lambdaQuery()
            .eq(OrgRoleOperation::getRoleId, roleId)
            .list()
            .stream()
            .map(OrgRoleOperation::getOperationId)
            .collect(Collectors.toList());
    }

    @Override
    public List<Long> listOperationIdsByRoleIds(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
            .in(OrgRoleOperation::getRoleId, roleIds)
            .list()
            .stream()
            .map(OrgRoleOperation::getOperationId)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRoleOperations(Long roleId, Set<Long> operationIds) {
        if (roleId == null) {
            return;
        }

        // 删除旧关联
        lambdaUpdate().eq(OrgRoleOperation::getRoleId, roleId).remove();

        // 插入新关联
        if (operationIds != null && !operationIds.isEmpty()) {
            List<OrgRoleOperation> list = operationIds.stream()
                .map(opId -> {
                    OrgRoleOperation entity = new OrgRoleOperation();
                    entity.setRoleId(roleId);
                    entity.setOperationId(opId);
                    entity.setCreateTime(LocalDateTime.now());
                    return entity;
                })
                .collect(Collectors.toList());
            saveBatch(list);
        }

        log.info("Saved role operations, roleId={}, operationCount={}", roleId, operationIds != null ? operationIds.size() : 0);
    }

}
