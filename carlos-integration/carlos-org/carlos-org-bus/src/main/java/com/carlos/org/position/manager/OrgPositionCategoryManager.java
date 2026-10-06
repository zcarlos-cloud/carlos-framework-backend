package com.carlos.org.position.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.org.position.pojo.dto.OrgPositionCategoryDTO;
import com.carlos.org.position.pojo.entity.OrgPositionCategory;
import com.carlos.org.position.pojo.param.OrgPositionCategoryPageParam;
import com.carlos.org.position.pojo.vo.OrgPositionCategoryVO;

import java.io.Serializable;

/**
 * <p>
 * 岗位类别表（职系） 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
public interface OrgPositionCategoryManager extends BaseService<OrgPositionCategory> {

    /**
     * 新增岗位类别表（职系）
     *
     * @param dto 岗位类别表（职系）数据
     * @return boolean
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    boolean add(OrgPositionCategoryDTO dto);

    /**
     * 删除岗位类别表（职系）
     *
     * @param id 岗位类别表（职系）id
     * @return boolean
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    boolean delete(Serializable id);

    /**
     * 修改岗位类别表（职系）信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    boolean modify(OrgPositionCategoryDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.org.position.pojo.dto.OrgPositionCategoryDTO
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionCategoryDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param  param 分页参数
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    Paging<OrgPositionCategoryVO> getPage(OrgPositionCategoryPageParam param);
}
