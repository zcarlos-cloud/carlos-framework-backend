package com.carlos.integration.module.dingtalk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * <p>
 * 删除部门请求
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DingtalkDeptDeleteRequest extends DingtalkBaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 部门ID（必填）
     */
    @JsonProperty("dept_id")
    private Long deptId;
}

