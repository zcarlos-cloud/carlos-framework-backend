package com.carlos.core.auth;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * <p>
 * 用户上下文信息
 * </p>
 *
 * @author Carlos
 * @date 2022/11/14 17:56
 */
@Data
@JsonInclude(Include.NON_NULL)
public class UserContext implements Serializable {

    /**
     * token
     */
    private String token;

    /**
     * 用户id
     */
    private Serializable userId;

    /**
     * 角色id
     */
    private Serializable roleId;

    /**
     * 角色id，适用于多角色场景
     */
    private Set<Serializable> roleIds;

    /**
     * 账号名
     */
    private String account;


    /**
     * 电话
     */
    private String phone;

    /**
     * 租户id
     */
    private Serializable tenantId;

    /**
     * clientId
     */
    private Serializable clientId;

    /**
     * 部门id
     */
    private Serializable departmentId;

    /**
     * 部门id(适用于多部门)
     */
    private Set<Serializable> departmentIds;

    /**
     * 权限列表（如：user:read, user:write, order:delete）
     */
    private Set<String> permissions;

    /**
     * 获取用户ID（字符串形式）
     */
    public String getUserId() {
        return userId != null ? userId.toString() : null;
    }

    /**
     * 获取租户ID（Long形式）
     */
    public Long getTenantId() {
        return tenantId != null ? Long.valueOf(tenantId.toString()) : null;
    }

    /**
     * 获取部门ID（Long形式）
     */
    public Long getDepartmentId() {
        return departmentId != null ? Long.valueOf(departmentId.toString()) : null;
    }

}
