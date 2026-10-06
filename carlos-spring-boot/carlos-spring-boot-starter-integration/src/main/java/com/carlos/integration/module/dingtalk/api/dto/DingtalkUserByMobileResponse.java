package com.carlos.integration.module.dingtalk.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 鏍规嵁鎵嬫満鍙锋煡璇㈢敤鎴峰搷搴?
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DingtalkUserByMobileResponse extends DingtalkBaseResponse {

    private static final long serialVersionUID = 1L;

    /**
     * 鐢ㄦ埛淇℃伅
     */
    private UserGetByMobileResponse result;

    /**
     * <p>
     * 鎵嬫満鍙锋煡璇㈢敤鎴风粨鏋?
     * </p>
     */
    @Data
    public static class UserGetByMobileResponse {

        /**
         * 鐢ㄦ埛ID
         */
        private String userid;

        /**
         * 鐢ㄦ埛unionId
         */
        private String unionid;
    }
}

