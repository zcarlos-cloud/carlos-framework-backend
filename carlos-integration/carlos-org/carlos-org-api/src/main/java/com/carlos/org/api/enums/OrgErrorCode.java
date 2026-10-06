package com.carlos.org.api.enums;

import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 组织模块错误码
 * <p>
 * 模块编码：01（用户组织服务）
 * <p>
 * 错误码格式：A-BB-CC
 * <ul>
 *   <li>A - 错误级别：1客户端错误，2业务错误，3第三方错误，5系统错误</li>
 *   <li>BB - 模块编码：01用户组织</li>
 *   <li>CC - 具体错误序号</li>
 * </ul>
 *
 * @author carlos
 * @since 3.0.0
 */
@Getter
@RequiredArgsConstructor
public enum OrgErrorCode implements ErrorCode {

    // ==================== 客户端错误 (1-01-xx) ====================
    ORG_PARAM_USER_ID_EMPTY("10101", "用户ID不能为空", 400),
    ORG_PARAM_DEPT_ID_EMPTY("10102", "部门ID不能为空", 400),
    ORG_PARAM_ROLE_ID_EMPTY("10103", "角色ID不能为空", 400),
    ORG_PARAM_USER_NAME_EMPTY("10104", "用户名不能为空", 400),
    ORG_PARAM_USER_NAME_FORMAT_INVALID("10105", "用户名格式无效", 400),
    ORG_PARAM_PHONE_FORMAT_INVALID("10106", "手机号格式无效", 400),
    ORG_PARAM_EMAIL_FORMAT_INVALID("10107", "邮箱格式无效", 400),

    // ==================== 业务错误 (2-01-xx) ====================
    // 用户相关
    ORG_USER_NOT_FOUND("20101", "用户不存在", 404),
    ORG_USER_ALREADY_EXISTS("20102", "用户已存在", 409),
    ORG_USER_NAME_ALREADY_EXISTS("20103", "用户名已存在", 409),
    ORG_USER_PHONE_ALREADY_EXISTS("20104", "手机号已存在", 409),
    ORG_USER_EMAIL_ALREADY_EXISTS("20105", "邮箱已存在", 409),
    ORG_USER_ACCOUNT_LOCKED("20106", "用户账号已被锁定", 403),
    ORG_USER_ACCOUNT_DISABLED("20107", "用户账号已被禁用", 403),
    ORG_USER_ACCOUNT_EXPIRED("20108", "用户账号已过期", 403),
    ORG_USER_PASSWORD_ERROR("20109", "用户密码错误", 400),
    ORG_USER_PASSWORD_EXPIRED("20110", "用户密码已过期", 400),
    ORG_USER_OLD_PASSWORD_ERROR("20111", "原密码错误", 400),

    // 部门相关
    ORG_DEPT_NOT_FOUND("20121", "部门不存在", 404),
    ORG_DEPT_ALREADY_EXISTS("20122", "部门已存在", 409),
    ORG_DEPT_CODE_ALREADY_EXISTS("20123", "部门编码已存在", 409),
    ORG_DEPT_HAS_CHILDREN("20124", "该部门存在子部门，不能删除", 400),
    ORG_DEPT_HAS_USERS("20125", "该部门下存在用户，不能删除", 400),
    ORG_DEPT_PARENT_NOT_FOUND("20126", "上级部门不存在", 404),
    ORG_DEPT_PARENT_CANNOT_BE_SELF("20127", "上级部门不能是自己", 400),
    ORG_DEPT_PARENT_CANNOT_BE_CHILD("20128", "上级部门不能是自己的子部门", 400),

    // 角色相关
    ORG_ROLE_NOT_FOUND("20141", "角色不存在", 404),
    ORG_ROLE_ALREADY_EXISTS("20142", "角色已存在", 409),
    ORG_ROLE_CODE_ALREADY_EXISTS("20143", "角色编码已存在", 409),
    ORG_ROLE_HAS_USERS("20144", "该角色已分配给用户，不能删除", 400),
    ORG_ROLE_SYSTEM_CANNOT_DELETE("20145", "系统内置角色不能删除", 403),

    // 用户-部门关系
    ORG_USER_DEPT_NOT_FOUND("20161", "用户部门关系不存在", 404),
    ORG_USER_DEPT_ALREADY_EXISTS("20162", "用户已在此部门", 409),
    ORG_USER_MAIN_DEPT_REQUIRED("20163", "用户必须有一个主部门", 400),

    // 用户-角色关系
    ORG_USER_ROLE_NOT_FOUND("20171", "用户角色关系不存在", 404),
    ORG_USER_ROLE_ALREADY_EXISTS("20172", "用户已拥有该角色", 409),

    // 部门-角色关系
    ORG_DEPT_ROLE_NOT_FOUND("20181", "部门角色关系不存在", 404),
    ORG_DEPT_ROLE_ALREADY_EXISTS("20182", "部门已拥有该角色", 409),

    // 租户相关
    ORG_TENANT_NOT_FOUND("20191", "租户不存在", 404),
    ORG_TENANT_CODE_ALREADY_EXISTS("20192", "租户编码已存在", 409),
    ORG_TENANT_DISABLED("20193", "租户已停用", 403),
    ORG_TENANT_EXPIRED("20194", "租户已过期", 403),
    ORG_TENANT_HAS_USERS("20195", "该租户下存在用户，不能删除", 400),
    ORG_TENANT_PLATFORM_NOT_EDITABLE("20196", "平台租户不允许停用或删除", 403),
    ORG_TENANT_ADMIN_ALREADY_EXISTS("20197", "该租户下管理员账号已存在", 409),

    // ==================== 第三方错误 (3-01-xx) ====================
    ORG_SYNC_LDAP_ERROR("30101", "LDAP 同步异常", 500),
    ORG_SYNC_AD_ERROR("30102", "AD 同步异常", 500),
    ORG_SYNC_HR_SYSTEM_ERROR("30103", "HR 系统同步异常", 500),

    // ==================== 系统错误 (5-01-xx) ====================
    ORG_SYSTEM_ERROR("50101", "组织系统内部错误", 500),
    ORG_DATA_INTEGRITY_ERROR("50102", "组织数据完整性错误", 500),
    ORG_DEPT_CACHE_INIT_FAILED("50103", "初始化部门缓存失败", 500);

    private final String code;
    private final String message;
    private final int httpStatus;

    /**
     * 创建业务异常（使用默认消息）
     *
     * @return BusinessException
     */
    @Override
    public BusinessException exception() {
        return new BusinessException(this);
    }

    /**
     * 创建业务异常（自定义消息）
     *
     * @param customMessage 自定义消息
     * @return BusinessException
     */
    @Override
    public BusinessException exception(String customMessage) {
        return new BusinessException(this, customMessage);
    }

    /**
     * 创建业务异常（格式化消息）
     *
     * @param format 格式字符串
     * @param args   参数
     * @return BusinessException
     */
    @Override
    public BusinessException exception(String format, Object... args) {
        return new BusinessException(this, String.format(format, args));
    }
}
