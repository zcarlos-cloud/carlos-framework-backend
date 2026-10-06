package com.carlos.system.i18n.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 国际化消息 新增参数
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
@Data
@Accessors(chain = true)
@Schema(description = "国际化消息新增参数")
public class SysI18nCreateParam {

    @Schema(description = "语言标识")
    @NotBlank(message = "语言标识不能为空")
    @Size(max = 20, message = "语言标识长度不能超过20")
    @Pattern(regexp = "^[a-zA-Z]{2}([-_][a-zA-Z]{2})?$", message = "语言标识格式不正确，示例：zh_CN、en-US")
    private String lang;

    @Schema(description = "所属模块ID")
    private Long moduleId;

    @Schema(description = "消息键")
    @NotBlank(message = "消息键不能为空")
    @Size(max = 200, message = "消息键长度不能超过200")
    private String msgKey;

    @Schema(description = "消息值")
    @NotBlank(message = "消息值不能为空")
    @Size(max = 4000, message = "消息值长度不能超过4000")
    private String msgValue;

    @Schema(description = "描述说明")
    @Size(max = 500, message = "描述说明长度不能超过500")
    private String description;

    @Schema(description = "状态：true启用 false禁用")
    private Boolean msgStatus;

}
