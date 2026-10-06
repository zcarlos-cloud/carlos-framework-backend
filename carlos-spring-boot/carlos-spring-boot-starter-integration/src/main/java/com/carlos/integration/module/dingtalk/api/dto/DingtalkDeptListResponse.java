package com.carlos.integration.module.dingtalk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>
 * 鑾峰彇瀛愰儴闂ㄥ垪琛ㄥ搷搴?
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DingtalkDeptListResponse extends DingtalkBaseResponse {

    private static final long serialVersionUID = 1L;

    /**
     * 閮ㄩ棬鍒楄〃
     */
    private List<DeptBaseResponse> result;

    /**
     * <p>
     * 閮ㄩ棬鍩虹淇℃伅
     * </p>
     */
    @Data
    public static class DeptBaseResponse {

        /**
         * 閮ㄩ棬ID
         */
        @JsonProperty("dept_id")
        private Long deptId;

        /**
         * 閮ㄩ棬鍚嶇О
         */
        private String name;

        /**
         * 鐖堕儴闂↖D
         */
        @JsonProperty("parent_id")
        private Long parentId;

        /**
         * 閮ㄩ棬璺寰
         */
        @JsonProperty("parent_dept_path_list")
        private List<Long> parentDeptPathList;
    }
}

