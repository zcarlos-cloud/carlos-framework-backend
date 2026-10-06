package com.carlos.devkit.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 单个模板渲染预览结果
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "模板渲染预览结果")
public class TemplateRenderPreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否渲染成功")
    private Boolean success;

    @Schema(description = "渲染结果（成功时）")
    private String content;

    @Schema(description = "错误信息（失败时）")
    private String error;
}
