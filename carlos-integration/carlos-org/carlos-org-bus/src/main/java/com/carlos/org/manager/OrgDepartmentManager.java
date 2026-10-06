package com.carlos.org.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.org.pojo.dto.OrgDepartmentDTO;
import com.carlos.org.pojo.dto.OrgDepartmentUserDTO;
import com.carlos.org.pojo.dto.OrgUserDTO;
import com.carlos.org.pojo.entity.OrgDepartment;
import com.carlos.org.pojo.param.OrgDepartmentPageParam;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 部门 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
public interface OrgDepartmentManager extends BaseService<OrgDepartment> {

    /**
     * 新增部门
     *
     * @param dto 部门数据
     * @return boolean
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    boolean add(OrgDepartmentDTO dto);

    /**
     * 删除部门
     *
     * @param id 部门id
     * @return boolean
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    boolean delete(Serializable id);

    /**
     * 修改部门信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    boolean modify(OrgDepartmentDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.org.pojo.dto.OrgDepartmentDTO
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgDepartmentDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param param 分页参数
     * @return Paging<OrgDepartmentDTO>
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    Paging<OrgDepartmentDTO> getPage(OrgDepartmentPageParam param);

    /**
     * 获取所有部门
     *
     * @return List<OrgDepartmentDTO>
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    List<OrgDepartmentDTO> listAll();

    /**
     * 根据部门编号获取部门
     *
     * @param deptCode 部门编号
     * @return OrgDepartmentDTO
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgDepartmentDTO getByDeptCode(String deptCode);

    /**
     * 获取子部门列表
     *
     * @param parentId 父部门ID
     * @return List<OrgDepartmentDTO>
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    List<OrgDepartmentDTO> getChildrenByParentId(Serializable parentId);

    /**
     * 获取部门下的用户列表
     *
     * @param deptId 部门ID
     * @return List<OrgUserDTO>
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    List<OrgUserDTO> getUsersByDeptId(Serializable deptId);

    /**
     * 获取指定租户内部门下的用户列表
     *
     * <p>语义与 {@link #getUsersByDeptId} 完全一致（非递归，只含直接归属该部门的用户），
     * 但绕过租户拦截器后显式按 {@code org_user.tenant_id} 过滤，
     * 供**无 {@code HttpServletRequest} 上下文**的调用方（后台任务、系统间调用）使用。
     * 有请求上下文时租户由 {@code OrgTenantLineHandler} 自动注入，不需要本方法。</p>
     *
     * <p>注意：{@code org_user_department} 表自身没有 {@code tenant_id} 列
     * （见 {@code OrgProperties.OrgTenantProperties#tables}），
     * 租户条件只能落在关联的用户行上。</p>
     *
     * @param deptId   部门ID
     * @param tenantId 租户ID
     * @return List&lt;OrgUserDTO&gt;，任一参数为空时返回空列表
     */
    List<OrgUserDTO> getUsersByDeptIdAndTenant(Serializable deptId, Long tenantId);

    /**
     * 分页获取部门下的用户列表
     *
     * @param deptId 部门ID
     * @param param  分页参数
     * @return Paging<OrgDepartmentUserDTO>
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    Paging<OrgDepartmentUserDTO> getUsersByDeptId(Serializable deptId, OrgDepartmentPageParam param);

    /**
     * 初始化缓存
     *
     * @author Carlos
     * @date 2026-04-29
     */
    void initCache();

    /**
     * 清空缓存
     *
     * @return 删除的key数量
     * @author Carlos
     * @date 2026-04-29
     */
    long clearCache();

    /**
     * 获取指定部门的祖先链（自顶向下）
     *
     * @param id    部门主键
     * @param limit 限制层级，<=0 表示不限制
     * @return 祖先 id 列表
     * @author Carlos
     * @date 2026-04-29
     */
    List<Long> getAncestorIdsFromCache(Long id, long limit);

    /**
     * 获取指定部门的全部子孙 id 集合
     *
     * @param id 部门主键
     * @return 子孙 id 集合
     * @author Carlos
     * @date 2026-04-29
     */
    Set<Long> getDescIdsFromCache(Long id);

    /**
     * 从缓存中批量获取部门对象（按需指定字段）
     *
     * @param ids    部门 id 列表
     * @param fields 指定字段，null 表示全部
     * @return 部门 DTO 列表
     * @author Carlos
     * @date 2026-04-29
     */
    List<OrgDepartmentDTO> listDeptFromCache(List<Long> ids, List<String> fields);

    /**
     * 获取部门<b>及其全部子孙部门</b>下的用户列表
     *
     * <p><b>语义：递归</b>。部门集合由服务端展开（优先命中「全部子孙」缓存 DESC_KEY；
     * 缓存未预热/被清空时逐层下探数据库），随后用**一次** {@code org_user_department}
     * 查询取回全部用户ID，再批量取用户。调用方只需一次远程往返，
     * 不要为每个子部门各调一次 {@link #getUsersByDeptId}。</p>
     *
     * <p>展开口径：命中的部门集合 = 传入部门自身 + 其全部子孙部门；
     * 按 {@code org_user_department.dept_id} 归属判定用户，结果按 {@code user_id} 去重
     * （一个人可能同时属于子树里的多个部门）。与 {@link #getUsersByDeptId}（非递归）语义不同。</p>
     *
     * @param deptId 部门ID（子树的根）
     * @return List&lt;OrgUserDTO&gt;，按 {@code user_id} 去重；deptId 为空时返回空列表
     */
    List<OrgUserDTO> getUsersByDeptIdRecursive(Serializable deptId);

    /**
     * 获取指定租户内部门<b>及其全部子孙部门</b>下的用户列表
     *
     * <p>语义与 {@link #getUsersByDeptIdRecursive} 完全一致（递归展开部门集合、一次归属查询、
     * 按 {@code user_id} 去重），但绕过租户拦截器后显式按 {@code org_user.tenant_id} 过滤，
     * 供**无 {@code HttpServletRequest} 上下文**的调用方（后台任务、系统间调用）使用。</p>
     *
     * <p>注意：部门集合的展开复用部门缓存 / 部门表（{@code org_department}），
     * 租户条件只落在用户行上——即使部门集合跨了租户，用户查询也会按 {@code tenant_id} 收窄，
     * 不会返回其它租户的用户。</p>
     *
     * @param deptId   部门ID（子树的根）
     * @param tenantId 租户ID
     * @return List&lt;OrgUserDTO&gt;，按 {@code user_id} 去重；任一参数为空时返回空列表
     */
    List<OrgUserDTO> getUsersByDeptIdRecursiveAndTenant(Serializable deptId, Long tenantId);

}
