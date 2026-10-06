package com.carlos.datascope;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * <p>
 * 数据权限类型
 * </p>
 *
 * @author Carlos
 * @date 2022/11/21 13:01
 */
@Getter
@AllArgsConstructor
public enum DataScopeType {

    /**
     *
     */
    NONE("无数据权限"),
    ALL("全部数据权限"),
    CURRENT_DEPT("本部门数据权限"),
    DEPT_AND_SUB("本部门及以下数据权限"),
    CURRENT_ROLE("本岗位数据权限"),
    CURRENT_USER("仅本人数据权限"),

    CUSTOM("自定义数据权限"),
    ;

    private final String info;


}
