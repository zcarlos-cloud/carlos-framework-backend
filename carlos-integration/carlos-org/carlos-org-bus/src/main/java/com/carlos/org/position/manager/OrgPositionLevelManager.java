package com.carlos.org.position.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.org.position.pojo.dto.OrgPositionLevelDTO;
import com.carlos.org.position.pojo.entity.OrgPositionLevel;
import com.carlos.org.position.pojo.param.OrgPositionLevelPageParam;
import com.carlos.org.position.pojo.vo.OrgPositionLevelVO;

import java.io.Serializable;

/**
 * <p>
 * 职级表 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
public interface OrgPositionLevelManager extends BaseService<OrgPositionLevel> {

    /**
     * 新增职级表
     *
     * @param dto 职级表数据
     * @return boolean
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    boolean add(OrgPositionLevelDTO dto);

    /**
     * 删除职级表
     *
     * @param id 职级表id
     * @return boolean
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    boolean delete(Serializable id);

    /**
     * 修改职级表信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    boolean modify(OrgPositionLevelDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.org.position.pojo.dto.OrgPositionLevelDTO
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionLevelDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param  param 分页参数
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    Paging<OrgPositionLevelVO> getPage(OrgPositionLevelPageParam param);
}
