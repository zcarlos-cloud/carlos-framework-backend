package com.carlos.auth.app.manager.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.auth.app.manager.AppRoleAccessManager;
import com.carlos.auth.app.mapper.AppRoleAccessMapper;
import com.carlos.auth.app.pojo.entity.AppRoleAccess;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用角色访问控制 查询封装实现类
 *
 * @author Carlos
 * @date 2026-07-02
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AppRoleAccessManagerImpl extends BaseServiceImpl<AppRoleAccessMapper, AppRoleAccess> implements AppRoleAccessManager {

    private static final String CACHE_KEY_PREFIX = "auth:app:role-access:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setAppRoles(Long appId, List<String> roleCodes) {
        // 删除旧记录
        LambdaQueryWrapper<AppRoleAccess> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppRoleAccess::getAppId, appId);
        remove(wrapper);

        // 插入新记录
        if (CollUtil.isNotEmpty(roleCodes)) {
            List<AppRoleAccess> entities = roleCodes.stream()
                    .map(roleCode -> new AppRoleAccess()
                            .setAppId(appId)
                            .setRoleCode(roleCode))
                    .collect(Collectors.toList());
            saveBatch(entities);
        }

        // 清理缓存
        clearAppCache(appId);
        log.debug("Set app  roles: {}", appId, roleCodes);
    }

    @Override
    public List<String> getAppRoles(Long appId) {
        String cacheKey = CACHE_KEY_PREFIX + appId;
        List<String> cached = RedisUtil.getValue(cacheKey);
        if (cached != null) {
            return cached;
        }

        LambdaQueryWrapper<AppRoleAccess> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppRoleAccess::getAppId, appId);
        wrapper.select(AppRoleAccess::getRoleCode);
        List<String> roles = list(wrapper).stream()
                .map(AppRoleAccess::getRoleCode)
                .collect(Collectors.toList());

        RedisUtil.setValue(cacheKey, roles, CACHE_TTL);
        return roles;
    }

    @Override
    public List<Long> getAccessibleAppIds(List<String> roleCodes) {
        if (CollUtil.isEmpty(roleCodes)) {
            return List.of();
        }
        LambdaQueryWrapper<AppRoleAccess> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(AppRoleAccess::getRoleCode, roleCodes);
        wrapper.select(AppRoleAccess::getAppId);
        return list(wrapper).stream()
                .map(AppRoleAccess::getAppId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getAllRestrictedAppIds() {
        LambdaQueryWrapper<AppRoleAccess> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(AppRoleAccess::getAppId);
        return list(wrapper).stream()
                .map(AppRoleAccess::getAppId)
                .distinct()
                .collect(Collectors.toList());
    }

    private void clearAppCache(Long appId) {
        RedisUtil.delete(CACHE_KEY_PREFIX + appId);
    }
}
