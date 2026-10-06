package com.carlos.monitor.instance.manager.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.monitor.instance.manager.MonitorInstanceAppRelManager;
import com.carlos.monitor.instance.mapper.MonitorInstanceAppRelMapper;
import com.carlos.monitor.instance.pojo.entity.MonitorInstanceAppRel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 监控实例-应用关联 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MonitorInstanceAppRelManagerImpl extends BaseServiceImpl<MonitorInstanceAppRelMapper, MonitorInstanceAppRel>
    implements MonitorInstanceAppRelManager {

    @Override
    public void replaceAppRels(Long instanceId, List<String> appNames) {
        deleteByInstanceId(instanceId);
        if (CollUtil.isEmpty(appNames)) {
            return;
        }
        List<MonitorInstanceAppRel> entities = appNames.stream()
            .filter(StrUtil::isNotBlank)
            .distinct()
            .map(appName -> new MonitorInstanceAppRel().setInstanceId(instanceId).setAppName(appName))
            .toList();
        saveBatch(entities);
        log.debug("Replace 'MonitorInstanceAppRel' data, instanceId:{}, appNames:{}", instanceId, appNames);
    }

    @Override
    public List<String> listAppNames(Long instanceId) {
        if (instanceId == null) {
            return List.of();
        }
        LambdaQueryWrapper<MonitorInstanceAppRel> wrapper = queryWrapper();
        wrapper.select(MonitorInstanceAppRel::getAppName);
        wrapper.eq(MonitorInstanceAppRel::getInstanceId, instanceId);
        return list(wrapper).stream().map(MonitorInstanceAppRel::getAppName).toList();
    }

    @Override
    public List<MonitorInstanceAppRel> listByInstanceIds(Collection<Long> instanceIds) {
        if (CollUtil.isEmpty(instanceIds)) {
            return List.of();
        }
        LambdaQueryWrapper<MonitorInstanceAppRel> wrapper = queryWrapper();
        wrapper.in(MonitorInstanceAppRel::getInstanceId, instanceIds);
        return list(wrapper);
    }

    @Override
    public List<Long> listInstanceIdsByAppName(String appName) {
        if (StrUtil.isBlank(appName)) {
            return List.of();
        }
        LambdaQueryWrapper<MonitorInstanceAppRel> wrapper = queryWrapper();
        wrapper.select(MonitorInstanceAppRel::getInstanceId);
        wrapper.eq(MonitorInstanceAppRel::getAppName, appName);
        return list(wrapper).stream().map(MonitorInstanceAppRel::getInstanceId).toList();
    }

    @Override
    public void deleteByInstanceId(Serializable instanceId) {
        LambdaQueryWrapper<MonitorInstanceAppRel> wrapper = queryWrapper();
        wrapper.eq(MonitorInstanceAppRel::getInstanceId, instanceId);
        remove(wrapper);
    }
}
