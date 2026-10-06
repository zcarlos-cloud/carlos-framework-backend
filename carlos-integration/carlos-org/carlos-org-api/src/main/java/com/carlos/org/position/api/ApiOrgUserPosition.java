package com.carlos.org.position.api;

import com.carlos.core.response.Result;
import com.carlos.org.ServiceNameConstant;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.position.api.fallback.ApiOrgUserPositionFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * <p>
 * 用户岗位职级关联表（核心任职信息） feign 提供接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "userPosition", path = "/api/org/user/position", fallbackFactory = ApiOrgUserPositionFallbackFactory.class)
public interface ApiOrgUserPosition {

    /**
     * 根据用户ID查询其直属下级（直接汇报人）
     *
     * <p>数据源为 {@code org_user_position.report_to}（汇报对象/直接上级），
     * 返回以 userId 为直接上级的任职人员，按 user_id 去重。支撑「取某人的下属」这类
     * 由上级向下级路由的场景（如代下级办理、下级通知）。</p>
     *
     * <p><b>方向自下而上无效</b>：本端点查的是 {@code report_to = userId}，拿到的是
     * <b>下级</b>。「某人的直属上级」要读<b>他自己</b>那条任职记录的 {@code report_to}，
     * 请用 {@link #getSuperiorByUserId}。两者方向相反，不可互相替代。</p>
     *
     * <p><b>仅直接下级，不递归</b>：不展开下级的下级，逐层追需要由调用方循环调用。
     * 不做任职状态过滤，已卸任人员若 report_to 未清理也会返回。</p>
     *
     * @param userId 上级用户ID
     * @return 直属下级用户列表
     */
    @GetMapping("/listSubordinatesByUserId")
    Result<List<OrgUserAO>> listSubordinatesByUserId(@RequestParam("userId") Long userId);

    /**
     * 根据用户ID查询指定租户内其直属下级（直接汇报人）
     *
     * <p>语义与 {@link #listSubordinatesByUserId} 完全一致（仅直接下级、不递归；
     * 不做任职状态过滤；按 {@code user_id} 去重），只是把租户条件由
     * 「隐式取当前登录上下文」改为「显式传入」。</p>
     *
     * <p><b>什么时候用本变体，而不是 {@link #listSubordinatesByUserId}</b>：
     * {@link #listSubordinatesByUserId} 的租户条件来自 {@code OrgTenantLineHandler}
     * （读 {@code SecurityUtils.getCurrentUser().getTenantId()}），而框架的
     * {@code FeignRequestInterceptor} 只从**当前 {@code HttpServletRequest}**
     * 透传 {@code X-Tenant-Id} / {@code X-User-Id}。用户请求链路天然正确；
     * 后台任务 / 无请求上下文的链路无法透传。</p>
     *
     * <p><b>本变体的首要使用方就是 carlos-job</b>：调度线程执行「超时升级上级」时
     * 线程内没有 {@code HttpServletRequest}，租户条件会静默回落到
     * {@code carlos.org.tenant.platform-tenant-id}（平台租户 0），
     * 在多租户部署下会把工单升级给**另一个租户的上级**。因此本变体绕过租户拦截器后
     * 显式按 {@code tenant_id} 过滤，<b>专供无请求上下文的调用方使用</b>，请勿删除。</p>
     *
     * @param userId   上级用户ID
     * @param tenantId 租户ID（必填；为 null 时返回空列表，不查库）
     * @return 直属下级用户列表
     */
    @GetMapping("/listSubordinatesByUserIdAndTenant")
    Result<List<OrgUserAO>> listSubordinatesByUserIdAndTenant(@RequestParam("userId") Long userId,
                                                              @RequestParam("tenantId") Long tenantId);

    /**
     * 根据用户ID查询其直属上级
     *
     * <p>读的是<b>该用户自己</b>任职记录上的 {@code org_user_position.report_to}，
     * 与 {@link #listSubordinatesByUserId} 方向相反（后者按 {@code report_to = userId} 取<b>下级</b>）。</p>
     *
     * <p>用户有多条任职记录（兼岗）时，优先取主岗位（{@code is_main = true}）记录的
     * {@code report_to}，主岗位未配置时回退为任意一条已配置的记录。不做任职状态过滤，
     * 口径与 {@link #listSubordinatesByUserId} 一致。</p>
     *
     * <p><b>没有任职记录、或 report_to 未配置时返回 {@code data = null}，不是错误</b>：
     * 「汇报线上没有上级」是正常数据状态（例如组织架构顶层），调用方按 null 降级处理。</p>
     *
     * @param userId 用户ID
     * @return 直属上级用户信息；无上级时 {@code data = null}
     */
    @GetMapping("/getSuperiorByUserId")
    Result<OrgUserAO> getSuperiorByUserId(@RequestParam("userId") Long userId);

