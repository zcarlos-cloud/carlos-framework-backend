package com.carlos.security.permission;

/**
 * <p>
 * 可做数据权限判定的资源标记接口
 * </p>
 *
 * <p>业务实体实现本接口后，即可直接在 {@code @PreAuthorize} 里对**单条数据**做行级鉴权：</p>
 * <pre>
 * &#64;PreAuthorize("hasPermission(#order, 'update')")
 * public void update(Order order) { ... }
 *
 * &#64;PreAuthorize("@ps.canAccess(#order)")
 * public OrderVO detail(Order order) { ... }
 * </pre>
 *
 * <p>三个字段都是可选的：只关心「本人」维度就实现 {@link #getOwnerId()}，
 * 只关心部门维度就实现 {@link #getDeptId()}，不需要租户隔离则无需实现 {@link #getTenantId()}。
 * 返回 null 表示该维度不参与判定。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
public interface ScopedResource {

    /**
     * 数据归属人 ID（「仅本人」维度）。
     *
     * @return 归属人 ID；null 表示该维度不参与判定
     */
    default Long getOwnerId() {
        return null;
    }

    /**
     * 数据归属部门 ID（「本部门 / 本部门及子部门」维度）。
     *
     * @return 归属部门 ID；null 表示该维度不参与判定
     */
    default Long getDeptId() {
        return null;
    }

    /**
     * 数据归属租户 ID（租户隔离维度）。
     *
     * @return 归属租户 ID；null 表示该维度不参与判定
     */
    default Long getTenantId() {
        return null;
    }
}
