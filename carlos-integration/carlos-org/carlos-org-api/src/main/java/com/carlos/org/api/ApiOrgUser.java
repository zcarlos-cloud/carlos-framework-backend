package com.carlos.org.api;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.org.ServiceNameConstant;
import com.carlos.org.api.fallback.ApiOrgUserFallbackFactory;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.api.pojo.ao.OrgUserCandidateAO;
import com.carlos.org.api.pojo.param.ApiOrgUserChangeStateParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 系统用户 feign 提供接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "user", path = "/api/org/user", fallbackFactory = ApiOrgUserFallbackFactory.class)
public interface ApiOrgUser {

    /**
     * 根据标识查询用户（支持用户名、手机号、邮箱）
     *
     * @param identifier 用户标识
     * @return 用户信息
     */
    @GetMapping("/user/by-identifier")
    Result<OrgUserAO> getUserByIdentifier(@RequestParam("identifier") String identifier);

    /**
     * 根据标识查询指定租户内的用户（支持用户名、手机号）
     *
     * <p>供 carlos-auth 登录流程在多租户模式下按租户定位用户；
     * 与 {@link #getUserByIdentifier} 的区别是显式按 tenant_id 过滤。</p>
     *
     * @param identifier 用户标识
     * @param tenantId   租户ID
     * @return 用户信息
     */
    @GetMapping("/user/by-identifier-and-tenant")
    Result<OrgUserAO> getUserByIdentifierAndTenant(@RequestParam("identifier") String identifier,
                                                   @RequestParam("tenantId") Long tenantId);

    /**
     * 根据用户ID查询用户
     *
     * <p>用户不存在（含已逻辑删除）时返回 {@code data = null}，不报错。
     * 供 carlos-auth 等调用方按 ID 定位用户，替代原先只能按标识查询的限制。</p>
     *
     * @param id 用户ID（必填，缺失返回 400）
     * @return 用户信息
     */
    @GetMapping("/user/by-id")
    Result<OrgUserAO> getUserById(@RequestParam("id") Long id);

    /**
     * 根据用户ID查询指定租户内的用户
     *
     * <p><b>什么时候用本变体，而不是 {@link #getUserById}</b>：
     * {@link #getUserById} 的租户条件来自 {@code OrgTenantLineHandler}，它从
     * {@code SecurityUtils.getCurrentUser().getTenantId()} 取当前租户；而框架的
     * {@code FeignRequestInterceptor} 只会从**当前 {@code HttpServletRequest}** 透传
     * {@code X-Tenant-Id} / {@code X-User-Id}。所以：</p>
     * <ul>
     *   <li>**用户请求链路**（浏览器 → 网关 → 服务）天然带上租户，用 {@link #getUserById} 即可；</li>
     *   <li>**后台任务 / 无请求上下文的链路**（例如 carlos-job 调度线程执行「超时升级上级」，
     *       线程内没有 {@code HttpServletRequest}）什么都不会透传，租户条件会静默回落到
     *       配置的 {@code carlos.org.tenant.platform-tenant-id}（平台租户 0），
     *       在多租户部署下会解析出**错误的用户**，且不报错。</li>
     * </ul>
     *
     * <p>本变体显式接收 tenantId，绕过租户拦截器后按 {@code tenant_id} 过滤，
     * <b>专供无请求上下文的调用方（后台任务、系统间调用）使用</b>。
     * 请勿因为它「看起来和 getUserById 重复」而删除。</p>
     *
     * @param id       用户ID
     * @param tenantId 租户ID（必填；为 null 时返回 {@code data = null}，不查库）
     * @return 用户信息
     */
    @GetMapping("/user/by-id-and-tenant")
    Result<OrgUserAO> getUserByIdAndTenant(@RequestParam("id") Long id,
                                           @RequestParam("tenantId") Long tenantId);

    /**
     * 根据用户ID集合批量查询用户
     *
     * <p>供待办列表、消息路由等场景批量解析办理人，避免逐个调用（N+1）。
     * id 集合以重复查询参数传递（{@code ?ids=1&ids=2}），也支持 {@code ?ids=1,2}；
     * 空集合、不存在的 id 均不报错，返回结果只包含实际存在的用户。</p>
     *
     * <p>调用方无需在批量解析前判空：Feign 在集合为空时发送 {@code ?ids}（无值），
     * 服务端按空集合处理并返回空数组，不产生数据库查询。</p>
     *
     * @param ids 用户ID集合（建议单次不超过 500 个，避免 GET 查询串过长）
     * @return 用户信息列表
     */
    @GetMapping("/user/list-by-ids")
    Result<List<OrgUserAO>> listUsersByIds(@RequestParam("ids") Collection<Long> ids);

