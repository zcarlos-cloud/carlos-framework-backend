package com.carlos.integration.module.dingtalk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>
 * 获取部门详情响应
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DingtalkDeptResponse extends DingtalkBaseResponse {

    private static final long serialVersionUID = 1L;

    /**
     * 部门信息
     */
    private DeptGetResponse result;

    /**
     * <p>
     * 部门信息
     * </p>
     */
    @Data
    public static class DeptGetResponse {

        /**
         * 部门ID
         */
        @JsonProperty("dept_id")
        private Long deptId;

        /**
         * 部门名称
         */
        private String name;

        /**
         * 父部门ID
         */
        @JsonProperty("parent_id")
        private Long parentId;

        /**
         * 是否同步创建一个关联此部门的企业群
         */
        @JsonProperty("create_dept_group")
        private Boolean createDeptGroup;

        /**
         * 部门群ID
         */
        @JsonProperty("dept_group_chat_id")
        private String deptGroupChatId;

        /**
         * 部门主管列表
         */
        @JsonProperty("dept_manager_userid_list")
        private List<String> deptManagerUseridList;
    }
}

