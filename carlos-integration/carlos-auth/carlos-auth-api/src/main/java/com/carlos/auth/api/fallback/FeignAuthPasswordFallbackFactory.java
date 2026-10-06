package com.carlos.auth.api.fallback;

import com.carlos.auth.api.ApiAuthPassword;
import com.carlos.auth.api.pojo.ao.PasswordEncodeAO;
import com.carlos.auth.api.pojo.ao.PasswordMatchAO;
import com.carlos.core.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * 认证服务降级处理
 *
 * @author Carlos
 * @date 2022/11/16 11:13
 */
@Slf4j
public class FeignAuthPasswordFallbackFactory implements FallbackFactory<ApiAuthPassword> {

    @Override
    public ApiAuthPassword create(final Throwable throwable) {
        final String message = throwable.getMessage();
        log.error("认证服务调用失败: message:{}", message, throwable);
        return new ApiAuthPassword() {


            @Override
            public Result<String> encodePassword(final PasswordEncodeAO request) {
                return Result.error("加密失败");
            }

            @Override
            public Result<Boolean> match(final PasswordMatchAO param) {
                return Result.error();
            }

        };
    }
}
