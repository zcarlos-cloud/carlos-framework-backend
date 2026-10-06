package com.carlos.monitor.instance.manager;

import com.carlos.datasource.base.BaseService;
import com.carlos.monitor.instance.pojo.entity.MonitorGroupAuth;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 监控分组-角色授权 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
public interface MonitorGroupAuthManager extends BaseService<MonitorGroupAuth> {

    /**
     * 全量替换分组授权（先删后插）
     *
     * @param groupId 分组ID
     * @param roleIds 角色ID列表
     */
    void replaceAuth(Long groupId, List<Long> roleIds);

    /**
     * 查询分组授权的角色ID列表
     *
     * @param groupId 分组ID
     * @return 角色ID列表
     */
    List<Long> listRoleIds(Long groupId);

    /**
     * 批量查询多个分组的授权角色ID（分页回显用，避免 N+1）
     *
     * @param groupIds 分组ID集合
     * @return key=分组ID，value=角色ID列表
     */
    Map<Long, List<Long>> listRoleIdMap(Collection<Long> groupIds);

    /**
     * 删除分组的全部授权（分组删除时调用）
     *
     * @param groupId 分组ID
     */
    void deleteByGroupId(Serializable groupId);
}
