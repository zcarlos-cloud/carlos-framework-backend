package com.carlos.monitor.instance.service.impl;

import com.carlos.monitor.enums.MonitorErrorCode;
import com.carlos.monitor.instance.manager.MonitorGroupAuthManager;
import com.carlos.monitor.instance.manager.MonitorInstanceGroupManager;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceGroupDTO;
import com.carlos.monitor.instance.service.MonitorGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 监控实例分组 业务接口实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorGroupServiceImpl implements MonitorGroupService {

    private final MonitorInstanceGroupManager groupManager;

    private final MonitorGroupAuthManager groupAuthManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addGroup(MonitorInstanceGroupDTO dto) {
        if (groupManager.countByName(dto.getGroupName(), null) > 0) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NAME_EXISTS.exception("分组名称已存在");
        }
        groupManager.add(dto);
        groupAuthManager.replaceAuth(dto.getId(), dto.getRoleIds());
        log.info("新增监控实例分组, id:{}, groupName:{}", dto.getId(), dto.getGroupName());
        return dto.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateGroup(MonitorInstanceGroupDTO dto) {
        MonitorInstanceGroupDTO old = groupManager.getDtoById(dto.getId());
        if (old == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception("分组不存在或已删除");
        }
        if (groupManager.countByName(dto.getGroupName(), dto.getId()) > 0) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NAME_EXISTS.exception("分组名称已存在");
        }
        boolean success = groupManager.modify(dto);
        if (success && dto.getRoleIds() != null) {
            groupAuthManager.replaceAuth(dto.getId(), dto.getRoleIds());
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteGroup(Serializable id) {
        MonitorInstanceGroupDTO old = groupManager.getDtoById(id);
        if (old == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception("分组不存在或已删除");
        }
        boolean success = groupManager.delete(id);
        if (success) {
            groupAuthManager.deleteByGroupId(id);
        }
        return success;
    }

    @Override
    public MonitorInstanceGroupDTO getDetail(Serializable id) {
        MonitorInstanceGroupDTO dto = groupManager.getDtoById(id);
        if (dto == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception("分组不存在或已删除");
        }
        dto.setRoleIds(groupAuthManager.listRoleIds(dto.getId()));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAuth(Long groupId, List<Long> roleIds) {
        MonitorInstanceGroupDTO old = groupManager.getDtoById(groupId);
        if (old == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception("分组不存在或已删除");
        }
        groupAuthManager.replaceAuth(groupId, roleIds);
    }

    @Override
    public List<MonitorInstanceGroupDTO> listAll() {
        List<MonitorInstanceGroupDTO> groups = groupManager.listAll();
        for (MonitorInstanceGroupDTO group : groups) {
            group.setRoleIds(groupAuthManager.listRoleIds(group.getId()));
        }
        return groups;
    }
}
