package com.carlos.message.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 消息类型 更新参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Data
@Accessors(chain = true)
@Schema(description = "消息类型修改参数")
public class MessageTypeUpdateParam {
    @NotNull(message = "主键ID不能为空")
    @Schema(description = "主键ID")
    private Long id;
    @Size(max = 32, message = "类型编码长度不能超过 32")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_.-]{1,63}$", message = "类型编码须以字母开头，仅允许字母、数字、下划线、中划线、点")
    @Schema(description = "类型编码")
    private String typeCode;
    @Size(max = 32, message = "类型名称长度不能超过 32")
    @Schema(description = "类型名称")
    private String typeName;
    @Schema(description = "是否启用: 0-禁用 1-启用")
    private Boolean enabled;
}
