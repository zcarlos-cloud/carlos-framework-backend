package com.carlos.security.config;

import com.carlos.security.auth.UserContext;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * 资源服务器配置属性
 * </p>
 *
 * <h3>配置示例：</h3>
 * <pre>{@code
 * carlos:
 *   resource-server:
 *     enabled: true
 *     auth:
 *       user-id-header: X-User-Id
 *       username-header: X-User-Name
 *       roles-header: X-User-Roles
 *       whitelist-paths:
 *         - /actuator/**
 *         - /public/**
 *         - /swagger-ui/**
 *     permission:
 *       cache:
 *         enabled: true
 *         local-ttl: 600
 *         max-size: 10000
 *       redis-ttl: 604800
 * }</pre>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 * @see HttpHeadersConstant
 */
@Data
@ConfigurationProperties(prefix = "carlos.resource-server")
public class ResourceServerProperties {

    /**
     * 是否启用资源服务器功能
     */
    private boolean enabled = true;

    /**
     * 是否启用方法级安全注解（@PreAuthorize/@PostAuthorize/@Secured/@RolesAllowed）
     */
    private boolean methodSecurityEnabled = true;

    /**
     * 认证相关配置
     */
    private AuthProperties auth = new AuthProperties();

    /**
     * 权限缓存配置
     */
    private PermissionProperties permission = new PermissionProperties();

    /**
     * 认证配置
     */
    @Data
    public static class AuthProperties {

        /**
         * 白名单路径（不需要认证）
         */
        private Set<String> whitelistPaths = new HashSet<>() {{
            add("/actuator/**");
            add("/swagger-ui/**");
            add("/v3/api-docs/**");
            add("/doc.html");
            add("/webjars/**");
            add("/error");
        }};

        /**
         * 是否启用严格模式
         * <p>启用后，身份断言存在但验签失败时直接返回 401，不再静默降级为匿名。</p>
         */
        private boolean strictHeaderAuth = false;

        /**
         * 超级管理员角色编码
         * <p>供 {@code UserContext.isSuperAdmin()} 判定使用。取值为角色<b>编码</b>（org_role.role_code），
         * 与 X-User-Context 断言里的角色标识、PermissionProvider 缓存 key 保持同一体系。</p>
         */
        private String superRoleCode = UserContext.DEFAULT_SUPER_ROLE_CODE;
    }

    /**
     * 权限配置
     */
    @Data
    public static class PermissionProperties {

        /**
         * 本地缓存配置
         */
        private CacheProperties cache = new CacheProperties();

        /**
         * Redis 缓存过期时间（秒，默认7天）
         */
        private long redisTtl = 7 * 24 * 60 * 60;

        /**
         * 缓存配置
         */
        @Data
        public static class CacheProperties {

            /**
             * 是否启用本地缓存
             */
            private boolean enabled = true;

            /**
             * 本地缓存过期时间（秒，默认10分钟）
             */
            private long localTtl = 10 * 60;

            /**
             * 本地缓存最大条目数
             */
            private int maxSize = 10000;
        }
    }
}
