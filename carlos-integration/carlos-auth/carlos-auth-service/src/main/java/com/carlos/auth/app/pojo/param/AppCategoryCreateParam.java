package com.carlos.auth.app.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 应用分类 新增参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026-07-02
 */
@Data
@Accessors(chain = true)
@Schema(description = "应用分类新增参数")
public class AppCategoryCreateParam {

    @NotBlank(message = "分类编码不能为空")
    @Size(max = 64, message = "分类编码长度不能超过64")
    @Schema(description = "分类编码")
    private String categoryCode;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 64, message = "分类名称长度不能超过64")
    @Schema(description = "分类名称")
    private String categoryName;

    @Size(max = 255, message = "分类图标长度不能超过255")
    @Schema(description = "分类图标")
    private String categoryIcon;

    @Schema(description = "排序")
    private Integer sortOrder = 0;
}
