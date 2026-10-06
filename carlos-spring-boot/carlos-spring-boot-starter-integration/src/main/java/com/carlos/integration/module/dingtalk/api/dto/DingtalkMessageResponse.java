package com.carlos.integration.module.dingtalk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 鍙戦佸伐浣滈氱煡娑堟伅鍝嶅簲
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DingtalkMessageResponse extends DingtalkBaseResponse {

    private static final long serialVersionUID = 1L;

    /**
     * 鍙戦佺粨鏋?
     */
    private AsyncSendMessageResponse result;

    /**
     * <p>
     * 寮傛ュ彂閫佹秷鎭缁撴?
     * </p>
     */
    @Data
    public static class AsyncSendMessageResponse {

        /**
         * 娑堟伅浠诲姟ID
         */
        @JsonProperty("task_id")
        private Long taskId;

        /**
         * 鏃犳晥鐨勭敤鎴稩D鍒楄〃
         */
        @JsonProperty("invalid_userid_list")
        private String invalidUseridList;

        /**
         * 鏃犳晥鐨勯儴闂↖D鍒楄〃
         */
        @JsonProperty("invalid_dept_id_list")
        private String invalidDeptIdList;

        /**
         * 鍥犲彂閫佹秷鎭杩囦簬棰戠箒鎴栬秴閲忚岃娴佹帶杩囨护鍚庯紝瀹為檯鏈鍙戦佺殑閮ㄩ棬ID鍒楄〃
         */
        @JsonProperty("forbidden_list")
        private String forbiddenList;

        /**
         * 琚绂佹㈠彂閫佹秷鎭鐨勭敤鎴稩D鍒楄〃
         */
        @JsonProperty("forbidden_user_id_list")
        private String forbiddenUserIdList;
    }
}

