package com.carlos.message.pojo.param;


import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


/**
 * <p>
 * 消息发送日志 列表查询参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "消息发送日志列表查询参数")
public class MessageSendLogPageParam extends ParamPage {
    @Size(max = 64, message = "消息ID长度不能超过 64")
    @Schema(description = "消息ID")
    private String messageId;
    @Schema(description = "接收人记录ID")
    private Long receiverId;
    @Size(max = 32, message = "渠道编码长度不能超过 32")
    @Schema(description = "渠道编码")
    private String channelCode;
    @Size(max = 2000, message = "请求参数长度不能超过 2000")
    @Schema(description = "请求参数")
    private String requestParam;
    @Size(max = 2000, message = "响应数据长度不能超过 2000")
    @Schema(description = "响应数据")
    private String responseData;
    @Schema(description = "是否成功: 0-失败 1-成功")
    private Boolean success;
    @Size(max = 32, message = "错误码长度不能超过 32")
    @Schema(description = "错误码")
    private String errorCode;
    @Size(max = 512, message = "错误信息长度不能超过 512")
    @Schema(description = "错误信息")
    private String errorMessage;
    @Min(value = 0, message = "耗时不能为负")
    @Schema(description = "耗时(ms)")
    private Integer costTime;
    @Schema(description = "开始时间")
    private LocalDateTime start;

    @Schema(description = "结束时间")
    private LocalDateTime end;
}
