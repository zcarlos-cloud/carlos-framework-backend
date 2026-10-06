package com.carlos.security.permission;

/**
 * <p>
 * 数据权限范围提供者（数据维度的鉴权 SPI）
 * </p>
 *
 * <p>{@link PermissionChecker} 的「数据维度」方法（{@code canAccessData} / {@code hasScope}
 * / {@code isSelf}）依赖本接口。数据范围（全部 / 本部门及子部门 / 仅本部门 / 仅本人 / 自定义）
 * 是业务语义，规则数据存在业务模块（如 carlos-org 的 {@code org_role.data_scope}），
 * 因此接口定义在框架层、实现由业务模块提供。</p>
 *
 * <p>未提供实现时（如未引入 org 模块的轻量服务），{@link PermissionChecker} 的数据维度方法
 * 一律**保守拒绝**，避免因缺少规则而误放行。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
public interface DataScopeProvider {

    /**
     * 当前用户的有效数据范围编码。
     *
     * <p>约定：{@code 1=全部, 2=本部门及子部门, 3=仅本部门, 4=仅本人, 5=自定义}
     * （与 {@code OrgDataScopeEnum} 对齐），数值越小范围越宽。多个角色时取**最宽**的一个。</p>
     *
     * @return 数据范围编码；null 表示无法判定（调用方应保守拒绝）
     */
    Integer currentScopeCode();

    /**
     * 判断当前用户能否访问某条数据。
     *
     * @param ownerId   数据归属人 ID（仅本人维度用），可为 null
     * @param deptId    数据归属部门 ID（部门维度用），可为 null
     * @param tenantId  数据归属租户 ID（租户隔离用），可为 null
     * @return true 表示可访问
     */
    boolean canAccessData(Long ownerId, Long deptId, Long tenantId);
}
