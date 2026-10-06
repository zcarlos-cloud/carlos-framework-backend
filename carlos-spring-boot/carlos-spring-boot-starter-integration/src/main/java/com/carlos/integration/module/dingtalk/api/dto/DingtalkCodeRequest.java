package com.carlos.integration.module.dingtalk.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * <p>
 * 閽夐拤 Code 鎹㈠彇鐢ㄦ埛淇℃伅璇锋眰
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DingtalkCodeRequest extends DingtalkBaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 涓存椂鎺堟潈鐮?
     */
    private String code;
}

