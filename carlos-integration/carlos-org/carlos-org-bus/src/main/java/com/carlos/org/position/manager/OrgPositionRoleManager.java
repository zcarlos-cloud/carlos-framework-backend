package com.carlos.org.position.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.org.position.pojo.dto.OrgPositionRoleDTO;
import com.carlos.org.position.pojo.entity.OrgPositionRole;
import com.carlos.org.position.pojo.param.OrgPositionRolePageParam;
import com.carlos.org.position.pojo.vo.OrgPositionRoleVO;

import java.io.Serializable;

/**
 * <p>
 * 岗位角色关联表（岗位默认权限配置） 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
public interface OrgPositionRoleManager extends BaseService<OrgPositionRole> {

    /**
     * 新增岗位角色关联表（岗位默认权限配置）
     *
     * @param dto 岗位角色关联表（岗位默认权限配置）数据
     * @return boolean
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    boolean add(OrgPositionRoleDTO dto);

    /**
     * 删除岗位角色关联表（岗位默认权限配置）
     *
     * @param id 岗位角色关联表（岗位默认权限配置）id
     * @return boolean
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    boolean delete(Serializable id);

    /**
     * 修改岗位角色关联表（岗位默认权限配置）信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    boolean modify(OrgPositionRoleDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.org.position.pojo.dto.OrgPositionRoleDTO
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionRoleDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param  param 分页参数
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    Paging<OrgPositionRoleVO> getPage(OrgPositionRolePageParam param);
}
