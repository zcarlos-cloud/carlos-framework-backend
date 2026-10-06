package com.carlos.integration.module.dingtalk.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 发送工作通知消息请求
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DingtalkMessageRequest extends DingtalkBaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 应用AgentID
     */
    @JsonProperty("agent_id")
    private Long agentId;

    /**
     * 接收者的用户ID列表
     */
    @JsonProperty("userid_list")
    private String useridList;

    /**
     * 接收者的部门ID列表
     */
    @JsonProperty("dept_id_list")
    private String deptIdList;

    /**
     * 是否发送给企业全部用户
     */
    @JsonProperty("to_all_user")
    private Boolean toAllUser = false;

    /**
     * 消息内容
     */
    private Msg msg;

    /**
     * <p>
     * 消息内容
     * </p>
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Msg {

        /**
         * 消息类型
         */
        private String msgtype;

        /**
         * 文本消息
         */
        private Text text;

        /**
         * 图片消息
         */
        private Image image;

        /**
         * 链接消息
         */
        private Link link;

        /**
         * Markdown消息
         */
        private Markdown markdown;

        /**
         * 文件消息
         */
        private File file;

        /**
         * OA消息
         */
        private OA oa;

        @Data
        public static class Text {
            private String content;
        }

        @Data
        public static class Image {
            @JsonProperty("media_id")
            private String mediaId;
        }

        @Data
        public static class Link {
            private String title;
            private String text;
            @JsonProperty("picUrl")
            private String picUrl;
            @JsonProperty("messageUrl")
            private String messageUrl;
        }

        @Data
        public static class Markdown {
            private String title;
            private String text;
        }

        @Data
        public static class File {
            @JsonProperty("media_id")
            private String mediaId;
        }

        @Data
        public static class OA {
            private String title;
            private String content;
            @JsonProperty("message_url")
            private String messageUrl;
        }
    }
}

