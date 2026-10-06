package com.carlos.auth.oauth2.client;

import com.carlos.auth.oauth2.OAuth2ErrorCodesExpand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsSet;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 客户端凭证模式 Token 自定义增强器
 *
 * <p>针对 CLIENT_CREDENTIALS 授权类型的 Token 进行增强，添加客户端相关信息。</p>
 *
 * <h3>添加的声明：</h3>
 * <ul>
 *   <li>client_id - 客户端ID</li>
 *   <li>grant_type - 授权类型</li>
 *   <li>token_type - 令牌类型（client_token）</li>
 *   <li>issued_at - 签发时间（格式化）</li>
 *   <li>env - 环境标识（从配置读取）</li>
 * </ul>
 *
 * <h3>使用场景：</h3>
 * <p>服务间调用时，资源服务器可以通过 Token 中的 client_id 识别调用方，
 * 进行相应的权限控制。</p>
 *
 * <h3>扩展方式：</h3>
 * <p>如果需要添加更多客户端信息，可以注册自定义的 OAuth2TokenCustomizer Bean，
 * 或继承此类重写 customize 方法。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 * @see OAuth2TokenCustomizer
 * @see AuthorizationGrantType#CLIENT_CREDENTIALS
 */
@Slf4j
public class CustomizeClientOAuth2TokenCustomizer implements OAuth2TokenCustomizer<OAuth2TokenClaimsContext> {

    /**
     * Token 类型标识
     */
    private static final String TOKEN_TYPE = "client_token";

    /**
     * 时间格式化器
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /**
     * 自定义 Token 声明
     *
     * <p>仅处理客户端凭证模式（CLIENT_CREDENTIALS）的 Token。</p>
     *
     * @param context Token 声明上下文
     */
    @Override
    public void customize(OAuth2TokenClaimsContext context) {
        // 只处理客户端凭证模式
        if (!AuthorizationGrantType.CLIENT_CREDENTIALS.equals(
            context.getAuthorizationGrantType())) {
            return;
        }

        OAuth2TokenClaimsSet.Builder claims = context.getClaims();

        // 获取客户端ID
        String clientId = context.getPrincipal().getName();

        // 构建客户端特定声明
        Map<String, Object> clientClaims = buildClientClaims(context, clientId);

        // 添加到 Token
        clientClaims.forEach(claims::claim);

        log.debug("Enhanced client credentials token for client: {}", clientId);
    }

    /**
     * 构建客户端声明
     *
     * <p>子类可以重写此方法添加自定义声明。</p>
     *
     * @param context Token 上下文
     * @param clientId 客户端ID
     * @return 客户端声明映射
     */
    protected Map<String, Object> buildClientClaims(OAuth2TokenClaimsContext context, String clientId) {
        Map<String, Object> claims = new HashMap<>();

        // 基础客户端信息
        claims.put("client_id", clientId);
        claims.put("grant_type", AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
        claims.put("token_type", TOKEN_TYPE);

        // 时间信息
        Instant issuedAt = context.getClaims().build().getIssuedAt();
        if (issuedAt != null) {
            claims.put("issued_at", DATE_TIME_FORMATTER.format(issuedAt));
        }

        // 可以从配置读取环境标识
        String env = System.getProperty("spring.profiles.active", "default");
        claims.put("env", env);

        // 授权范围
        if (context.getAuthorizedScopes() != null && !context.getAuthorizedScopes().isEmpty()) {
            claims.put("client_scopes", String.join(" ", context.getAuthorizedScopes()));
        }

        return claims;
    }

    /**
     * 创建错误响应声明
     *
     * <p>用于在 Token 生成失败时携带错误信息（可选）。</p>
     *
     * @param errorCode 错误码
     * @param errorDescription 错误描述
     * @return 错误声明映射
     */
    protected Map<String, Object> buildErrorClaims(OAuth2ErrorCodesExpand errorCode, String errorDescription) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("error_code", errorCode.getErrorCode());
        claims.put("error_description", errorDescription);
        claims.put("token_type", "error");
        return claims;
    }

}
