package com.carlos.auth.app.manager;

import com.carlos.auth.app.pojo.entity.AppRoleAccess;
import com.carlos.datasource.base.BaseService;

import java.util.List;

/**
 * 应用角色访问控制 查询封装接口
 *
 * @author Carlos
 * @date 2026-07-02
 */
public interface AppRoleAccessManager extends BaseService<AppRoleAccess> {

    /**
     * 设置应用的可见角色
     *
     * @param appId     应用ID
     * @param roleCodes 角色编码列表
     */
    void setAppRoles(Long appId, List<String> roleCodes);

    /**
     * 获取应用的可见角色列表
     *
     * @param appId 应用ID
     * @return 角色编码列表
     */
    List<String> getAppRoles(Long appId);

    /**
     * 获取指定角色可见的应用ID列表
     *
     * @param roleCodes 角色编码列表
     * @return 应用ID列表
     */
    List<Long> getAccessibleAppIds(List<String> roleCodes);

    /**
     * 获取所有有角色限制的应用ID集合
     *
     * @return 在 app_role_access 表中有记录的所有 appId
     */
    List<Long> getAllRestrictedAppIds();
}
