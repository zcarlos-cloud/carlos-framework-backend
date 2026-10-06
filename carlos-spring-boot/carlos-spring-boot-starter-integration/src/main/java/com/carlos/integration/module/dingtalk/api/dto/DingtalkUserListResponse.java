package com.carlos.integration.module.dingtalk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>
 * 鑾峰彇閮ㄩ棬鐢ㄦ埛鍒楄〃鍝嶅簲
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DingtalkUserListResponse extends DingtalkBaseResponse {

    private static final long serialVersionUID = 1L;

    /**
     * 鍒嗛〉缁撴灉
     */
    private PageResult result;

    /**
     * <p>
     * 鍒嗛〉缁撴灉
     * </p>
     */
    @Data
    public static class PageResult {

        /**
         * 鏄鍚﹁繕鏈夋洿澶氭暟鎹
         */
        @JsonProperty("has_more")
        private Boolean hasMore;

        /**
         * 涓嬩竴娆℃父鏍?
         */
        @JsonProperty("next_cursor")
        private Long nextCursor;

        /**
         * 鐢ㄦ埛鍒楄〃
         */
        private List<DingtalkUserResponse.DingtalkUser> list;
    }
}

