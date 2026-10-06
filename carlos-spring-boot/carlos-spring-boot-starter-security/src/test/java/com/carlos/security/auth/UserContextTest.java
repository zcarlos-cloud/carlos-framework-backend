package com.carlos.security.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link UserContext} 权限匹配单元测试
 *
 * <p>覆盖「模块:功能:操作」三段式权限码与多级通配符（{@code org:*}、{@code org:user:*}）。</p>
 *
 * @author carlos
 */
class UserContextTest {

    private UserContext contextWith(String... permissions) {
        UserContext ctx = new UserContext();
        ctx.setPermissions(new java.util.HashSet<>(Set.of(permissions)));
        return ctx;
    }

    @Test
    @DisplayName("精确匹配三段式权限码")
    void should_match_exact_three_segment_permission() {
        UserContext ctx = contextWith("org:user:create");
        assertTrue(ctx.hasPermission("org:user:create"));
        assertFalse(ctx.hasPermission("org:user:delete"));
    }

    @Test
    @DisplayName("模块级通配符 org:* 应放行其下任意操作")
    void should_match_module_wildcard() {
        UserContext ctx = contextWith("org:*");
        assertTrue(ctx.hasPermission("org:user:create"));
        assertTrue(ctx.hasPermission("org:role:view"));
        assertFalse(ctx.hasPermission("system:user:create"));
    }

    @Test
    @DisplayName("功能级通配符 org:user:* 应放行该功能下任意操作")
    void should_match_feature_wildcard() {
        UserContext ctx = contextWith("org:user:*");
        assertTrue(ctx.hasPermission("org:user:create"));
        assertTrue(ctx.hasPermission("org:user:update"));
        assertFalse(ctx.hasPermission("org:role:create"));
    }

    @Test
    @DisplayName("全局通配符 * 放行一切")
    void should_match_global_wildcard() {
        UserContext ctx = contextWith("*");
        assertTrue(ctx.hasPermission("org:user:create"));
        assertTrue(ctx.hasPermission("anything:goes:here"));
    }

    @Test
    @DisplayName("兼容两段式旧权限码 user:* -> user:create")
    void should_still_support_two_segment_wildcard() {
        UserContext ctx = contextWith("user:*");
        assertTrue(ctx.hasPermission("user:create"));
        // user:* 是 user 下的通配，更深层级也放行
        assertTrue(ctx.hasPermission("user:create:extra"));
    }

    @Test
    @DisplayName("无权限时不放行")
    void should_deny_when_empty() {
        assertFalse(contextWith().hasPermission("org:user:create"));
    }
}
