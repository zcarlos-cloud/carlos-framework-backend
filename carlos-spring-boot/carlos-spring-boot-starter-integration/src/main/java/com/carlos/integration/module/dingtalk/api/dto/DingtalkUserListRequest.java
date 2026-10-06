package com.carlos.integration.module.dingtalk.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 获取部门用户列表请求
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DingtalkUserListRequest extends DingtalkBaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 分页游标
     */
    private Long cursor = 0L;

    /**
     * 分页大小
     */
    private Long size = 100L;

    /**
     * 语言
     */
    private String language = "zh_CN";
}

