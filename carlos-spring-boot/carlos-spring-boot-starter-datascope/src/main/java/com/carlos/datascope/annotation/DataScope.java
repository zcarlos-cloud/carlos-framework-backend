package com.carlos.datascope.annotation;

import com.carlos.datascope.core.model.ScopeDimension;

import java.lang.annotation.*;

/**
 * 数据权限注解 V2 - 简化配置，智能推断
 * <p>
 * 使用示例：
 * <pre>
 * // 本部门及子部门数据权限
 * &#64;DataScope(dimension = ScopeDimension.DEPT_AND_CHILDREN)
 * public List&lt;User&gt; listUsers() { ... }
 *
 * // 仅本人数据权限
 * &#64;DataScope(dimension = ScopeDimension.CURRENT_USER)
 * public List&lt;Order&gt; listMyOrders() { ... }
 *
 * // 使用SpEL表达式自定义条件
 * &#64;DataScope(condition = "@ds.isOwner(#id) or @ds.hasRole('admin')")
 * public Order getOrder(Long id) { ... }
 * </pre>
 *
 * @author Carlos
 * @version 2.0
 */
@Repeatable(DataScopes.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DataScope {

    /**
     * 权限维度，默认自动推断
     * <p>
     * 当设置为 AUTO 时，系统会按照以下优先级自动选择：
     * 1. DEPT_AND_CHILDREN (如果用户有所属部门)
     * 2. CURRENT_USER (如果用户已登录)
     * 3. NONE (拒绝访问)
     */
    ScopeDimension dimension() default ScopeDimension.AUTO;

    /**
     * 权限控制字段，默认智能推断
     * <p>
     * 根据 dimension 自动推断字段名：
     * <ul>
     *   <li>CURRENT_USER -&gt; create_by / user_id</li>
     *   <li>CURRENT_DEPT -&gt; dept_id / department_id</li>
     *   <li>CURRENT_ROLE -&gt; role_id</li>
     *   <li>CURRENT_REGION -&gt; region_code / area_code</li>
     * </ul>
     */
    String field() default "";

    /**
     * 适用的表名，默认所有表
     * <p>
     * 支持通配符 *，例如：sys_* 匹配所有 sys_ 开头的表
     */
    String[] tables() default {};

    /**
     * 排除的表名
     * <p>
     * 优先级高于 tables，被排除的表不会应用数据权限
     */
    String[] excludeTables() default {};

    /**
     * 自定义规则表达式 (SpEL)
     * <p>
     * 示例：
     * <pre>
     * "@ds.isOwner(#entity)"                      // 检查是否所有者
     * "@ds.hasRole('admin')"                      // 检查是否有角色
     * "@ds.hasAnyRole('admin', 'manager')"        // 检查是否有任一角色
     * "#userId == authentication.principal.id"    // 使用SpEL变量
     * </pre>
     */
    String condition() default "";

    /**
     * 优先级，数字越小优先级越高
     * <p>
     * 当存在多个规则时，按优先级顺序执行
     */
    int priority() default 100;

    /**
     * 是否启用
     */
    boolean enabled() default true;

    /**
     * 备注说明
     */
    String remark() default "";
}
