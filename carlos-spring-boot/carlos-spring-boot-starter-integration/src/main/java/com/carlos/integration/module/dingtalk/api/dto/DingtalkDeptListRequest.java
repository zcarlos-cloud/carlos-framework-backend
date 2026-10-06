package com.carlos.integration.module.dingtalk.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * <p>
 * 鑾峰彇瀛愰儴闂ㄥ垪琛ㄨ锋?
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DingtalkDeptListRequest extends DingtalkBaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 鐖堕儴闂↖D锛堝～1琛ㄧず鏍归儴闂锛
     */
    private Long deptId = 1L;

    /**
     * 璇瑷
     */
    private String language = "zh_CN";
}

