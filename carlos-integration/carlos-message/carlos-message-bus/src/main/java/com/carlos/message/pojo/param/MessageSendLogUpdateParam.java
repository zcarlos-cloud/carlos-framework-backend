package com.carlos.message.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 消息发送日志 更新参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Data
@Accessors(chain = true)
@Schema(description = "消息发送日志修改参数")
public class MessageSendLogUpdateParam {
    @NotNull(message = "主键ID不能为空")
    @Schema(description = "主键ID")
    private Long id;
    @Size(max = 64, message = "消息ID长度不能超过 64")
    @Schema(description = "消息ID")
    private String messageId;
    @Schema(description = "接收人记录ID")
    private Long receiverId;
    @Size(max = 32, message = "渠道编码长度不能超过 32")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_.-]{1,63}$", message = "渠道编码须以字母开头，仅允许字母、数字、下划线、中划线、点")
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
}
