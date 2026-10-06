package com.carlos.message.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 消息发送日志 显示层对象，向页面传输的对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:06
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageSendLogVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "消息ID")
    private String messageId;
    @Schema(description = "接收人记录ID")
    private Long receiverId;
    @Schema(description = "渠道编码")
    private String channelCode;
    @Schema(description = "请求参数")
    private String requestParam;
    @Schema(description = "响应数据")
    private String responseData;
    @Schema(description = "是否成功: 0-失败 1-成功")
    private Boolean success;
    @Schema(description = "错误码")
    private String errorCode;
    @Schema(description = "错误信息")
    private String errorMessage;
    @Schema(description = "耗时(ms)")
    private Integer costTime;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
