package com.carlos.message.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 消息模板 更新参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Data
@Accessors(chain = true)
@Schema(description = "消息模板修改参数")
public class MessageTemplateUpdateParam {
    @NotNull(message = "主键ID不能为空")
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "消息类型ID")
    private Long typeId;
    @Size(max = 50, message = "模板编码长度不能超过 50")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_.-]{1,63}$", message = "模板编码须以字母开头，仅允许字母、数字、下划线、中划线、点")
    @Schema(description = "模板编码")
    private String templateCode;
    @Size(max = 50, message = "模板名称长度不能超过 50")
    @Schema(description = "模板名称")
    private String templateName;
    @Size(max = 100, message = "标题模板长度不能超过 100")
    @Schema(description = "标题模板")
    private String titleTemplate;
    @Size(max = 2000, message = "模板内容长度不能超过 2000")
    @Schema(description = "模板内容")
    private String contentTemplate;
    @Size(max = 2000, message = "参数定义JSON长度不能超过 2000")
    @Schema(description = "参数定义JSON")
    private String paramSchema;
    @Size(max = 2000, message = "渠道特殊配置长度不能超过 2000")
    @Schema(description = "渠道特殊配置")
    private String channelConfig;
    @Schema(description = "是否启用: 0-禁用 1-启用 2-草稿")
    private Boolean enabled;
}
