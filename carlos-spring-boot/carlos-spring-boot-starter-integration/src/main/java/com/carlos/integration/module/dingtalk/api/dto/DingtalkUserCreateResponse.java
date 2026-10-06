package com.carlos.integration.module.dingtalk.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 创建用户响应
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DingtalkUserCreateResponse extends DingtalkBaseResponse {

    private static final long serialVersionUID = 1L;

    /**
     * 创建结果
     */
    private UserCreateResponse result;

    /**
     * <p>
     * 用户创建结果
     * </p>
     */
    @Data
    public static class UserCreateResponse {

        /**
         * 用户ID
         */
        private String userid;
    }
}

