package com.carlos.org.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.carlos.security.auth.UserContext;
import com.carlos.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * <p>
 * 多租户拦截处理器
 * </p>
 *
 * <p>隔离表清单与平台租户ID从 {@link OrgProperties.OrgTenantProperties} 读取
 * （{@code carlos.org.tenant.tables} / {@code platform-tenant-id}），避免每加一张租户表都改代码。</p>
 *
 * <p>规则:只拦截真实存在 tenant_id 列的表。
 * 向无该列的表(如 org_user_department)拼接 tenant_id 条件会导致 SQL 报错;
 * 遗漏有该列的表(如职位体系四张表)会导致跨租户数据泄露。</p>
 *
 * @author Carlos
 * @date 2026年5月19日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrgTenantLineHandler implements TenantLineHandler {

    private final OrgProperties orgProperties;

    @Override
    public Expression getTenantId() {
        Long tenantId = getCurrentTenantId();
        if (tenantId == null) {
            tenantId = orgProperties.getTenant().getPlatformTenantId();
        }
        return new LongValue(tenantId);
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        Set<String> tables = orgProperties.getTenant().getTables();
        if (!tables.contains(tableName)) {
            return true;
        }
        // 超管跨租户放行：配置开启且当前用户为超管时，不追加 tenant_id 条件。
        // 注意：租户插件对 INSERT 也先判 ignoreTable，放行期间超管的写入不会自动填 tenant_id
        // （实体未显式赋值则为 NULL），写入路径须显式回填，见 OrgProperties.tenant.superAdminIgnore
        return Boolean.TRUE.equals(orgProperties.getTenant().getSuperAdminIgnore()) && isSuperAdmin();
    }

    /**
     * 获取当前租户ID
     *
     * @return 租户ID
     */
    private Long getCurrentTenantId() {
        try {
            UserContext userContext = SecurityUtils.getCurrentUser();
            if (userContext != null) {
                return userContext.getTenantId();
            }
        } catch (Exception e) {
            log.debug("无法获取当前租户ID: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 当前用户是否超级管理员
     *
     * @return true 如果当前登录用户是超级管理员
     */
    private boolean isSuperAdmin() {
        try {
            UserContext userContext = SecurityUtils.getCurrentUser();
            return userContext != null && userContext.isSuperAdmin();
        } catch (Exception e) {
            log.debug("无法判断当前用户是否超管: {}", e.getMessage());
            return false;
        }
    }
}
