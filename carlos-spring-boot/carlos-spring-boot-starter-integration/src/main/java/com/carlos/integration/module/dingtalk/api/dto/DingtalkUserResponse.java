package com.carlos.integration.module.dingtalk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>
 * 閽夐拤鐢ㄦ埛鏌ヨ㈠搷搴
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DingtalkUserResponse extends DingtalkBaseResponse {

    private static final long serialVersionUID = 1L;

    /**
     * 鐢ㄦ埛淇℃伅
     */
    private DingtalkUser result;

    /**
     * <p>
     * 閽夐拤鐢ㄦ埛淇℃伅
     * </p>
     */
    @Data
    public static class DingtalkUser {

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
         * 澶村儚URL
         */
        private String avatar;

        /**
         * 鏄鍚︾＄悊鍛?
         */
        @JsonProperty("admin")
        private Boolean isAdmin;

        /**
         * 鏄鍚﹁佹澘
         */
        @JsonProperty("boss")
        private Boolean isBoss;
    }
}

