package com.carlos.org.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.org.pojo.dto.OrgUserCandidateDTO;
import com.carlos.org.pojo.dto.OrgUserDTO;
import com.carlos.org.pojo.entity.OrgUser;
import com.carlos.org.pojo.param.OrgUserCandidatePageParam;
import com.carlos.org.pojo.param.OrgUserPageParam;
import com.carlos.org.pojo.vo.OrgUserVO;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 系统用户 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
public interface OrgUserManager extends BaseService<OrgUser> {

    /**
     * 新增系统用户
     *
     * @param dto 系统用户数据
     * @return boolean
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    boolean add(OrgUserDTO dto);

    /**
     * 删除系统用户
     *
     * @param id 系统用户id
     * @return boolean
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    boolean delete(Serializable id);

    /**
     * 修改系统用户信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    boolean modify(OrgUserDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.org.pojo.dto.OrgUserDTO
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgUserDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param  param 分页参数
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    Paging<OrgUserVO> getPage(OrgUserPageParam param);

    /**
     * 分页查询候选用户（选人场景：转办、加签、减签、审批人指派等）
     *
     * <p>与 {@link #getPage(OrgUserPageParam)} 的区别：</p>
     * <ul>
     *   <li>只返回 {@code id / account / nickname / main_dept_id + 部门名称}，不含密码、证件号等敏感字段；</li>
     *   <li>支持按 {@code includeIds} 收窄、按 {@code excludeIds} 排除（例如「加签只能选还没参与的人」）；</li>
     *   <li>按部门过滤时<b>含子孙部门</b>（{@code org_user.main_dept_id} 落在该子树内）；</li>
     *   <li>固定过滤禁用账号，并按「部门 + 用户id」稳定排序，翻页不会重复或跳记录。</li>
     * </ul>
     *
     * @param param 候选查询参数
     * @return Paging&lt;OrgUserCandidateDTO&gt;，{@code includeIds} 给出空集合时返回空分页
     */
    Paging<OrgUserCandidateDTO> getCandidatePage(OrgUserCandidatePageParam param);

    // 逻辑删除由框架自动处理，使用removeById即可

    /**
     * 根据账号查询用户
     *
     * @param account 账号
     * @return OrgUserDTO
     */
    OrgUserDTO getUserByAccount(String account);

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return OrgUserDTO
     */
    OrgUserDTO getUserByPhone(String phone);

    /**
     * 根据账号查询指定租户内的用户
     *
     * <p>绕过租户拦截器（InterceptorIgnoreHelper）后显式按 tenant_id 过滤，
     * 供登录链路按租户定位用户、租户管理员初始化做租户内唯一性校验使用。</p>
     *
     * @param account  账号
     * @param tenantId 租户ID
     * @return OrgUserDTO
     */
    OrgUserDTO getUserByAccountAndTenant(String account, Long tenantId);

    /**
     * 根据手机号查询指定租户内的用户
     *
     * @param phone    手机号
     * @param tenantId 租户ID
     * @return OrgUserDTO
     */
    OrgUserDTO getUserByPhoneAndTenant(String phone, Long tenantId);

    /**
     * 统计指定租户下的用户数量
     *
     * @param tenantId 租户ID
     * @return 用户数量
     */
    Long countByTenantId(Long tenantId);

    /**
     * 根据查询参数导出用户列表
     *
     * @param param 查询参数
     * @return List<OrgUserDTO>
     */
    List<OrgUserDTO> listByParam(OrgUserPageParam param);

    /**
     * 根据用户ID集合批量查询用户
     *
     * <p>供 Feign 远程批量查询使用，一次查询取回全部用户，避免调用方 N+1。
     * id 集合会先去重去空，空集合直接返回空列表，不产生数据库查询。</p>
     *
     * @param ids 用户ID集合
     * @return List<OrgUserDTO>
     */
    List<OrgUserDTO> listDtoByIds(Collection<Long> ids);

    /**
     * 根据用户ID查询指定租户内的用户
     *
     * <p>绕过租户拦截器（{@code InterceptorIgnoreHelper}）后显式按 {@code tenant_id} 过滤，
     * 供**无 {@code HttpServletRequest} 上下文**的调用方（后台任务、系统间调用）使用。
     * 有请求上下文时租户由 {@code OrgTenantLineHandler} 自动注入，不需要本方法。</p>
     *
     * @param id       用户ID
     * @param tenantId 租户ID
     * @return OrgUserDTO，任一参数为空或用户不存在时返回 null
     */
    OrgUserDTO getDtoByIdAndTenant(Serializable id, Long tenantId);

    /**
     * 根据用户ID集合批量查询指定租户内的用户
     *
     * <p>空集合、空 tenantId 直接返回空列表，不产生数据库查询；
     * 绕过租户拦截器（{@code InterceptorIgnoreHelper}）后显式按 {@code tenant_id} 过滤，
     * 供**无 {@code HttpServletRequest} 上下文**的调用方（后台任务、系统间调用）使用。</p>
     *
     * @param ids      用户ID集合
     * @param tenantId 租户ID
     * @return List&lt;OrgUserDTO&gt;
     */
    List<OrgUserDTO> listDtoByIdsAndTenant(Collection<Long> ids, Long tenantId);

    /**
     * 更新用户密码（内部接口 - 密码迁移专用）
     *
     * <p>直接覆盖用户密码字段，传入的密码已经是编码后的哈希值。</p>
     *
     * @param userId          用户 ID
     * @param encodedPassword 编码后的密码哈希（带前缀如 {@code {argon2}$argon2id$...}）
     */
    void updatePassword(Long userId, String encodedPassword);
}
