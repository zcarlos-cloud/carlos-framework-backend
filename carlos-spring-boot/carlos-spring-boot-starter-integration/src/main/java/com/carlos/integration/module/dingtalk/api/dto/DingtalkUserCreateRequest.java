package com.carlos.integration.module.dingtalk.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>
 * 鍒涘缓鐢ㄦ埛璇锋眰
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DingtalkUserCreateRequest extends DingtalkBaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 鐢ㄦ埛ID
     */
    private String userid;

    /**
     * 濮撳悕
     */
    private String name;

    /**
     * 鎵嬫満鍙?
     */
    private String mobile;

    /**
     * 閮ㄩ棬ID鍒楄〃
     */
    @JsonProperty("dept_id_list")
    private List<Long> deptIdList;

    /**
     * 鑱屼綅
     */
    private String title;

    /**
     * 閭绠
     */
    private String email;

    /**
     * 鏄鍚﹀紑鍚鐢熸棩鎻愰?
     */
    @JsonProperty("open_birthday_remind")
    private Boolean openBirthdayRemind;
}

