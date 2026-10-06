package com.carlos.integration.module.dingtalk.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 钉钉用户信息响应
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DingtalkUserInfoResponse extends DingtalkBaseResponse {

    private static final long serialVersionUID = 1L;

    /**
     * 用户信息
     */
    private UserGetByCodeResponse result;

    /**
     * <p>
     * 用户 Code 换取结果
     * </p>
     */
    @Data
    public static class UserGetByCodeResponse {

        /**
         * 用户ID
         */
        private String userid;

        /**
         * 用户unionId
         */
        private String unionid;

        /**
         * 设备ID
         */
        private String deviceId;
    }
}

