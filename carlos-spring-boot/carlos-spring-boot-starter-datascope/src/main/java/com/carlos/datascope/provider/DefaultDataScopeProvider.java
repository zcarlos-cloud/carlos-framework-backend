package com.carlos.datascope.provider;

import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 默认数据权限提供实现
 * <p>
 * 提供基础的数据提供实现，实际项目中需要扩展
 *
 * @author Carlos
 * @version 2.0
 */
@Slf4j
public class DefaultDataScopeProvider implements DataScopeProvider {

    @Override
    public Serializable getCurrentUserId() {
        // TODO: 从SecurityContext获取当前用户ID
        log.warn("DefaultDataScopeProvider.getCurrentUserId() not implemented");
        return null;
    }

    @Override
    public Object getCurrentUser() {
        // TODO: 从SecurityContext获取当前用户
        return null;
    }

    @Override
    public Set<Serializable> getUserAndSubordinateIds(Serializable userId) {
        // TODO: 查询用户及下属
        Set<Serializable> result = new HashSet<>();
        result.add(userId);
        return result;
    }

    @Override
    public Serializable getCurrentDeptId() {
        // TODO: 从SecurityContext获取当前部门ID
        return null;
    }

    @Override
    public Object getCurrentDept() {
        // TODO: 从SecurityContext获取当前部门
        return null;
    }

    @Override
    public Set<Serializable> getDeptChildrenIds(Serializable deptId) {
        // TODO: 查询子部门
        return Collections.emptySet();
    }

    @Override
    public Set<Serializable> getDeptTreeIds(Serializable deptId, int maxLevel) {
        // TODO: 查询部门树
        return Collections.emptySet();
    }

    @Override
    public Set<Serializable> getCurrentRoleIds() {
        // TODO: 从SecurityContext获取当前角色ID
        return Collections.emptySet();
    }

    @Override
    public Set<Object> getCurrentRoles() {
        // TODO: 从SecurityContext获取当前角色
        return Collections.emptySet();
    }

    @Override
    public Set<Serializable> getInheritedRoleIds(Serializable roleId) {
        // TODO: 查询继承的角色
        return Collections.emptySet();
    }

    @Override
    public String getCurrentRegionCode() {
        // TODO: 从SecurityContext获取当前区域编码
        return null;
    }

    @Override
    public Set<String> getRegionChildrenCodes(String regionCode) {
        // TODO: 查询子区域
        return Collections.emptySet();
    }
}
