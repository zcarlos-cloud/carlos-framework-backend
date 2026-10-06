package com.carlos.auth.app.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 应用分类 显示层对象，向页面传输的对象
 * </p>
 *
 * @author Carlos
 * @date 2026-07-02
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "应用分类视图对象")
public class AppCategoryVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "分类编码")
    private String categoryCode;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "分类图标")
    private String categoryIcon;

    @Schema(description = "排序")
    private Integer sortOrder;
}
