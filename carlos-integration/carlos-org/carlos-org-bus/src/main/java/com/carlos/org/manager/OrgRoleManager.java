package com.carlos.org.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.org.pojo.dto.OrgRoleDTO;
import com.carlos.org.pojo.dto.OrgRoleUserDTO;
import com.carlos.org.pojo.entity.OrgRole;
import com.carlos.org.pojo.param.OrgRolePageParam;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 角色 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
public interface OrgRoleManager extends BaseService<OrgRole> {

    /**
     * 新增角色
     *
     * @param dto 角色数据
     * @return boolean
     */
    boolean add(OrgRoleDTO dto);

    /**
     * 删除角色
     *
     * @param id 角色id
     * @return boolean
     */
    boolean delete(Serializable id);

    /**
     * 修改角色信息
     *
     * @param dto 对象信息
     * @return boolean
     */
    boolean modify(OrgRoleDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return OrgRoleDTO
     */
    OrgRoleDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param param 分页参数
     * @return Paging<OrgRoleDTO>
     */
    Paging<OrgRoleDTO> getPage(OrgRolePageParam param);

    /**
     * 根据角色编码获取角色
     *
     * @param roleCode 角色编码
     * @return OrgRoleDTO
     */
    OrgRoleDTO getByRoleCode(String roleCode);

    /**
     * 查询启用状态的角色列表（仅 id/roleName，用于下拉选项，避免分页截断）
     *
     * @return 启用角色列表
     */
    List<OrgRoleDTO> listEnabled();

    /**
     * 获取角色的操作ID列表
     *
     * @param roleId 角色ID
     * @return List<Long>
     */
    List<Long> getOperationIdsByRoleId(Serializable roleId);

    /**
     * 获取使用某角色的用户数量
     *
     * @param roleId 角色ID
     * @return int
     */
    Long getUserCountByRoleId(Serializable roleId);

    /**
     * 分配角色操作权限
     *
     * @param roleId       角色ID
     * @param operationIds 操作ID列表
     * @return boolean
     */
    boolean assignOperations(Serializable roleId, List<Serializable> operationIds);

    /**
     * 获取角色用户列表
     *
     * @param roleId 角色ID
     * @param param 分页参数
     * @return Paging<OrgRoleUserDTO>
     */
    Paging<OrgRoleUserDTO> getRoleUsers(Serializable roleId, OrgRolePageParam param);

    /**
     * 获取持有某角色的用户ID列表（去重）
     *
     * <p>ADR-002: 仅返回未过期的授权（expire_time 为空或晚于当前时间），
     * 过期临时授权不计入。不做 dept_id 作用域过滤，部门内生效的授权同样计入。</p>
     *
     * <p>供 Feign 远程按角色解析用户列表使用：先取 id 集合，再批量查询用户，
     * 避免逐条查询（N+1）。</p>
     *
     * @param roleId 角色ID
     * @return 用户ID列表
     */
    List<Long> listUserIdsByRoleId(Serializable roleId);

    /**
     * 获取指定租户内持有某角色的用户ID列表（去重）
     *
     * <p>口径与 {@link #listUserIdsByRoleId} 完全一致（ADR-002：仅返回未过期的授权，
     * {@code expire_time} 为空或晚于当前时间；不做 dept_id 作用域过滤），
     * 但绕过租户拦截器后显式按 {@code tenant_id} 过滤，
     * 供**无 {@code HttpServletRequest} 上下文**的调用方（后台任务、系统间调用）使用。</p>
     *
     * @param roleId   角色ID
     * @param tenantId 租户ID
     * @return 用户ID列表，任一参数为空时返回空列表
     */
    List<Long> listUserIdsByRoleIdAndTenant(Serializable roleId, Long tenantId);

}
