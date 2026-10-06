package com.carlos.monitor.instance.service;

import com.carlos.monitor.instance.pojo.dto.MonitorInstanceGroupDTO;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 监控实例分组 业务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
public interface MonitorGroupService {

    /**
     * 新增分组（含角色授权）
     *
     * @param dto 分组数据
     * @return 分组ID
     */
    Long addGroup(MonitorInstanceGroupDTO dto);

    /**
     * 修改分组（含角色授权全量替换）
     *
     * @param dto 分组数据
     * @return 是否成功
     */
    boolean updateGroup(MonitorInstanceGroupDTO dto);

    /**
     * 删除分组（同时清理分组授权；分组下实例的 group_id 保留为悬挂引用，编辑实例时可改组）
     *
     * @param id 分组ID
     * @return 是否成功
     */
    boolean deleteGroup(Serializable id);

    /**
     * 分组详情（含授权角色ID列表）
     *
     * @param id 分组ID
     * @return 分组 DTO
     */
    MonitorInstanceGroupDTO getDetail(Serializable id);

    /**
     * 全量替换分组授权
     *
     * @param groupId 分组ID
     * @param roleIds 角色ID列表
     */
    void updateAuth(Long groupId, List<Long> roleIds);

    /**
     * 全部分组列表（含授权角色ID列表）
     *
     * @return 分组 DTO 列表
     */
    List<MonitorInstanceGroupDTO> listAll();
}
