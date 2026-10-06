package com.carlos.integration.module.dingtalk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * <p>
 * 閽夐拤 Token 鍝嶅簲
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
public class DingtalkTokenResponse {

    /**
     * 杩斿洖鐮?
     */
    private Integer errcode;

    /**
     * 杩斿洖淇℃伅
     */
    private String errmsg;

    /**
     * AccessToken
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * 杩囨湡鏃堕棿锛堢掞?
     */
    @JsonProperty("expires_in")
    private Integer expiresIn;

    /**
     * 鏄鍚︽垚鍔
     */
    public boolean isSuccess() {
        return errcode != null && errcode == 0;
    }
}

