package com.carlos.message.pojo.dto;


import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 消息发送日志 数据传输对象，service和manager向外传输对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Data
@Accessors(chain = true)
public class MessageSendLogDTO {
    /** 主键ID */
    private Long id;
    /** 消息ID */
    private String messageId;
    /** 接收人记录ID */
    private Long receiverId;
    /** 渠道编码 */
    private String channelCode;
    /** 请求参数 */
    private String requestParam;
    /** 响应数据 */
    private String responseData;
    /** 是否成功: 0-失败 1-成功 */
    private Boolean success;
    /** 错误码 */
    private String errorCode;
    /** 错误信息 */
    private String errorMessage;
    /** 耗时(ms) */
    private Integer costTime;
    /** 创建时间 */
    private LocalDateTime createTime;
}
