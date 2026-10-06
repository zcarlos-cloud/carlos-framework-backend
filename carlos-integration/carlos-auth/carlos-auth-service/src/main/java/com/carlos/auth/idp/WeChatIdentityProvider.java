package com.carlos.auth.idp;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.carlos.auth.api.enums.AuthErrorCode;
import com.carlos.auth.config.OAuth2Properties;
import com.carlos.auth.oauth2.grant.CustomGrantTypes;
import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgUser;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 微信身份源提供者
 *
 * <p>支持微信开放平台扫码登录。</p>
 *
 * <p>登录流程：</p>
 * <ol>
 *   <li>前端调用 {@link #buildAuthorizeUrl} 获取微信授权 URL</li>
 *   <li>用户扫码授权后，微信回调到 {@link #handleCallback}</li>
 *   <li>后端用 code 换取 access_token 和 openid</li>
 *   <li>用 access_token 和 openid 获取用户信息</li>
 *   <li>根据 unionId / openid 匹配本地用户，不存在则标记为新用户</li>
 * </ol>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-05-19
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "carlos.auth.social.wechat", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class WeChatIdentityProvider implements IdentityProvider {

    private static final String WECHAT_ACCESS_TOKEN_BASE = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private static final String WECHAT_USERINFO_BASE = "https://api.weixin.qq.com/sns/userinfo";

    private final OAuth2Properties oAuth2Properties;
    private final ApiOrgUser apiOrgUser;
    private final RestClient restClient = RestClient.create();

    @Override
    public String getProviderId() {
        return "wechat";
    }

    @Override
    public String getProviderType() {
        return "oauth2";
    }

    @Override
    public Set<String> getSupportedGrantTypes() {
        return Set.of(CustomGrantTypes.WECHAT.getValue(), CustomGrantTypes.SOCIAL.getValue());
    }

    @Override
    public UserIdentity authenticate(IdentityProviderRequest request) {
        String code = (String) request.getPrincipal();
        if (StrUtil.isBlank(code)) {
            throw new BadCredentialsException("微信授权码为空");
        }
        // 处理 social 类型的 principal 格式 wechat:code
        if (code.contains(":")) {
            int idx = code.indexOf(':');
            code = code.substring(idx + 1);
        }
        if (StrUtil.isBlank(code)) {
            throw new BadCredentialsException("微信授权码为空");
        }
        return authenticateByCode(code);
    }

    @Override
    public String buildAuthorizeUrl(String state, String redirectUri) {
        OAuth2Properties.WeChat wechat = oAuth2Properties.getLogin().getMethods().getSocial().getWechat();
        if (!wechat.isEnabled()) {
            throw AuthErrorCode.AUTH_SOCIAL_LOGIN_SERVICE_NOT_CONFIGURED.exception("微信登录未启用");
        }
        String encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        return String.format(
            "https://open.weixin.qq.com/connect/qrconnect?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_login&state=%s#wechat_redirect",
            wechat.getAppId(), encodedRedirect, state
        );
    }

    @Override
    public UserIdentity handleCallback(Map<String, String> callbackParams) {
        String code = callbackParams.get("code");
        if (StrUtil.isBlank(code)) {
            throw new BadCredentialsException("微信授权码为空");
        }
        return authenticateByCode(code);
    }

    /**
     * 通过临时授权码认证
     *
     * @param code 微信临时授权码
     * @return 用户身份
     */
    private UserIdentity authenticateByCode(String code) {
        log.info("WeChat authentication by code");
        OAuth2Properties.WeChat config = oAuth2Properties.getLogin().getMethods().getSocial().getWechat();

        // 1. 用 code 换取 access_token 和 openid
        String tokenUrl = UriComponentsBuilder.fromUriString(WECHAT_ACCESS_TOKEN_BASE)
            .queryParam("appid", config.getAppId())
            .queryParam("secret", config.getAppSecret())
            .queryParam("code", code)
            .queryParam("grant_type", "authorization_code")
            .toUriString();

        WeChatAccessTokenResponse tokenResponse = restClient.get()
            .uri(tokenUrl)
            .retrieve()
            .body(WeChatAccessTokenResponse.class);

        if (tokenResponse == null || StrUtil.isNotBlank(tokenResponse.getErrcode())) {
            String errMsg = tokenResponse != null ? tokenResponse.getErrmsg() : "微信接口无响应";
            log.error("WeChat access_token request failed: {}", errMsg);
            throw new BadCredentialsException("微信授权码无效: " + errMsg);
        }

        String accessToken = tokenResponse.getAccessToken();
        String openid = tokenResponse.getOpenid();
        String unionid = tokenResponse.getUnionid();
        log.debug("WeChat token obtained, openid: {}, unionid: {}", openid, unionid);

        // 2. 获取用户信息
        String userInfoUrl = UriComponentsBuilder.fromUriString(WECHAT_USERINFO_BASE)
            .queryParam("access_token", accessToken)
            .queryParam("openid", openid)
            .queryParam("lang", "zh_CN")
            .toUriString();

        WeChatUserInfoResponse userInfo = restClient.get()
            .uri(userInfoUrl)
            .retrieve()
            .body(WeChatUserInfoResponse.class);

        if (userInfo == null || StrUtil.isNotBlank(userInfo.getErrcode())) {
            String errMsg = userInfo != null ? userInfo.getErrmsg() : "微信接口无响应";
            log.error("WeChat userinfo request failed: {}", errMsg);
            throw new BadCredentialsException("获取微信用户信息失败: " + errMsg);
        }

        log.info("WeChat user info obtained: nickname={}", userInfo.getNickname());

        // 3. 尝试匹配本地用户（优先 unionId，其次 openid）
        OrgUserAO localUser = findLocalUser(unionid, openid);

        boolean isNewUser = localUser == null;
        String localUserId = isNewUser ? null : String.valueOf(localUser.getId());
        String username = isNewUser ? "wechat_" + openid : localUser.getAccount();

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
            "unionid", StrUtil.nullToEmpty(unionid),
            "nickname", StrUtil.nullToEmpty(userInfo.getNickname()),
            "avatar", StrUtil.nullToEmpty(userInfo.getHeadimgurl()),
            "sex", String.valueOf(userInfo.getSex()),
            "localUserId", StrUtil.nullToEmpty(localUserId)
        );

        return UserIdentity.builder()
            .providerId(getProviderId())
            .providerUserId(openid)
            .username(username)
            .phone(localUser != null ? localUser.getPhone() : null)
            .roleCodes(roleCodes)
            .isNewUser(isNewUser)
            .extraAttrs(extraAttrs)
            .build();
    }

    /**
     * 查找本地用户（优先 unionId 匹配，其次 openid）
     */
    private OrgUserAO findLocalUser(String unionId, String openid) {
        try {
            // 优先按 unionId 匹配
            if (StrUtil.isNotBlank(unionId)) {
                Result<OrgUserAO> result = apiOrgUser.getUserByIdentifier("wechat:" + unionId);
                if (result != null && result.isSuccess() && result.getData() != null) {
                    log.debug("Local user matched by unionId");
                    return result.getData();
                }
            }

            // 其次按 openid 匹配
            if (StrUtil.isNotBlank(openid)) {
                Result<OrgUserAO> result = apiOrgUser.getUserByIdentifier("wechat:" + openid);
                if (result != null && result.isSuccess() && result.getData() != null) {
                    log.debug("Local user matched by openid");
                    return result.getData();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to find local user for WeChat login: {}", e.getMessage());
        }
        return null;
    }

    // ==================== 微信 API 响应 DTO ====================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class WeChatAccessTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("expires_in")
        private Integer expiresIn;
        @JsonProperty("refresh_token")
        private String refreshToken;
        private String openid;
        private String unionid;
        private String scope;
        private String errcode;
        private String errmsg;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class WeChatUserInfoResponse {
        private String openid;
        private String nickname;
        private Integer sex;
        private String province;
        private String city;
        private String country;
        @JsonProperty("headimgurl")
        private String headimgurl;
        private String unionid;
        private String errcode;
        private String errmsg;
    }
}
