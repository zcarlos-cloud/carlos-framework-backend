package com.carlos.message.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;


/**
 * <p>
 * 消息渠道配置 新增参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Data
@Accessors(chain = true)
@Schema(description = "消息渠道配置新增参数")
public class MessageChannelCreateParam {
    @NotBlank(message = "渠道编码不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_.-]{1,63}$", message = "渠道编码须以字母开头，仅允许字母、数字、下划线、中划线、点")
    @Size(max = 32, message = "渠道编码长度不能超过 32")
    @Schema(description = "渠道编码")
    private String channelCode;
    @NotBlank(message = "渠道名称不能为空")
    @Size(max = 32, message = "渠道名称长度不能超过 32")
    @Schema(description = "渠道名称")
    private String channelName;
    @NotNull(message = "渠道类型: 1-短信 2-邮件 3-钉钉 4-企业微信 5-站内信不能为空")
    @Min(value = 1, message = "渠道类型取值 1-5")
    @Max(value = 5, message = "渠道类型取值 1-5")
    @Schema(description = "渠道类型: 1-短信 2-邮件 3-钉钉 4-企业微信 5-站内信")
    private Integer channelType;
    @Size(max = 32, message = "服务商编码长度不能超过 32")
    @Schema(description = "服务商编码")
    private String provider;
    @NotBlank(message = "渠道配置JSON不能为空")
    @Size(max = 2000, message = "渠道配置JSON长度不能超过 2000")
    @Schema(description = "渠道配置JSON")
    private String channelConfig;
    @Min(value = 1, message = "每秒最大请求数不能小于 1")
    @Schema(description = "每秒最大请求数")
    private Integer rateLimitQps;
    @Min(value = 1, message = "突发流量限制不能小于 1")
    @Schema(description = "突发流量限制")
    private Integer rateLimitBurst;
    @Min(value = 0, message = "最大重试次数不能为负")
    @Max(value = 10, message = "最大重试次数不能超过 10")
    @Schema(description = "最大重试次数")
    private Integer retryTimes;
    @Min(value = 0, message = "重试间隔不能为负")
    @Schema(description = "重试间隔(ms)")
    private Integer retryInterval;
    @Min(value = 0, message = "权重不能为负")
    @Schema(description = "权重")
    private Integer weight;
    @NotNull(message = "是否启用: 0-禁用 1-启用 2-故障不能为空")
    @Min(value = 0, message = "是否启用取值 0-2")
    @Max(value = 2, message = "是否启用取值 0-2")
    @Schema(description = "是否启用: 0-禁用 1-启用 2-故障")
    private Integer enabled;
}
