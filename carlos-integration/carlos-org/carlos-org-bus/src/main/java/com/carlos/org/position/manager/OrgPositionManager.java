package com.carlos.org.position.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.org.position.pojo.dto.OrgPositionDTO;
import com.carlos.org.position.pojo.entity.OrgPosition;
import com.carlos.org.position.pojo.param.OrgPositionPageParam;
import com.carlos.org.position.pojo.vo.OrgPositionVO;

import java.io.Serializable;

/**
 * <p>
 * 岗位表 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
public interface OrgPositionManager extends BaseService<OrgPosition> {

    /**
     * 新增岗位表
     *
     * @param dto 岗位表数据
     * @return boolean
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    boolean add(OrgPositionDTO dto);

    /**
     * 删除岗位表
     *
     * @param id 岗位表id
     * @return boolean
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    boolean delete(Serializable id);

    /**
     * 修改岗位表信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    boolean modify(OrgPositionDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.org.position.pojo.dto.OrgPositionDTO
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param  param 分页参数
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    Paging<OrgPositionVO> getPage(OrgPositionPageParam param);
}
