package com.carlos.integration.module.dingtalk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 创建部门响应
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DingtalkDeptCreateResponse extends DingtalkBaseResponse {

    private static final long serialVersionUID = 1L;

    /**
     * 创建结果
     */
    private DeptCreateResponse result;

    /**
     * <p>
     * 部门创建结果
     * </p>
     */
    @Data
    public static class DeptCreateResponse {

        /**
         * 部门ID
         */
        @JsonProperty("dept_id")
        private Long deptId;
    }
}

