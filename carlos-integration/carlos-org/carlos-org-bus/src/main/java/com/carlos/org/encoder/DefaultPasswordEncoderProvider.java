package com.carlos.org.encoder;

import cn.hutool.extra.spring.SpringUtil;
import com.carlos.auth.api.ApiAuth;
import com.carlos.auth.api.ApiAuthPassword;
import com.carlos.auth.api.pojo.ao.PasswordEncodeAO;
import com.carlos.auth.api.pojo.ao.PasswordMatchAO;
import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 默认密码加密提供者
 * </p>
 *
 * <p>基于 {@link ApiAuth} Feign 调用认证中心完成密码编码与匹配。</p>
 *
 * <p><strong>替换方式：</strong>实现 {@link PasswordEncoderProvider} 接口并注册为 Spring Bean，
 * 本默认实现将自动失效。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultPasswordEncoderProvider implements PasswordEncoderProvider {

    @Override
    public String encode(String rawPassword) {
        PasswordEncodeAO request = new PasswordEncodeAO();
        request.setPassword(rawPassword);
        ApiAuthPassword apiAuthPassword = SpringUtil.getBean(ApiAuthPassword.class);
        Result<String> result = apiAuthPassword.encodePassword(request);
        if (!result.isSuccess()) {
            log.error("Password encoding failed: {}", result.getMsg());
            throw new BusinessException("Password encoding failed: " + result.getMsg());
        }
        String encoded = result.getData();
        log.debug("Password encoded via Feign Auth service: {}", encoded != null ? "success" : "null");
        return encoded;
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        ApiAuthPassword apiAuthPassword = SpringUtil.getBean(ApiAuthPassword.class);
        Result<Boolean> result = apiAuthPassword.match(new PasswordMatchAO(rawPassword, encodedPassword));
        if (!result.isSuccess()) {
            log.error("Password match failed: {}", result.getMsg());
            return false;
        }
        boolean matched = result.getData();
        log.debug("Password match result via Feign Auth service: {}", matched);
        return matched;
    }

}
