package com.carlos.auth.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 应用门户分类展示对象
 *
 * @author Carlos
 * @date 2026-07-02
 */
@Data
@Accessors(chain = true)
@Schema(description = "应用门户分类")
public class AppPortalCategoryVO {

    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "分类编码")
    private String code;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "分类图标")
    private String icon;
}
