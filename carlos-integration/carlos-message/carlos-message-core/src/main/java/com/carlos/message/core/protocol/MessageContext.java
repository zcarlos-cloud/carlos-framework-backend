package com.carlos.message.core.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * <p>
 * 消息上下文
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 渠道编码
     */
    private String channelCode;

    /**
     * 接收者ID
     */
    private String receiverId;

    /**
     * 接收者类型
     */
    private Integer receiverType;

    /**
     * 接收地址
     */
    private String receiverNumber;

    /**
     * 消息标题
     */
    private String messageTitle;

    /**
     * 消息内容
     */
    private String messageContent;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 模板参数
     */
    private Map<String, Object> templateParams;

    /**
     * 渠道配置
     */
    private Map<String, Object> channelConfig;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 回调URL
     */
    private String callbackUrl;

    /**
     * 扩展信息
     */
    private Map<String, Object> extras;
}
