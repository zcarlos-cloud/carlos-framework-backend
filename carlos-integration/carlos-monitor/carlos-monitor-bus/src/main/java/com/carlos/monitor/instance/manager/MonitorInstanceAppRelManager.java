package com.carlos.monitor.instance.manager;

import com.carlos.datasource.base.BaseService;
import com.carlos.monitor.instance.pojo.entity.MonitorInstanceAppRel;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 监控实例-应用关联 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
public interface MonitorInstanceAppRelManager extends BaseService<MonitorInstanceAppRel> {

    /**
     * 全量替换实例的应用关联（先删后插）
     *
     * @param instanceId 实例ID
     * @param appNames   应用名列表
     */
    void replaceAppRels(Long instanceId, List<String> appNames);

    /**
     * 查询实例关联的应用名列表
     *
     * @param instanceId 实例ID
     * @return 应用名列表
     */
    List<String> listAppNames(Long instanceId);

    /**
     * 批量查询实例关联的应用名（instanceId -> appNames）
     *
     * @param instanceIds 实例ID集合
     * @return 关联列表
     */
    List<MonitorInstanceAppRel> listByInstanceIds(Collection<Long> instanceIds);

    /**
     * 按应用名查询关联的实例ID列表
     *
     * @param appName 应用名
     * @return 实例ID列表
     */
    List<Long> listInstanceIdsByAppName(String appName);

    /**
     * 删除实例的全部应用关联（实例删除时调用）
     *
     * @param instanceId 实例ID
     */
    void deleteByInstanceId(Serializable instanceId);
}
