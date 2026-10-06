package com.carlos.org.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.org.pojo.dto.OrgUserDepartmentDTO;
import com.carlos.org.pojo.entity.OrgUserDepartment;
import com.carlos.org.pojo.param.OrgUserDepartmentPageParam;
import com.carlos.org.pojo.vo.OrgUserDepartmentVO;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 用户部门 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
public interface OrgUserDepartmentManager extends BaseService<OrgUserDepartment> {

    /**
     * 新增用户部门
     *
     * @param dto 用户部门数据
     * @return boolean
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    boolean add(OrgUserDepartmentDTO dto);

    /**
     * 删除用户部门
     *
     * @param id 用户部门id
     * @return boolean
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    boolean delete(Serializable id);

    /**
     * 修改用户部门信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    boolean modify(OrgUserDepartmentDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.org.pojo.dto.OrgUserDepartmentDTO
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    OrgUserDepartmentDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param  param 分页参数
     * @author Carlos
     * @date 2026年2月28日 下午1:25:36
     */
    Paging<OrgUserDepartmentVO> getPage(OrgUserDepartmentPageParam param);

    /**
     * 根据用户ID删除部门关联
     *
     * @param userId 用户ID
     * @return boolean
     */
    boolean removeByUserId(Serializable userId);

    /**
     * 根据用户ID查询部门关联列表
     *
     * @param userId 用户ID
     * @return List<OrgUserDepartmentDTO>
     */
    List<OrgUserDepartmentDTO> listByUserId(Serializable userId);
}
