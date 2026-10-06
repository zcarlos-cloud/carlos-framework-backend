package com.carlos.datascope.core.model;

import lombok.Getter;

/**
 * 数据权限维度枚举
 * <p>
 * 定义数据权限控制的不同维度，支持用户、部门、角色、区域等多个维度
 *
 * @author Carlos
 * @version 2.0
 */
@Getter
public enum ScopeDimension {

    /**
     * 自动推断 - 根据上下文自动选择合适的权限维度
     * <p>优先级：DEPT_AND_CHILDREN > CURRENT_USER > NONE
     */
    AUTO("自动推断", "根据上下文自动选择合适的权限维度"),

    // ==================== 用户维度 ====================

    /**
     * 仅本人 - 只能查看本人创建的数据
     */
    CURRENT_USER("仅本人", "只能查看本人创建的数据"),

    /**
     * 本人及下属 - 查看本人及直属下属的数据
     */
    USER_AND_SUBORDINATE("本人及下属", "查看本人及直属下属的数据"),

    // ==================== 部门维度 ====================

    /**
     * 本部门 - 只能查看本部门的数据
     */
    CURRENT_DEPT("本部门", "只能查看本部门的数据"),

    /**
     * 本部门及子部门 - 查看本部门及所有子部门的数据
     */
    DEPT_AND_CHILDREN("本部门及子部门", "查看本部门及所有子部门的数据"),

    /**
     * 部门树 - 可配置层级的部门树数据
     */
    DEPT_TREE("部门树", "可配置层级的部门树数据"),

    // ==================== 角色维度 ====================

    /**
     * 本角色 - 只能查看本角色相关的数据
     */
    CURRENT_ROLE("本角色", "只能查看本角色相关的数据"),

    /**
     * 角色及继承 - 本角色及继承角色的数据
     */
    ROLE_AND_INHERIT("角色及继承", "本角色及继承角色的数据"),

    // ==================== 区域维度 ====================

    /**
     * 本区域 - 只能查看本区域的数据
     */
    CURRENT_REGION("本区域", "只能查看本区域的数据"),

    /**
     * 本区域及子区域 - 查看本区域及子区域的数据
     */
    REGION_AND_CHILDREN("本区域及子区域", "查看本区域及子区域的数据"),

    // ==================== 数据范围维度 ====================

    /**
     * 数据范围 - 指定的数据范围（如：只查看某时间段内的数据）
     */
    DATA_RANGE("数据范围", "指定的数据范围"),

    // ==================== 特殊维度 ====================

    /**
     * 全部数据 - 查看所有数据，不受限制
     */
    ALL("全部数据", "查看所有数据，不受限制"),

    /**
     * 无权限 - 无任何数据权限
     */
    NONE("无权限", "无任何数据权限"),

    /**
     * 自定义 - 通过自定义表达式或处理器实现
     */
    CUSTOM("自定义", "通过自定义表达式或处理器实现");

    private final String label;
    private final String description;

    ScopeDimension(String label, String description) {
        this.label = label;
        this.description = description;
    }

    /**
     * 是否需要用户上下文
     *
     * @return true 需要用户登录
     */
    public boolean requireUserContext() {
        return this != ALL && this != NONE && this != CUSTOM;
    }

    /**
     * 是否属于部门维度
     *
     * @return true 是部门维度
     */
    public boolean isDeptDimension() {
        return this == CURRENT_DEPT || this == DEPT_AND_CHILDREN || this == DEPT_TREE;
    }

    /**
     * 是否属于用户维度
     *
     * @return true 是用户维度
     */
    public boolean isUserDimension() {
        return this == CURRENT_USER || this == USER_AND_SUBORDINATE;
    }

    /**
     * 是否属于角色维度
     *
     * @return true 是角色维度
     */
    public boolean isRoleDimension() {
        return this == CURRENT_ROLE || this == ROLE_AND_INHERIT;
    }

    /**
     * 是否属于区域维度
     *
     * @return true 是区域维度
     */
    public boolean isRegionDimension() {
        return this == CURRENT_REGION || this == REGION_AND_CHILDREN;
    }

    /**
     * 是否需要查询关联数据
     *
     * @return true 需要查询关联数据（如子部门、子区域等）
     */
    public boolean requireRelationQuery() {
        return this == DEPT_AND_CHILDREN || this == DEPT_TREE
            || this == REGION_AND_CHILDREN || this == ROLE_AND_INHERIT
            || this == USER_AND_SUBORDINATE;
    }
}
