package com.carlos.auth.idp;

import cn.hutool.extra.spring.SpringUtil;
import com.carlos.auth.captcha.CaptchaService;
import com.carlos.auth.oauth2.grant.CustomGrantTypes;
import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgUser;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

/**
 * 短信验证码身份源提供者
 *
 * <p>支持短信验证码登录认证。短信验证码由 auth 模块生成并存储于 Redis，因此校验也在 auth 模块完成；
 * 用户信息通过 Feign 从 carlos-org 模块获取。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsCodeIdentityProvider implements IdentityProvider {

    private final CaptchaService captchaService;

    @Override
    public String getProviderId() {
        return "sms";
    }

    @Override
    public String getProviderType() {
        return "local";
    }

    @Override
    public Set<String> getSupportedGrantTypes() {
        return Collections.singleton(CustomGrantTypes.SMS_CODE.getValue());
    }

    @Override
    public UserIdentity authenticate(IdentityProviderRequest request) {
        String phone = (String) request.getPrincipal();
        String smsCode = (String) request.getCredentials();

        // 1. 校验短信验证码（auth 模块统一校验）
        if (!captchaService.verifySmsCaptcha(phone, smsCode)) {
            throw new BadCredentialsException("短信验证码错误或已过期");
        }

        // 2. 查询用户信息
        ApiOrgUser userApi = SpringUtil.getBean(ApiOrgUser.class);
        Result<OrgUserAO> userResult = userApi.getUserByIdentifier(phone);

        if (!userResult.isSuccess() || userResult.getData() == null) {
            throw new BadCredentialsException("用户不存在");
        }

        OrgUserAO orgUser = userResult.getData();

        // 3. 构建统一用户身份
        return convertToUserIdentity(orgUser);
    }

    private UserIdentity convertToUserIdentity(OrgUserAO user) {
        // 从角色提供者查询用户角色（懒获取，避免启动时循环依赖）
        Set<String> roleCodes;
        try {
            RoleProvider roleProvider = SpringUtil.getBean(RoleProvider.class);
            roleCodes = roleProvider.getRoleCodesByUserId(user.getId());
        } catch (Exception e) {
            roleCodes = Collections.emptySet();
        }

        return UserIdentity.builder()
            .providerId(getProviderId())
            .providerUserId(String.valueOf(user.getId()))
            .username(user.getAccount())
            .email(user.getEmail())
            .phone(user.getPhone())
            .roleCodes(roleCodes)
            .isNewUser(false)
            .build();
    }
}