    /**
     * 根据用户ID集合批量查询指定租户内的用户
     *
     * <p>语义与 {@link #listUsersByIds} 完全一致（空集合不查库、不存在的 id 静默忽略、
     * 结果只含实际存在的用户），只是把租户条件由「隐式取当前登录上下文」改为「显式传入」。</p>
     *
     * <p><b>什么时候用本变体，而不是 {@link #listUsersByIds}</b>：
     * {@link #listUsersByIds} 的租户条件来自 {@code OrgTenantLineHandler}
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
     * @param ids      用户ID集合（建议单次不超过 500 个，避免 GET 查询串过长）
     * @param tenantId 租户ID（必填；为 null 时返回空列表，不查库）
     * @return 用户信息列表
     */
    @GetMapping("/user/list-by-ids-and-tenant")
    Result<List<OrgUserAO>> listUsersByIdsAndTenant(@RequestParam("ids") Collection<Long> ids,
                                                    @RequestParam("tenantId") Long tenantId);

    /**
     * 分页查询候选用户（选人场景：待办转办、加签、减签、审批人指派等）
     *
     * <p>过滤口径：</p>
     * <ul>
     *   <li>{@code includeIds} 非空时只在这些用户里挑（例如「减签只能选当前已有的办理人」）；</li>
     *   <li>{@code excludeIds} 非空时排除这些用户（例如「加签只能选还没参与的人」）；</li>
     *   <li>{@code deptId} 非空时按部门过滤，<b>含该部门的全部子孙部门</b>
     *       （与 {@code ruoyi} 系实现中 {@code find_in_set(deptId, ancestors)} 的口径一致）；</li>
     *   <li>始终过滤掉已禁用的用户账号。</li>
     * </ul>
     *
     * <p>返回的部门名称由服务端一次补齐，<b>调用方不要按部门逐个回查</b>（N+1）。</p>
     *
     * @param deptId     部门id，可选；含子孙部门
     * @param includeIds 限定用户id集合，可选；<b>给出「空集合」表示无可选人</b>（返回空分页，不查库），
     *                   不限定请直接不传
     * @param excludeIds 排除用户id集合，可选
     * @param current    页码，从 1 开始，小于 1 时按 1 处理
     * @param size       页大小，小于 1 时按 10 处理
     * @return 候选用户分页结果
     */
    @GetMapping("/user/candidate/page")
    Result<Paging<OrgUserCandidateAO>> pageCandidates(@RequestParam(value = "deptId", required = false) Long deptId,
                                                      @RequestParam(value = "includeIds", required = false) Collection<Long> includeIds,
                                                      @RequestParam(value = "excludeIds", required = false) Collection<Long> excludeIds,
                                                      @RequestParam("current") int current,
                                                      @RequestParam("size") int size);

    /**
     * 分页查询用户选择项（按账号/昵称关键字挑选用户）
     *
     * <p>与 {@link #pageCandidates} 的区别是<b>检索方式</b>：本端点按关键字模糊匹配，
     * 供「输入姓名找人的选择器」使用；{@code pageCandidates} 按部门/包含/排除集合取候选，
     * 两者互补，不互相替代。</p>
     *
     * <p>过滤口径与 {@code OrgUserManager#getPage} 一致：账号、昵称都按<b>模糊</b>匹配，
     * 条件之间是 AND，两参数都可为空（为空即不过滤）。数据权限范围由 carlos-org 侧
     * 按当前登录上下文施加。返回结果<b>不含密码哈希</b>（映射自 VO，不是 DTO）。</p>
     *
     * @param account  用户名（模糊，可空）
     * @param nickname 昵称（模糊，可空）
     * @param pageNum  页码，从 1 开始（小于 1 时按 1 处理）
     * @param pageSize 每页条数（小于 1 时按 10 处理）
     * @return 分页后的用户列表
     */
    @GetMapping("/pageUserOptions")
    Result<Paging<OrgUserAO>> pageUserOptions(@RequestParam(value = "account", required = false) String account,
                                              @RequestParam(value = "nickname", required = false) String nickname,
                                              @RequestParam("pageNum") Integer pageNum,
                                              @RequestParam("pageSize") Integer pageSize);

    /**
     * 变更用户状态（启用/禁用/锁定）
     *
     * <p>供 carlos-auth 等内部服务调用，同步账号锁定/解锁状态。</p>
     *
     * @param param 状态变更参数
     */
    @PostMapping("/user/changeState")
    void changeState(@RequestBody ApiOrgUserChangeStateParam param);

    /**
     * 更新用户密码（内部接口 - 密码迁移专用）
     *
     * <p>供 carlos-auth 密码迁移使用：登录时将 SM4 旧密码升级为 Argon2id。
     * 传入的密码已经是编码后的哈希值（带前缀如 {@code {argon2}$argon2id$...}），
     * carlos-org 直接覆盖存储，不做二次编码。</p>
     *
     * @param userId          用户 ID
     * @param encodedPassword 编码后的新密码哈希
     * @return 操作结果
     */
    @PostMapping("/user/updatePassword")
    Result<Void> updatePassword(@RequestParam("userId") Long userId, @RequestParam("encodedPassword") String encodedPassword);

}
