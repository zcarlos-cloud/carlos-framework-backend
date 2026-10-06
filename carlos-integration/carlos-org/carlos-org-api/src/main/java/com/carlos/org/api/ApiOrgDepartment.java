package com.carlos.org.api;

import com.carlos.core.response.Result;
import com.carlos.org.ServiceNameConstant;
import com.carlos.org.api.fallback.ApiOrgDepartmentFallbackFactory;
import com.carlos.org.api.pojo.ao.OrgDepartmentAO;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 部门 feign 提供接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "department", path = "/api/org/department", fallbackFactory = ApiOrgDepartmentFallbackFactory.class)
public interface ApiOrgDepartment {

    /**
     * 根据ID查询部门
     *
     * @param id 部门ID
     * @return 部门信息
     */
    @GetMapping("/getById")
    Result<OrgDepartmentAO> getDepartmentById(@RequestParam("id") Long id);

    /**
     * 根据部门ID查询部门下的用户
     *
     * <p><b>语义：非递归</b>。只返回通过 {@code org_user_department} 直接归属该部门的用户，
     * <b>不包含子部门</b>的用户。需要子树人员时，请由调用方先解析部门层级，再逐级调用本接口
     * （与同 JVM 的 {@code OrgDepartmentManager#getUsersByDeptId} 语义保持一致）。</p>
     *
     * @param deptId 部门ID
     * @return 用户列表
     */
    @GetMapping("/listUsersByDeptId")
    Result<List<OrgUserAO>> listUsersByDeptId(@RequestParam("deptId") Long deptId);

    /**
     * 根据部门ID查询指定租户内该部门下的用户
     *
     * <p>语义与 {@link #listUsersByDeptId} 完全一致（**非递归**，只含直接归属该部门的用户，
     * 不含子部门），只是把租户条件由「隐式取当前登录上下文」改为「显式传入」。</p>
     *
     * <p><b>什么时候用本变体，而不是 {@link #listUsersByDeptId}</b>：
     * {@link #listUsersByDeptId} 的租户条件来自 {@code OrgTenantLineHandler}
     * （读 {@code SecurityUtils.getCurrentUser().getTenantId()}），而框架的
     * {@code FeignRequestInterceptor} 只从**当前 {@code HttpServletRequest}**
     * 透传 {@code X-Tenant-Id} / {@code X-User-Id}。用户请求链路天然正确；
     * 后台任务 / 无请求上下文的链路（如 carlos-job 调度线程的「超时升级上级」）
     * 无法透传，租户条件会静默回落到 {@code carlos.org.tenant.platform-tenant-id}
     * （平台租户 0），在多租户部署下返回**错误租户的用户**。</p>
     *
     * <p>本变体绕过租户拦截器后显式按 {@code org_user.tenant_id} 过滤
     * （{@code org_user_department} 表自身无 {@code tenant_id} 列），
     * <b>专供无请求上下文的调用方（后台任务、系统间调用）使用</b>，请勿删除。</p>
     *
     * @param deptId   部门ID
     * @param tenantId 租户ID（必填；为 null 时返回空列表，不查库）
     * @return 用户列表
     */
    @GetMapping("/listUsersByDeptIdAndTenant")
    Result<List<OrgUserAO>> listUsersByDeptIdAndTenant(@RequestParam("deptId") Long deptId,
                                                       @RequestParam("tenantId") Long tenantId);

