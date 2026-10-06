package com.carlos.integration.module.dingtalk.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>
 * 鍒涘缓閮ㄩ棬璇锋眰
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DingtalkDeptCreateRequest extends DingtalkBaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 閮ㄩ棬鍚嶇О锛堝繀濉锛
     */
    private String name;

    /**
     * 鐖堕儴闂↖D锛堝繀濉锛
     */
    @JsonProperty("parent_id")
    private Long parentId;

    /**
     * 鏄鍚﹀悓姝ュ垱寤轰竴涓鍏宠仈姝ら儴闂ㄧ殑浼佷笟缇
     */
    @JsonProperty("create_dept_group")
    private Boolean createDeptGroup;

    /**
     * 鏄鍚﹂殣钘忔湰閮ㄩ?
     */
    private Boolean hideDept;

    /**
     * 閮ㄩ棬涓荤″垪琛
     */
    @JsonProperty("dept_manager_userid_list")
    private List<String> deptManagerUseridList;
}

