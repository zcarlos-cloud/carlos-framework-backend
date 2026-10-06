package com.carlos.system.menu.pojo.param;

import com.carlos.system.enums.IconType;
import com.carlos.system.enums.MenuCategory;
import com.carlos.system.enums.MenuStatus;
import com.carlos.system.enums.MenuType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 系统菜单 更新参数封装
 * </p>
 *
 * @author carlos
 * @date 2021-12-28 15:26:57
 */
@Data
@Accessors(chain = true)
@Schema(description = "系统菜单修改参数")
public class SysMenuUpdateParam {

    @NotNull(message = "主键不能为空")
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "父级ID")
    private Long parentId;

    @Size(max = 100, message = "菜单名称长度不能超过100")
    @Schema(description = "菜单名称")
    private String menuName;

    @Size(max = 200, message = "路由地址长度不能超过200")
    @Schema(description = "前端路由path")
    private String routePath;

    @Size(max = 100, message = "路由name长度不能超过100")
    @Schema(description = "路由name，前端匹配用")
    private String routeName;

    @Size(max = 100, message = "图标长度不能超过100")
    @Schema(description = "前端图标")
    private String icon;

    @Schema(description = "图标类型")
    private IconType iconType;

    @Size(max = 4000, message = "菜单配置长度不能超过4000")
    @Schema(description = "菜单配置（JSON扩展）")
    private String meta;

    @Size(max = 200, message = "目标组件长度不能超过200")
    @Schema(description = "目标组件")
    private String component;

    @Size(max = 500, message = "请求地址长度不能超过500")
    @Schema(description = "请求地址")
    private String url;

    @Schema(description = "状态")
    private MenuStatus menuStatus;

    @Schema(description = "菜单排序")
    private Integer orderNum;

    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否在菜单中隐藏")
    private Boolean hideInMenu;

    @Schema(description = "菜单类别")
    private MenuCategory menuCategory;

    @Schema(description = "客户端类型")
    private MenuType clientType;

    @Size(max = 100, message = "国际化key长度不能超过100")
    @Schema(description = "国际化key")
    private String i18nKey;

    @Schema(description = "是否缓存页面")
    private Boolean keepAlive;

    @Schema(description = "是否常量路由")
    private Boolean constant;

    @Size(max = 500, message = "外链地址长度不能超过500")
    @Schema(description = "外链地址")
    private String href;

    @Size(max = 100, message = "高亮菜单路由名长度不能超过100")
    @Schema(description = "高亮菜单路由名")
    private String activeMenu;

    @Schema(description = "是否多标签页")
    private Boolean multiTab;

    @Schema(description = "标签页固定索引")
    private Integer fixedIndexInTab;

    @Size(max = 4000, message = "路由query参数长度不能超过4000")
    @Schema(description = "路由query参数（JSON）")
    private String routeQuery;

    // @Valid 级联到元素类型 SysMenuButtonParam，与新增参数同口径 —— 否则更新时
    // 按钮的 @NotBlank(code) 不生效；updateMenu 是「先删光再按传入值重建」，
    // 一旦重建出 code 为空的按钮，该菜单的按钮权限就永久坏掉了
    @Valid
    @Schema(description = "按钮权限列表")
    private List<SysMenuButtonParam> buttons;

}
