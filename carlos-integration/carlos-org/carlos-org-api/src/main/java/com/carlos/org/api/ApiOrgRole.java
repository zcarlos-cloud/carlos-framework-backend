package com.carlos.org.api;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.org.ServiceNameConstant;
import com.carlos.org.api.fallback.ApiOrgRoleFallbackFactory;
import com.carlos.org.api.pojo.ao.OrgRoleAO;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 系统角色 feign 提供接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "role", path = "/api/org/role", fallbackFactory = ApiOrgRoleFallbackFactory.class)
public interface ApiOrgRole {

    /**
     * 根据ID查询角色
     *
     * @param id 角色ID
     * @return 角色信息
     */
    @GetMapping("/getById")
    Result<OrgRoleAO> getRoleById(@RequestParam("id") Long id);

    /**
     * 根据编码查询角色
     *
     * @param roleCode 角色编码
     * @return 角色信息
     */
    @GetMapping("/getByCode")
    Result<OrgRoleAO> getRoleByCode(@RequestParam("roleCode") String roleCode);

    /**
     * 根据角色ID查询持有该角色的用户
     *
     * <p>仅返回生效授权（{@code org_user_role.expire_time} 为空或晚于当前时间，见 ADR-002），
     * 已过期的临时授权不计入；部门内生效的授权（dept_id 非空）同样计入。</p>
     *
     * <p>供不依赖 carlos-org 的模块（如 carlos-message、工作流）按角色解析候选人员，
     * 解析完成后再调用自身逻辑，避免反向依赖。</p>
     *
     * @param roleId 角色ID
     * @return 用户列表
     */
    @GetMapping("/listUsersByRoleId")
    Result<List<OrgUserAO>> listUsersByRoleId(@RequestParam("roleId") Long roleId);

    /**
     * 根据角色ID查询指定租户内持有该角色的用户
     *
     * <p>语义与 {@link #listUsersByRoleId} 完全一致（仅生效授权：{@code expire_time} 为空或
     * 晚于当前时间；不做 {@code dept_id} 作用域过滤；按 {@code user_id} 去重），
     * 只是把租户条件由「隐式取当前登录上下文」改为「显式传入」。</p>
     *
     * <p><b>什么时候用本变体，而不是 {@link #listUsersByRoleId}</b>：
     * {@link #listUsersByRoleId} 的租户条件来自 {@code OrgTenantLineHandler}
     * （读 {@code SecurityUtils.getCurrentUser().getTenantId()}），而框架的
     * {@code FeignRequestInterceptor} 只从**当前 {@code HttpServletRequest}**
     * 透传 {@code X-Tenant-Id} / {@code X-User-Id}。用户请求链路天然正确；
     * 后台任务 / 无请求上下文的链路（如 carlos-job 调度线程的「超时升级上级」）
     * 无法透传，租户条件会静默回落到 {@code carlos.org.tenant.platform-tenant-id}
     * （平台租户 0），在多租户部署下返回**错误租户的用户**。</p>
     *
     * <p>本变体绕过租户拦截器后显式按 {@code tenant_id} 过滤，
     * <b>专供无请求上下文的调用方（后台任务、系统间调用）使用</b>，请勿删除。</p>
     *
     * @param roleId   角色ID
     * @param tenantId 租户ID（必填；为 null 时返回空列表，不查库）
     * @return 用户列表
     */
    @GetMapping("/listUsersByRoleIdAndTenant")
    Result<List<OrgUserAO>> listUsersByRoleIdAndTenant(@RequestParam("roleId") Long roleId,
                                                       @RequestParam("tenantId") Long tenantId);

    /**
     * 分页查询角色选择项
     *
     * <p>供不依赖 carlos-org 的模块（如工作流的办理人选择器）按关键字挑选角色，
     * 条件语义与 {@code OrgRoleManager#getPage} 一致：名称、编码都按<b>模糊</b>匹配，
     * 条件之间是 AND，两参数都可为空（为空即不过滤）。
     * 数据权限范围由 carlos-org 侧按当前登录上下文施加。</p>
     *
     * @param roleName 角色名称（模糊，可空）
     * @param roleCode 角色唯一编码（模糊，可空）
     * @param pageNum  页码，从 1 开始（小于 1 时按 1 处理）
     * @param pageSize 每页条数（小于 1 时按 10 处理）
     * @return 分页后的角色列表
     */
    @GetMapping("/pageRoleOptions")
    Result<Paging<OrgRoleAO>> pageRoleOptions(@RequestParam(value = "roleName", required = false) String roleName,
                                              @RequestParam(value = "roleCode", required = false) String roleCode,
                                              @RequestParam("pageNum") Integer pageNum,
                                              @RequestParam("pageSize") Integer pageSize);

    /**
     * 按主键批量查询角色
     *
     * <p>供「已选中的角色回显名称」这类场景批量取数，避免逐个调用（N+1）。
     * 空集合、不存在的 id 均不报错，返回结果只包含实际存在的角色；
     * <b>不保证入参顺序</b>，调用方按 id 自行建映射。</p>
     *
     * @param ids 角色ID集合（建议单次不超过 500 个，避免 GET 查询串过长）
     * @return 角色列表
     */
    @GetMapping("/listRoleOptions")
    Result<List<OrgRoleAO>> listRoleOptions(@RequestParam("ids") Collection<Long> ids);

}
