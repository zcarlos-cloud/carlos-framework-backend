package com.carlos.auth.idp;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.carlos.auth.api.enums.AuthErrorCode;
import com.carlos.auth.config.OAuth2Properties;
import com.carlos.auth.oauth2.grant.CustomGrantTypes;
import com.carlos.auth.util.SensitiveDataUtil;
import com.carlos.core.response.Result;
import com.carlos.integration.module.dingtalk.api.dto.DingtalkUserInfoResponse;
import com.carlos.integration.module.dingtalk.api.dto.DingtalkUserResponse;
import com.carlos.integration.module.dingtalk.config.DingtalkProperties;
import com.carlos.integration.module.dingtalk.service.DingtalkRestClientService;
import com.carlos.org.api.ApiOrgUser;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 钉钉身份源提供者
 *
 * <p>支持钉钉扫码登录和钉钉内部应用免登。</p>
 *
 * <p>登录流程：</p>
 * <ol>
 *   <li>前端调用 {@link #buildAuthorizeUrl} 获取钉钉授权 URL</li>
 *   <li>用户扫码授权后，钉钉回调到 {@link #handleCallback}</li>
 *   <li>后端用临时授权码换取用户 ID，再获取用户信息</li>
 *   <li>根据 unionId / 手机号匹配本地用户，不存在则标记为新用户</li>
 * </ol>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-05-19
 */
@Slf4j
@Component
@ConditionalOnBean(DingtalkRestClientService.class)
@RequiredArgsConstructor
public class DingTalkIdentityProvider implements IdentityProvider {

    private final DingtalkRestClientService dingtalkService;
    private final DingtalkProperties dingtalkProperties;
    private final OAuth2Properties oAuth2Properties;
    private final ApiOrgUser apiOrgUser;

    @Override
    public String getProviderId() {
        return "dingtalk";
    }

    @Override
    public String getProviderType() {
        return "oauth2";
    }

    @Override
    public Set<String> getSupportedGrantTypes() {
        return Set.of(CustomGrantTypes.DINGTALK.getValue(), CustomGrantTypes.SOCIAL.getValue());
    }

    @Override
    public UserIdentity authenticate(IdentityProviderRequest request) {
        String code = (String) request.getPrincipal();
        if (StrUtil.isBlank(code)) {
            throw new BadCredentialsException("钉钉授权码为空");
        }
        // social 类型的 principal 格式是 dingtalk:code，需要解析
        if (code.contains(":")) {
            int idx = code.indexOf(':');
            if (idx > 0) {
                code = code.substring(idx + 1);
            }
        }
        if (StrUtil.isBlank(code)) {
            throw new BadCredentialsException("钉钉授权码为空");
        }
        return authenticateByCode(code);
    }

    @Override
    public String buildAuthorizeUrl(String state, String redirectUri) {
        OAuth2Properties.DingTalk dingTalk = oAuth2Properties.getLogin().getMethods().getSocial().getDingtalk();
        if (!dingTalk.isEnabled()) {
            throw AuthErrorCode.AUTH_SOCIAL_LOGIN_SERVICE_NOT_CONFIGURED.exception("钉钉登录未启用");
        }
        String corpId = StrUtil.isNotBlank(dingTalk.getCorpId())
                ? dingTalk.getCorpId()
                : dingtalkProperties.getCorpId();
        String encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        return String.format(
                "https://login.dingtalk.com/oauth2/auth?redirect_uri=%s&response_type=code&client_id=%s&scope=openid&state=%s",
                encodedRedirect, corpId, state
        );
    }

    @Override
    public UserIdentity handleCallback(Map<String, String> callbackParams) {
        String code = callbackParams.get("code");
        if (StrUtil.isBlank(code)) {
            throw new BadCredentialsException("钉钉授权码为空");
        }
        return authenticateByCode(code);
    }

    /**
     * 通过临时授权码认证
     *
     * @param code 钉钉临时授权码
     * @return 用户身份
     */
    private UserIdentity authenticateByCode(String code) {
        log.info("DingTalk authentication by code");

        OAuth2Properties.DingTalk dingTalk = oAuth2Properties.getLogin().getMethods().getSocial().getDingtalk();
        log.debug("DingTalk login config: appKey={}, corpId={}",
                dingTalk.getAppKey(),
                StrUtil.isNotBlank(dingTalk.getCorpId()) ? dingTalk.getCorpId() : dingtalkProperties.getCorpId());

        // 1. 用 code 换取用户 ID
        DingtalkUserInfoResponse.UserGetByCodeResponse userIdResult = dingtalkService.getUserId(code);
        if (userIdResult == null || StrUtil.isBlank(userIdResult.getUserid())) {
            throw new BadCredentialsException("钉钉授权码无效或已过期");
        }
        String dingtalkUserId = userIdResult.getUserid();
        String unionId = userIdResult.getUnionid();
        log.debug("DingTalk userId obtained: userid={}, unionId={}", dingtalkUserId, unionId);

        // 2. 获取用户详细信息
        DingtalkUserResponse.DingtalkUser dingtalkUser = dingtalkService.getUserInfo(dingtalkUserId);
        if (dingtalkUser == null) {
            throw new BadCredentialsException("获取钉钉用户信息失败");
        }
        log.info("DingTalk user info obtained: name={}, mobile={}",
                dingtalkUser.getName(), SensitiveDataUtil.maskPhone(dingtalkUser.getMobile()));

        // 3. 尝试匹配本地用户（优先按手机号，其次按 unionId）
        OrgUserAO localUser = findLocalUser(dingtalkUser.getMobile(), unionId);

        boolean isNewUser = localUser == null;
        String localUserId = isNewUser ? null : String.valueOf(localUser.getId());
        String username = isNewUser
                ? "dingtalk_" + dingtalkUserId
                : localUser.getAccount();

        // 4. 查询角色
        Set<String> roleCodes = Collections.emptySet();
        if (localUser != null && localUser.getId() != null) {
            try {
                RoleProvider roleProvider = SpringUtil.getBean(RoleProvider.class);
                roleCodes = roleProvider.getRoleCodesByUserId(localUser.getId());
            } catch (Exception e) {
                log.warn("Failed to get RoleProvider, using empty roles");
            }
        }

        // 5. 构建统一用户身份（含扩展属性，保持不可变性）
        Map<String, Object> extraAttrs = Map.of(
                "unionId", unionId,
                "deptIdList", dingtalkUser.getDeptIdList() != null ? dingtalkUser.getDeptIdList() : Collections.emptyList(),
                "title", StrUtil.nullToEmpty(dingtalkUser.getTitle()),
                "avatar", StrUtil.nullToEmpty(dingtalkUser.getAvatar()),
                "localUserId", StrUtil.nullToEmpty(localUserId)
        );

        return UserIdentity.builder()
                .providerId(getProviderId())
                .providerUserId(dingtalkUserId)
                .username(username)
                .email(dingtalkUser.getEmail())
                .phone(dingtalkUser.getMobile())
                .roleCodes(roleCodes)
                .isNewUser(isNewUser)
                .extraAttrs(extraAttrs)
                .build();
    }

    /**
     * 查找本地用户（优先手机号匹配，其次 unionId）
     */
    private OrgUserAO findLocalUser(String mobile, String unionId) {
        try {
            // 优先按手机号匹配
            if (StrUtil.isNotBlank(mobile)) {
                Result<OrgUserAO> result = apiOrgUser.getUserByIdentifier(mobile);
                if (result != null && result.isSuccess() && result.getData() != null) {
                    log.debug("Local user matched by mobile");
                    return result.getData();
                }
            }

            // 其次按 unionId 匹配（如果 org 模块支持）
            if (StrUtil.isNotBlank(unionId)) {
                Result<OrgUserAO> result = apiOrgUser.getUserByIdentifier("dingtalk:" + unionId);
                if (result != null && result.isSuccess() && result.getData() != null) {
                    log.debug("Local user matched by unionId");
                    return result.getData();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to find local user for DingTalk login: {}", e.getMessage());
        }
        return null;
    }
}
