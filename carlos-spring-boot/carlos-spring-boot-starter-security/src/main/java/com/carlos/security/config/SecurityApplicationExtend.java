package com.carlos.security.config;

import com.carlos.core.auth.UserContext;
import com.carlos.core.interfaces.ApplicationExtend;
import com.carlos.security.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.HashSet;

/**
 * 基于 Spring Security 上下文的默认 {@link ApplicationExtend} 实现
 *
 * <h2>为什么需要它</h2>
 * <p>
 * {@code WebRequestContext.getUserContext()} 走的是 {@link ApplicationExtend}，
 * 而该接口的默认实现<b>恒返回 null</b>。既有模块（carlos-org / carlos-system / carlos-message /
 * carlos-audit）各自注册了一份只覆写 {@code getUserId()} 的实现，于是
 * <b>任何依赖 {@code getUserContext()} 的能力都拿不到登录人</b> ——
 * 最直接的表现是 {@code @Log} 注解把审计日志记成 {@code principalType=ANONYMOUS}、操作人为空，
 * 审计失去意义。
 * </p>
 * <p>
 * 本类作为「安全模块能提供的那部分」的默认实现：只覆写 {@code getUserContext()}，
 * 其余方法沿用接口默认（返回 null）。carlos-job / carlos-workflow / carlos-agent
 * 原本连 {@code ApplicationExtend} 都没有，现在无需各自新增类即可获得身份。
 * </p>
 *
 * <h2>生效范围与优先级</h2>
 * <p>
 * 由 {@code ResourceServerAutoConfiguration} 以 {@code @ConditionalOnMissingBean} 注册，
 * 即：<b>已自带实现的模块（如 carlos-system）不受影响，它自己的实现优先</b>
 * （组件扫描先于自动配置）。仅当容器里没有任何 {@link ApplicationExtend} 时才兜底。
 * </p>
 *
 * <h2>前提</h2>
 * <p>
 * 身份来自 {@code SecurityContextHolder}，而它由 {@code UserContextAuthenticationFilter} 填充。
 * 该过滤器只在 {@code carlos.resource-server.enabled=true} 时注册 —— 开关关着时本类
 * 仍然存在，但 {@code SecurityUtils.getCurrentUser()} 恒为 null，{@code getUserContext()}
 * 返回 null，调用方（含 {@code LogAspect}）会退化为匿名。这是刻意的：宁可匿名，不要抛异常。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
public class SecurityApplicationExtend implements ApplicationExtend {

    /**
     * 把安全上下文里的用户信息转成 core 层的 {@link UserContext}
     *
     * <p>各服务自带的 {@code ApplicationExtend} 实现（carlos-system / carlos-org / carlos-message /
     * carlos-audit）都只需覆写 {@code getUserContext()}，实现体与本类完全一致。
     * 这里提供一个静态入口供它们委托，避免同一段映射散在五处 —— 否则将来「身份里该带哪些字段」
     * 变化时，漏改其中一两处不会有任何编译或运行错误，只会让少数服务的审计行缺字段。
     * </p>
     *
     * @param securityUser 安全上下文中的用户；为 null 时返回 null
     * @return core 层用户上下文；{@code securityUser} 为 null 时返回 null
     */
    public static UserContext fromSecurityUser(com.carlos.security.auth.UserContext securityUser) {
        if (securityUser == null) {
            return null;
        }

        UserContext coreUser = new UserContext();
        coreUser.setUserId(securityUser.getUserId());
        coreUser.setAccount(securityUser.getUsername());
        coreUser.setTenantId(securityUser.getTenantId());
        coreUser.setDepartmentId(securityUser.getDeptId());
        // 注意语义：值来自身份断言里的角色编码（org_role.role_code），
        // 而 core UserContext.roleIds 声明为 Set<Serializable>（框架刻意不表态形态），此处原样透传。
        coreUser.setRoleIds(new HashSet<Serializable>(securityUser.getRoleCodes()));
        return coreUser;
    }

    @Override
    public UserContext getUserContext() {
        return fromSecurityUser(SecurityUtils.getCurrentUser());
    }
}
