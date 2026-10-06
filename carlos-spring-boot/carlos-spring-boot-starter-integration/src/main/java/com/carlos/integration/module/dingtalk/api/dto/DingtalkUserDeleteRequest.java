package com.carlos.integration.module.dingtalk.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * <p>
 * 删除用户请求
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DingtalkUserDeleteRequest extends DingtalkBaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（必填）
     */
    private String userid;
}

