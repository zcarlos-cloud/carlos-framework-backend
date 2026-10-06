package com.carlos.system.fallback;

import com.carlos.core.response.Result;
import com.carlos.system.api.ApiSysNews;
import com.carlos.system.pojo.ao.SysNewsAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 公告 feign 熔断降级工厂
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@Component
public class ApiSysNewsFallbackFactory implements FallbackFactory<ApiSysNews> {

    @Override
    public ApiSysNews create(Throwable cause) {
        log.error("ApiSysNews 调用失败: {}", cause.getMessage());
        return id -> {
            log.error("ApiSysNews.getById 降级, id={}, cause={}", id, cause.getMessage());
            return Result.error("公告服务不可用");
        };
    }

}
