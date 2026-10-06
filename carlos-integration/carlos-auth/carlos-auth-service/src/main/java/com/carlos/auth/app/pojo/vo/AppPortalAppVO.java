package com.carlos.auth.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 应用门户展示对象
 *
 * @author Carlos
 * @date 2026-07-02
 */
@Data
@Accessors(chain = true)
@Schema(description = "应用门户卡片展示数据")
public class AppPortalAppVO {

    @Schema(description = "应用ID")
    private Long id;

    @Schema(description = "应用编号")
    private String appKey;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "应用Logo")
    private String appLogo;

    @Schema(description = "应用描述")
    private String appDesc;

    @Schema(description = "应用类型(1内部应用 2外部应用 3移动应用)")
    private Integer appType;

    @Schema(description = "应用首页地址")
    private String homepageUrl;

    @Schema(description = "展示排序")
    private Integer displayOrder;

    @Schema(description = "分类ID", hidden = true)
    private Long categoryId;
}
