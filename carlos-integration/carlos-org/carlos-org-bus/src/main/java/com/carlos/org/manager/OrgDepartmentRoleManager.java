package com.carlos.org.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.org.pojo.dto.OrgDepartmentRoleDTO;
import com.carlos.org.pojo.entity.OrgDepartmentRole;
import com.carlos.org.pojo.param.OrgDepartmentRolePageParam;
import com.carlos.org.pojo.vo.OrgDepartmentRoleVO;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 部门角色 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
public interface OrgDepartmentRoleManager extends BaseService<OrgDepartmentRole> {

    /**
     * 新增部门角色
     *
     * @param dto 部门角色数据
     * @return boolean
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    boolean add(OrgDepartmentRoleDTO dto);

    /**
     * 删除部门角色
     *
     * @param id 部门角色id
     * @return boolean
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    boolean delete(Serializable id);

    /**
     * 修改部门角色信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    boolean modify(OrgDepartmentRoleDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.org.pojo.dto.OrgDepartmentRoleDTO
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgDepartmentRoleDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param  param 分页参数
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    Paging<OrgDepartmentRoleVO> getPage(OrgDepartmentRolePageParam param);

    /**
     * 根据部门ID和角色ID删除关联
     *
     * @param deptId 部门ID
     * @param roleId 角色ID
     * @return boolean
     */
    boolean removeByDeptIdAndRoleId(Serializable deptId, Serializable roleId);

    /**
     * 根据部门ID查询角色关联列表
     *
     * @param deptId 部门ID
     * @return List<OrgDepartmentRoleDTO>
     */
    List<OrgDepartmentRoleDTO> listByDeptId(Serializable deptId);
}
