package com.carlos.system.menu.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 路由 VO（对应前端 ElegantConstRoute 格式）
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Data
@Accessors(chain = true)
public class RouteVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜单ID")
    private Long id;

    @Schema(description = "路由name，前端匹配用")
    private String name;

    @Schema(description = "前端路由path")
    private String path;

    @Schema(description = "前端组件路径，如 layout.base$view.manage_user")
    private String component;

    @Schema(description = "路由元数据")
    private RouteMetaVO meta;

    @Schema(description = "子路由")
    private List<RouteVO> children;

    /**
     * <p>
     * 路由元数据 VO
     * </p>
     */
    @Data
    @Accessors(chain = true)
    public static class RouteMetaVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "标题")
        private String title;

        @Schema(description = "国际化key")
        private String i18nKey;

        @Schema(description = "图标")
        private String icon;

        @Schema(description = "排序")
        private Integer orderNum;

        @Schema(description = "是否在菜单中隐藏")
        private Boolean hideInMenu;

        @Schema(description = "是否缓存")
        private Boolean keepAlive;

        @Schema(description = "是否常量路由")
        private Boolean constant;

        @Schema(description = "外链地址")
        private String href;

        @Schema(description = "高亮菜单路由名")
        private String activeMenu;

        @Schema(description = "是否多标签页")
        private Boolean multiTab;

        @Schema(description = "标签页固定索引")
        private Integer fixedIndexInTab;

    }

}
