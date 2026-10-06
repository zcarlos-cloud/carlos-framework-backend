package com.carlos.auth.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 应用门户分组（按分类聚合）
 *
 * @author Carlos
 * @date 2026-07-02
 */
@Data
@Accessors(chain = true)
@Schema(description = "按分类聚合的应用列表")
public class AppPortalGroupVO {

    @Schema(description = "分类信息")
    private AppPortalCategoryVO category;

    @Schema(description = "该分类下的应用列表")
    private List<AppPortalAppVO> apps;
}
