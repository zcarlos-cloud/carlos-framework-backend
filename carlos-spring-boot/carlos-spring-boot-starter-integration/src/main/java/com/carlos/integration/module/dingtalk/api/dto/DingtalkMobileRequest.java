package com.carlos.integration.module.dingtalk.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * <p>
 * 鏍规嵁鎵嬫満鍙锋煡璇㈢敤鎴疯锋?
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DingtalkMobileRequest extends DingtalkBaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 鎵嬫満鍙?
     */
    private String mobile;
}

