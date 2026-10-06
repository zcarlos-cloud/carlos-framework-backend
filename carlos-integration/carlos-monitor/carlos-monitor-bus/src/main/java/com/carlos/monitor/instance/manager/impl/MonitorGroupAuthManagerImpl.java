package com.carlos.monitor.instance.manager.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.monitor.instance.manager.MonitorGroupAuthManager;
import com.carlos.monitor.instance.mapper.MonitorGroupAuthMapper;
import com.carlos.monitor.instance.pojo.entity.MonitorGroupAuth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 监控分组-角色授权 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MonitorGroupAuthManagerImpl extends BaseServiceImpl<MonitorGroupAuthMapper, MonitorGroupAuth>
    implements MonitorGroupAuthManager {

    @Override
    public void replaceAuth(Long groupId, List<Long> roleIds) {
        deleteByGroupId(groupId);
        if (CollUtil.isEmpty(roleIds)) {
            return;
        }
        List<MonitorGroupAuth> entities = roleIds.stream()
            .distinct()
            .map(roleId -> new MonitorGroupAuth().setGroupId(groupId).setRoleId(roleId))
            .toList();
        saveBatch(entities);
        log.debug("Replace 'MonitorGroupAuth' data, groupId:{}, roleIds:{}", groupId, roleIds);
    }

    @Override
    public List<Long> listRoleIds(Long groupId) {
        if (groupId == null) {
            return List.of();
        }
        LambdaQueryWrapper<MonitorGroupAuth> wrapper = queryWrapper();
        wrapper.select(MonitorGroupAuth::getRoleId);
        wrapper.eq(MonitorGroupAuth::getGroupId, groupId);
        return list(wrapper).stream().map(MonitorGroupAuth::getRoleId).toList();
    }

    @Override
    public Map<Long, List<Long>> listRoleIdMap(Collection<Long> groupIds) {
        if (CollUtil.isEmpty(groupIds)) {
            return Map.of();
        }
        LambdaQueryWrapper<MonitorGroupAuth> wrapper = queryWrapper();
        wrapper.select(MonitorGroupAuth::getGroupId, MonitorGroupAuth::getRoleId);
        wrapper.in(MonitorGroupAuth::getGroupId, groupIds);
        return list(wrapper).stream()
            .collect(Collectors.groupingBy(MonitorGroupAuth::getGroupId,
                Collectors.mapping(MonitorGroupAuth::getRoleId, Collectors.toList())));
    }

    @Override
    public void deleteByGroupId(Serializable groupId) {
        LambdaQueryWrapper<MonitorGroupAuth> wrapper = queryWrapper();
        wrapper.eq(MonitorGroupAuth::getGroupId, groupId);
        remove(wrapper);
    }
}
