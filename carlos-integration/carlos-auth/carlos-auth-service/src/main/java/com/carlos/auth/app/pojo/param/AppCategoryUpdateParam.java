package com.carlos.auth.app.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 应用分类 更新参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026-07-02
 */
@Data
@Accessors(chain = true)
@Schema(description = "应用分类更新参数")
public class AppCategoryUpdateParam {

    @NotNull(message = "主键不能为空")
    @Schema(description = "主键")
    private Long id;

    @Size(max = 64, message = "分类编码长度不能超过64")
    @Schema(description = "分类编码")
    private String categoryCode;

    @Size(max = 64, message = "分类名称长度不能超过64")
    @Schema(description = "分类名称")
    private String categoryName;

    @Size(max = 255, message = "分类图标长度不能超过255")
    @Schema(description = "分类图标")
    private String categoryIcon;

    @Schema(description = "排序")
    private Integer sortOrder;
}