    /**
     * 根据部门ID查询该部门<b>及其全部子孙部门</b>下的用户
     *
     * <p><b>语义：递归</b>。部门集合由服务端展开（{@code carlos-org} 侧的部门树能力，
     * 优先命中「全部子孙」缓存，缓存未预热时逐层下探数据库），再一次查出这些部门的用户，
     * <b>调用方只需一次远程往返</b>——不要为每个子部门各调一次 {@link #listUsersByDeptId}。
     * 与 {@link #listUsersByDeptId}（非递归）语义不同，两者不可互相替代。</p>
     *
     * <p>展开口径：命中的部门集合 = 传入部门自身 + 其全部子孙部门；
     * 用户按 {@code org_user_department.dept_id} 归属判定，按 {@code user_id} 去重
     * （一个人可能同时属于子树里的多个部门）。</p>
     *
     * @param deptId 部门ID（子树的根）
     * @return 该部门及其全部子孙部门下的用户列表，按 {@code user_id} 去重
     */
    @GetMapping("/listUsersByDeptIdRecursive")
    Result<List<OrgUserAO>> listUsersByDeptIdRecursive(@RequestParam("deptId") Long deptId);

    /**
     * 根据部门ID查询指定租户内该部门<b>及其全部子孙部门</b>下的用户
     *
     * <p>语义与 {@link #listUsersByDeptIdRecursive} 完全一致（递归展开部门集合、
     * 一次远程往返、按 {@code user_id} 去重），只是把租户条件由「隐式取当前登录上下文」
     * 改为「显式传入」。</p>
     *
     * <p><b>什么时候用本变体，而不是 {@link #listUsersByDeptIdRecursive}</b>：
     * {@link #listUsersByDeptIdRecursive} 的租户条件来自 {@code OrgTenantLineHandler}
     * （读 {@code SecurityUtils.getCurrentUser().getTenantId()}），而框架的
     * {@code FeignRequestInterceptor} 只从**当前 {@code HttpServletRequest}**
     * 透传 {@code X-Tenant-Id} / {@code X-User-Id}。用户请求链路天然正确；
     * 后台任务 / 无请求上下文的链路无法透传，租户条件会静默回落到
     * {@code carlos.org.tenant.platform-tenant-id}（平台租户 0），
     * 在多租户部署下返回**错误租户的用户**。</p>
     *
     * <p>本变体绕过租户拦截器后显式按 {@code org_user.tenant_id} 过滤
     * （{@code org_user_department} 表自身无 {@code tenant_id} 列），
     * <b>专供无请求上下文的调用方（后台任务、系统间调用）使用</b>，请勿删除。</p>
     *
     * @param deptId   部门ID（子树的根）
     * @param tenantId 租户ID（必填；为 null 时返回空列表，不查库）
     * @return 该部门及其全部子孙部门下的用户列表，按 {@code user_id} 去重
     */
    @GetMapping("/listUsersByDeptIdRecursiveAndTenant")
    Result<List<OrgUserAO>> listUsersByDeptIdRecursiveAndTenant(@RequestParam("deptId") Long deptId,
                                                                @RequestParam("tenantId") Long tenantId);

    /**
     * 查询全部部门（扁平列表，含父级 id）
     *
     * <p>供不依赖 carlos-org 的模块（如工作流的办理人选择器）一次取回部门清单，
     * 由调用方自行筛选、分页或建树——部门数量在组织架构量级内，一次往返比「按需翻页」
     * 更省，也避免调用方为每个关键字各发一次请求。
     * 排序沿用 {@code OrgDepartmentManager#listAll} 的排序（{@code order_num} 升序），
     * 数据权限范围由 carlos-org 侧按当前登录上下文施加。</p>
     *
     * @return 部门列表（扁平）
     */
    @GetMapping("/listAllDepartmentOptions")
    Result<List<OrgDepartmentAO>> listAllDepartmentOptions();

    /**
     * 按主键批量查询部门
     *
     * <p>供「已选中的部门回显名称」这类场景批量取数，避免逐个调用（N+1）。
     * 空集合、不存在的 id 均不报错，返回结果只包含实际存在的部门；
     * <b>不保证入参顺序</b>，调用方按 id 自行建映射。</p>
     *
     * @param ids 部门ID集合（建议单次不超过 500 个，避免 GET 查询串过长）
     * @return 部门列表
     */
    @GetMapping("/listDepartmentOptions")
    Result<List<OrgDepartmentAO>> listDepartmentOptions(@RequestParam("ids") Collection<Long> ids);

}
