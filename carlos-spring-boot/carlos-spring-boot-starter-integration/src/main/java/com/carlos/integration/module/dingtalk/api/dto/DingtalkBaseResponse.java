package com.carlos.integration.module.dingtalk.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 閽夐拤鍩虹鍝嶅簲
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class DingtalkBaseResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 杩斿洖鐮?
     */
    private Integer errcode;

    /**
     * 杩斿洖淇℃伅
     */
    private String errmsg;

    /**
     * 璇锋眰ID
     */
    private String requestId;

    /**
     * 鏄鍚︽垚鍔
     */
    @JsonIgnore
    public boolean isSuccess() {
        return errcode != null && errcode == 0;
    }
}

