package com.carlos.auth.idp;

import java.util.Set;

/**
 * 角色提供者接口
 *
 * <p>为身份源提供者提供用户角色查询能力。</p>
 *
 * <p>生产环境应提供自定义实现，从权限中心或组织机构服务加载用户角色。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-29
 */
public interface RoleProvider {

    /**
     * 根据用户ID查询角色编码集合
     *
     * @param userId 用户ID
     * @return 角色编码集合，如果没有角色返回空集合
     */
    Set<String> getRoleCodesByUserId(Long userId);

    /**
     * 根据用户标识查询角色编码集合
     *
     * @param identifier 用户标识（用户名、手机号、邮箱）
     * @return 角色编码集合，如果没有角色返回空集合
     */
    Set<String> getRoleCodesByIdentifier(String identifier);
}
