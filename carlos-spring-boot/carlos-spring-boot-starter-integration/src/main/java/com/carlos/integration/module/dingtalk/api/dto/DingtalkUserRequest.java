package com.carlos.integration.module.dingtalk.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 钉钉用户查询请求
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DingtalkUserRequest extends DingtalkBaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private String userid;

    /**
     * 语言
     */
    private String language = "zh_CN";
}

