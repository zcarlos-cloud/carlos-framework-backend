package com.carlos.monitor.instance.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceGroupDTO;
import com.carlos.monitor.instance.pojo.entity.MonitorInstanceGroup;
import com.carlos.monitor.instance.pojo.param.GroupPageParam;
import com.carlos.monitor.instance.pojo.vo.InstanceGroupVO;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 监控实例分组 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
public interface MonitorInstanceGroupManager extends BaseService<MonitorInstanceGroup> {

    /**
     * 新增分组
     *
     * @param dto 分组数据
     * @return 是否成功
     */
    boolean add(MonitorInstanceGroupDTO dto);

    /**
     * 修改分组
     *
     * @param dto 分组数据
     * @return 是否成功
     */
    boolean modify(MonitorInstanceGroupDTO dto);

    /**
     * 逻辑删除分组
     *
     * @param id 分组ID
     * @return 是否成功
     */
    boolean delete(Serializable id);

    /**
     * 分组详情
     *
     * @param id 分组ID
     * @return 分组 DTO，不存在返回 null
     */
    MonitorInstanceGroupDTO getDtoById(Serializable id);

    /**
     * 分页查询
     *
     * @param param 分页参数
     * @return 分页结果
     */
    Paging<InstanceGroupVO> getPage(GroupPageParam param);

    /**
     * 全部分组列表（分组树/下拉用）
     *
     * @return 分组 DTO 列表
     */
    List<MonitorInstanceGroupDTO> listAll();

    /**
     * 统计分组名称数量（唯一性校验）
     *
     * @param groupName 分组名称
     * @param excludeId 排除的分组ID（编辑场景）
     * @return 数量
     */
    long countByName(String groupName, Serializable excludeId);
}
