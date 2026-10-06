package com.carlos.datascope.provider;

import java.io.Serializable;
import java.util.Set;

/**
 * 数据权限数据提供接口
 * <p>
 * 提供数据权限所需的用户、部门、角色等数据
 *
 * @author Carlos
 * @version 2.0
 */
public interface DataScopeProvider {

    // ==================== 用户相关 ====================

    /**
     * 获取当前用户ID
     *
     * @return 用户ID
     */
    Serializable getCurrentUserId();

    /**
     * 获取当前用户
     *
     * @return 用户对象
     */
    Object getCurrentUser();

    /**
     * 获取用户及下属用户ID集合
     *
     * @param userId 用户ID
     * @return 用户ID集合（包含本人）
     */
    Set<Serializable> getUserAndSubordinateIds(Serializable userId);

    // ==================== 部门相关 ====================

    /**
     * 获取当前部门ID
     *
     * @return 部门ID
     */
    Serializable getCurrentDeptId();

    /**
     * 获取当前部门
     *
     * @return 部门对象
     */
    Object getCurrentDept();

    /**
     * 获取部门的所有子部门ID
     *
     * @param deptId 部门ID
     * @return 子部门ID集合
     */
    Set<Serializable> getDeptChildrenIds(Serializable deptId);

    /**
     * 获取部门树（指定层级）
     *
     * @param deptId    部门ID
     * @param maxLevel  最大层级
     * @return 部门ID集合
     */
    Set<Serializable> getDeptTreeIds(Serializable deptId, int maxLevel);

    // ==================== 角色相关 ====================

    /**
     * 获取当前角色ID集合
     *
     * @return 角色ID集合
     */
    Set<Serializable> getCurrentRoleIds();

    /**
     * 获取当前角色
     *
     * @return 角色对象集合
     */
    Set<Object> getCurrentRoles();

    /**
     * 获取角色继承的角色ID集合
     *
     * @param roleId 角色ID
     * @return 继承的角色ID集合
     */
    Set<Serializable> getInheritedRoleIds(Serializable roleId);

    // ==================== 区域相关 ====================

    /**
     * 获取当前区域编码
     *
     * @return 区域编码
     */
    String getCurrentRegionCode();

    /**
     * 获取区域的所有子区域编码
     *
     * @param regionCode 区域编码
     * @return 子区域编码集合
     */
    Set<String> getRegionChildrenCodes(String regionCode);
}
