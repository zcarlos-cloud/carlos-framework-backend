package com.carlos.auth.oauth2.web;

import com.carlos.auth.api.enums.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OAuth2 授权确认页控制器
 *
 * <p>提供授权码模式下的 Scope 授权确认页面，用户在登录后、授权前
 * 可以看到客户端请求的权限范围并选择同意或拒绝。</p>
 *
 * <p><strong>流程说明：</strong></p>
 * <ol>
 *   <li>用户完成登录后，Spring Authorization Server 检测到客户端需要用户同意 Scope</li>
 *   <li>重定向到 {@code /oauth2/consent?client_id=xxx&state=xxx&scope=xxx}</li>
 *   <li>本控制器解析参数，查询客户端信息，准备 Scope 描述，渲染确认页面</li>
 *   <li>用户点击"同意授权"或"拒绝"，表单 POST 回 {@code /oauth2/authorize}</li>
 *   <li>Spring Authorization Server 的 {@code OAuth2AuthorizationEndpointFilter} 处理 consent 提交</li>
 * </ol>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-05-19
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ConsentController {

    /**
     * Scope 中文描述映射
     */
    private static final Map<String, String> SCOPE_DESCRIPTIONS;

    static {
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("openid", "获取您的基本身份标识（用户唯一标识）");
        descriptions.put("profile", "获取您的个人资料信息（昵称、头像等）");
        descriptions.put("email", "获取您的邮箱地址");
        descriptions.put("phone", "获取您的手机号码");
        descriptions.put("address", "获取您的地址信息");
        SCOPE_DESCRIPTIONS = Collections.unmodifiableMap(descriptions);
    }

    private final RegisteredClientRepository registeredClientRepository;

    /**
     * 渲染授权确认页面
     *
     * @param clientId 客户端 ID
     * @param scope    请求的范围（空格分隔）
     * @param state    OAuth2 状态参数
     * @param model    Spring MVC 模型
     * @return 授权确认视图名
     */
    /**
     * 客户端 ID 最大长度
     */
    private static final int MAX_CLIENT_ID_LENGTH = 64;

    @GetMapping("/oauth2/consent")
    public String consent(
        @RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
        @RequestParam(OAuth2ParameterNames.SCOPE) String scope,
        @RequestParam(OAuth2ParameterNames.STATE) String state,
        Model model) {

        // 校验 clientId 长度，防止日志截断或异常输入
        if (clientId.length() > MAX_CLIENT_ID_LENGTH) {
            throw AuthErrorCode.AUTH_PARAM_INVALID.exception("客户端ID长度超过限制");
        }

        log.debug("Consent page requested: clientId={}, scope={}", clientId, scope);

        // 查询客户端信息
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            log.error("Consent page error: client not found, clientId={}", clientId);
            throw AuthErrorCode.AUTH_CLIENT_NOT_FOUND.exception("客户端不存在: %s", clientId);
        }

        String clientName = StringUtils.hasText(registeredClient.getClientName())
            ? registeredClient.getClientName()
            : clientId;

        // 解析 scope 列表，并与客户端注册范围取交集（防止注入未注册 scope）
        Set<String> requestedScopes = parseScopes(scope);
        Set<String> allowedScopes = registeredClient.getScopes();
        Set<String> scopes = requestedScopes.stream()
            .filter(allowedScopes::contains)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        // 构建 scope 描述
        Map<String, String> scopeDescriptions = buildScopeDescriptions(scopes);

        model.addAttribute("clientId", clientId);
        model.addAttribute("clientName", clientName);
        model.addAttribute("state", state);
        model.addAttribute("scopes", scopes);
        model.addAttribute("scopeDescriptions", scopeDescriptions);

        log.info("Consent page rendered: clientName={}, scopeCount={}", clientName, scopes.size());
        return "consent";
    }

    /**
     * 解析 scope 字符串为有序集合
     *
     * @param scope 空格分隔的 scope 字符串
     * @return Scope 集合
     */
    private Set<String> parseScopes(String scope) {
        if (!StringUtils.hasText(scope)) {
            return Collections.emptySet();
        }
        Set<String> scopes = new LinkedHashSet<>();
        for (String s : scope.split(" ")) {
            String trimmed = s.trim();
            if (StringUtils.hasText(trimmed)) {
                scopes.add(trimmed);
            }
        }
        return scopes;
    }

    /**
     * 构建 Scope 描述映射
     *
     * @param scopes Scope 集合
     * @return Scope -> 描述的映射
     */
    private Map<String, String> buildScopeDescriptions(Set<String> scopes) {
        Map<String, String> descriptions = new HashMap<>();
        for (String scope : scopes) {
            String desc = SCOPE_DESCRIPTIONS.getOrDefault(scope, "访问 " + scope + " 相关资源");
            descriptions.put(scope, desc);
        }
        return descriptions;
    }
}
