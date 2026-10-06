package com.carlos.auth.security;

import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgTenant;
import com.carlos.org.api.pojo.ao.OrgTenantAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * <p>
 * 登录租户校验器
 * </p>
 *
 * <p>S2 租户管理：三个登录入口（REST 直登、/oauth2/token 自定义授权、OIDC formLogin）
 * 共用的租户校验逻辑。仅当 {@code carlos.auth.tenant.enabled=true} 时生效，
 * 否则一律返回 {@code null}（调用方保持原有非租户化行为）。</p>
 *
 * <p>抛出 {@link org.springframework.security.core.AuthenticationException} 子类，
 * 使各登录入口的现有异常处理路径（OAuth2 错误响应 / 登录失败处理）可以直接接管。</p>
 *
 * @author Carlos
 * @since 3.0.0
 * @date 2026年9月22日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantLoginChecker {

    /**
     * IdentityProviderRequest.additionalParameters 中租户ID的键
     */
    public static final String PARAM_TENANT_ID = "tenantId";

    /**
     * 登录请求/Token 请求中租户编码的键（/oauth2/token 扩展参数名）
     */
    public static final String PARAM_TENANT_CODE = "tenant_code";

    /**
     * 租户状态：停用（与 carlos-org OrgTenantStateEnum.DISABLE 对齐）
     */
    private static final Integer TENANT_STATUS_DISABLE = 2;

    private final TenantProperties tenantProperties;
    private final ApiOrgTenant apiOrgTenant;

    /**
     * 解析并校验租户
     *
     * @param tenantCode 登录请求携带的租户编码（可空）
     * @return 租户ID；功能未启用时返回 null（调用方保持现状行为），
     * 启用但未提供编码时返回默认租户ID（平台租户）
     * @throws BadCredentialsException 租户不存在
     * @throws DisabledException       租户已停用或已过期
     */
    public Long resolveTenantId(String tenantCode) {
        if (!tenantProperties.isEnabled()) {
            return null;
        }
        if (!StringUtils.hasText(tenantCode)) {
            return tenantProperties.getDefaultTenantId();
        }

        Result<OrgTenantAO> result;
        try {
            result = apiOrgTenant.getByCode(tenantCode.trim());
        } catch (Exception e) {
            log.error("租户查询服务调用失败: tenantCode={}, error={}", tenantCode, e.getMessage());
            throw new BadCredentialsException("租户校验服务暂不可用，请稍后再试");
        }

        if (result == null || !result.isSuccess() || result.getData() == null) {
            log.warn("登录租户不存在: tenantCode={}", tenantCode);
            throw new BadCredentialsException("租户不存在");
        }

        OrgTenantAO tenant = result.getData();
        if (TENANT_STATUS_DISABLE.equals(tenant.getTenantStatus())) {
            log.warn("登录租户已停用: tenantCode={}", tenantCode);
            throw new DisabledException("租户已停用，请联系管理员");
        }
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
            log.warn("登录租户已过期: tenantCode={}, expireTime={}", tenantCode, tenant.getExpireTime());
            throw new DisabledException("租户已过期，请联系管理员");
        }
        return tenant.getId();
    }

}
