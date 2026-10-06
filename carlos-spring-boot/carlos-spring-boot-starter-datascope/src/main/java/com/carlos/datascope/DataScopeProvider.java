package com.carlos.datascope;

import java.io.Serializable;
import java.util.Set;

/**
 * 数据权限，提供数据权限所需的一切数据
 *
 * @author Carlos
 * @date 2022/11/21 12:46
 */
public interface DataScopeProvider {

    /**
     * 获取当前访问用户的Id
     *
     * @return 当前访问用户的id
     * @author carlos
     * @date 2020/6/3 11:16
     */
    Serializable currentUserId();

    /**
     * 当前角色用户id
     *
     * @return java.util.Set<java.io.Serializable>
     * @author Carlos
     * @date 2022/11/23 10:14
     */
    Set<Serializable> currentRoleUserIds();

    /**
     * 当前部门用户id
     *
     * @return java.util.Set<java.io.Serializable>
     * @author Carlos
     * @date 2022/11/23 10:14
     */
    Set<Serializable> currentDeptUserIds();

    /**
     * 当前部门及其子部门用户id
     *
     * @return java.util.Set<java.io.Serializable>
     * @author Carlos
     * @date 2022/11/23 10:14
     */
    Set<Serializable> currentDeptAllUserIds(Serializable departmentId);

    /**
     * 获取统计部门及所有子部门id
     *
     * @return java.util.Set<java.io.Serializable>
     * @author Carlos
     * @date 2022/12/13 14:42
     */
    Set<Serializable> currentDeptAllIds(Serializable departmentId);

    /**
     * 获取当前区域及所有子区域code
     *
     * @return java.util.Set<java.io.Serializable>
     * @author Carlos
     * @date 2022/12/13 14:42
     */
    Set<String> currentRegionAllCodes();
}
