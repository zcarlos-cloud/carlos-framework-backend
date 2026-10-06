package com.carlos.auth.api.fallback;

import com.carlos.auth.api.ApiAuth;
import com.carlos.auth.api.pojo.ao.CheckTokenAO;
import com.carlos.core.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务降级处理
 *
 * @author Carlos
 * @date 2022/11/16 11:13
 */
@Slf4j
public class FeignAuthFallbackFactory implements FallbackFactory<ApiAuth> {

    @Override
    public ApiAuth create(final Throwable throwable) {
        final String message = throwable.getMessage();
        log.error("认证服务调用失败: message:{}", message, throwable);
        return new ApiAuth() {

            @Override
            public Map<String, Object> getAccessToken(
                final MultiValueMap<String, String> param, final String authorization) {
                Map<String, Object> map = new HashMap<>(4);
                map.put("error", "service_unavailable");
                map.put("error_description", message != null ? message : "认证服务调用失败");
                return map;
            }

            @Override
            public Result<CheckTokenAO> checkToken(String token) {
                return Result.success(CheckTokenAO.builder()
                    .active(false)
                    .error("认证服务不可用")
                    .build());
            }
        };
    }
}