    /**
     * 根据用户ID查询指定租户内其直属上级
     *
     * <p>语义与 {@link #getSuperiorByUserId} 完全一致（读该用户自己的 report_to；
     * 优先主岗位；不做任职状态过滤；无上级返回 {@code data = null}），
     * 只是把租户条件由「隐式取当前登录上下文」改为「显式传入」。</p>
     *
     * <p><b>什么时候用本变体，而不是 {@link #getSuperiorByUserId}</b>：
     * {@link #getSuperiorByUserId} 的租户条件来自 {@code OrgTenantLineHandler}
     * （读 {@code SecurityUtils.getCurrentUser().getTenantId()}），而框架的
     * {@code FeignRequestInterceptor} 只从**当前 {@code HttpServletRequest}**
     * 透传 {@code X-Tenant-Id} / {@code X-User-Id}。用户请求链路天然正确；
     * 后台任务 / 无请求上下文的链路无法透传，租户条件会静默回落到
     * {@code carlos.org.tenant.platform-tenant-id}（平台租户 0），
     * 在多租户部署下会解析出**另一个租户的上级**。</p>
     *
     * <p>本变体绕过租户拦截器后显式按 {@code tenant_id} 过滤，
     * <b>专供无请求上下文的调用方使用</b>，请勿删除。</p>
     *
     * @param userId   用户ID
     * @param tenantId 租户ID（必填；为 null 时返回 {@code data = null}，不查库）
     * @return 直属上级用户信息；无上级时 {@code data = null}
     */
    @GetMapping("/getSuperiorByUserIdAndTenant")
    Result<OrgUserAO> getSuperiorByUserIdAndTenant(@RequestParam("userId") Long userId,
                                                   @RequestParam("tenantId") Long tenantId);

    /**
     * 根据岗位ID查询任职该岗位的用户
     *
     * <p>数据源为 {@code org_user_position}，按 {@code position_id} 取 {@code user_id} 去重后
     * 批量返回用户，支撑「办理人 = 某岗位上的全部人」这类按岗位路由的场景。
     * 一个人在同一岗位上可能有多条任职记录（跨部门兼岗），返回结果按 {@code user_id} 去重。</p>
     *
     * <p><b>不做任职状态过滤</b>（{@code position_status} 不参与条件），口径与
     * {@link #listSubordinatesByUserId} 一致：已卸任（{@code position_status = 5}）的记录
     * 若未清理也会返回。刻意如此——{@code position_status = 2}（试用期）是正常在职状态，
     * 一刀切按「仅在职（1）」过滤会漏人；是否排除已卸任由调用方按业务判断。
     * 对比 {@code OrgUserPositionManager#countOccupiedByPositionId}（岗位编制占用人数的口径），
     * 那里只数在职（1），两者的数值本就不应一致。</p>
     *
     * @param positionId 岗位ID
     * @return 该岗位上的用户列表，按 {@code user_id} 去重
     */
    @GetMapping("/listUsersByPositionId")
    Result<List<OrgUserAO>> listUsersByPositionId(@RequestParam("positionId") Long positionId);

    /**
     * 根据岗位ID查询指定租户内任职该岗位的用户
     *
     * <p>语义与 {@link #listUsersByPositionId} 完全一致（不做任职状态过滤、按 {@code user_id} 去重），
     * 只是把租户条件由「隐式取当前登录上下文」改为「显式传入」。</p>
     *
     * <p><b>什么时候用本变体，而不是 {@link #listUsersByPositionId}</b>：
     * {@link #listUsersByPositionId} 的租户条件来自 {@code OrgTenantLineHandler}
     * （读 {@code SecurityUtils.getCurrentUser().getTenantId()}），而框架的
     * {@code FeignRequestInterceptor} 只从**当前 {@code HttpServletRequest}**
     * 透传 {@code X-Tenant-Id} / {@code X-User-Id}。用户请求链路天然正确；
     * 后台任务 / 无请求上下文的链路无法透传，租户条件会静默回落到
     * {@code carlos.org.tenant.platform-tenant-id}（平台租户 0），
     * 在多租户部署下返回**错误租户的用户**。</p>
     *
     * <p>本变体绕过租户拦截器后显式按 {@code tenant_id} 过滤，
     * <b>专供无请求上下文的调用方（后台任务、系统间调用）使用</b>，请勿删除。</p>
     *
     * @param positionId 岗位ID
     * @param tenantId   租户ID（必填；为 null 时返回空列表，不查库）
     * @return 该岗位上的用户列表，按 {@code user_id} 去重
     */
    @GetMapping("/listUsersByPositionIdAndTenant")
    Result<List<OrgUserAO>> listUsersByPositionIdAndTenant(@RequestParam("positionId") Long positionId,
                                                           @RequestParam("tenantId") Long tenantId);

}
