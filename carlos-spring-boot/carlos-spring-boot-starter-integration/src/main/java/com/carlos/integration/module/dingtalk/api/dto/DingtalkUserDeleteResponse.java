package com.carlos.integration.module.dingtalk.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 删除用户响应
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DingtalkUserDeleteResponse extends DingtalkBaseResponse {

    private static final long serialVersionUID = 1L;
}

