package com.carlos.system.menu.pojo.vo;

import com.carlos.system.enums.IconType;
import com.carlos.system.enums.MenuCategory;
import com.carlos.system.enums.MenuStatus;
import com.carlos.system.enums.MenuType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 系统菜单 显示层对象，向页面传输的对象
 * </p>
 *
 * @author carlos
 * @date 2021-12-28 15:26:57
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysMenuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "父级ID")
    private Long parentId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "前端路由path")
    private String routePath;

    @Schema(description = "路由name，前端匹配用")
    private String routeName;

    @Schema(description = "前端图标")
    private String icon;

    @Schema(description = "图标类型")
    private IconType iconType;

    @Schema(description = "菜单配置（JSON扩展）")
    private String meta;

    @Schema(description = "目标组件")
    private String component;

    @Schema(description = "请求地址")
    private String url;

    @Schema(description = "状态")
    private MenuStatus menuStatus;

    @Schema(description = "菜单级数")
    private Integer menuLevel;

    @Schema(description = "菜单排序")
    private Integer orderNum;

    @Schema(description = "是否在菜单中隐藏")
    private Boolean hideInMenu;

    @Schema(description = "菜单类别")
    private MenuCategory menuCategory;

    @Schema(description = "客户端类型")
    private MenuType clientType;

    @Schema(description = "国际化key")
    private String i18nKey;

    @Schema(description = "是否缓存页面")
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

    @Schema(description = "路由query参数（JSON）")
    private String routeQuery;

    @Schema(description = "创建人")
    private Long createBy;

    @Schema(description = "修改人")
    private Long updateBy;

    @Schema(description = "按钮权限列表")
    private List<SysMenuButtonVO> buttons;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    private LocalDateTime updateTime;

    @Schema(description = "是否有子菜单")
    private Boolean haveChildren;

}
