package com.carlos.auth.app.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.auth.app.convert.AppPortalConvert;
import com.carlos.auth.app.manager.AppPortalManager;
import com.carlos.auth.app.mapper.AppClientMapper;
import com.carlos.auth.app.pojo.dto.AppPortalAppDTO;
import com.carlos.auth.app.pojo.entity.AppClient;
import com.carlos.auth.app.pojo.enums.ClientStatusEnum;
import com.carlos.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 应用门户查询 Manager 实现
 *
 * @author Carlos
 * @date 2026-07-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppPortalManagerImpl implements AppPortalManager {

    private static final String CACHE_KEY = "auth:app:portal:apps:visible";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final AppClientMapper appClientMapper;

    @Override
    public List<AppPortalAppDTO> listPortalVisibleApps() {
        // 优先从缓存获取
        List<AppPortalAppDTO> cached = RedisUtil.getValue(CACHE_KEY);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // 查询数据库
        LambdaQueryWrapper<AppClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppClient::getPortalVisible, true)
                .eq(AppClient::getClientStatus, ClientStatusEnum.ENABLE)
                .select(
                        AppClient::getId,
                        AppClient::getAppKey,
                        AppClient::getAppName,
                        AppClient::getAppLogo,
                        AppClient::getAppDesc,
                        AppClient::getAppType,
                        AppClient::getHomepageUrl,
                        AppClient::getDisplayOrder,
                        AppClient::getAppCategoryId
                )
                .orderByAsc(AppClient::getDisplayOrder);

        List<AppClient> entities = appClientMapper.selectList(wrapper);

        List<AppPortalAppDTO> result = AppPortalConvert.INSTANCE.toAppDTOList(entities);

        // 写入缓存
        if (!result.isEmpty()) {
            RedisUtil.setValue(CACHE_KEY, result, CACHE_TTL);
        }

        log.debug("Loaded {} portal visible apps from database", result.size());
        return result;
    }
}
