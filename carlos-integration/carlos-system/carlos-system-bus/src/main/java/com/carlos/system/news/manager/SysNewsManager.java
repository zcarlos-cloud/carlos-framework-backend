package com.carlos.system.news.manager;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carlos.system.news.pojo.dto.SysNewsDTO;
import com.carlos.system.news.pojo.param.SysNewsPageParam;
import com.carlos.system.news.pojo.vo.SysNewsVO;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 公告/站内通知 查询封装接口
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
public interface SysNewsManager {

    /**
     * 新增公告
     *
     * @param dto 公告数据
     * @return true-成功
     */
    boolean add(SysNewsDTO dto);

    /**
     * 删除公告（逻辑删除）
     *
     * @param ids 公告ID集合
     * @return true-成功
     */
    boolean delete(Set<Long> ids);

    /**
     * 修改公告
     *
     * @param dto 公告数据
     * @return true-成功
     */
    boolean modify(SysNewsDTO dto);

    /**
     * 按ID查询
     *
     * @param id 公告ID
     * @return 公告数据，不存在返回 null
     */
    SysNewsDTO getDtoById(Serializable id);

    /**
     * 管理端分页
     *
     * @param param 分页参数
     * @return 分页结果
     */
    IPage<SysNewsVO> getPage(SysNewsPageParam param);

    /**
     * 查询已发布且未过期的候选公告（用户端可见性过滤前）
     *
     * <p>只查可见性判定与列表渲染所需字段，不查 content 大字段。</p>
     *
     * @return 候选公告列表
     */
    List<SysNewsDTO> listPublishedActive();

}
