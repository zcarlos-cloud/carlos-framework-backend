package com.carlos.integration.module.dingtalk.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 钉钉基础请求
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class DingtalkBaseRequest implements Serializable {

    private static final long serialVersionUID = 1L;
}

