package com.carlos.system.menu.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.carlos.system.enums.IconType;
import com.carlos.system.enums.MenuCategory;
import com.carlos.system.enums.MenuStatus;
import com.carlos.system.enums.MenuType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 系统菜单 数据源对象
 * </p>
 *
 * @author carlos
 * @date 2021-12-28 15:26:57
 */
@Data
@Accessors(chain = true)
@TableName("sys_menu")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 父级ID
     */
    @TableField(value = "parent_id")
    private Long parentId;
    /**
     * 菜单名称
     */
    @TableField(value = "menu_name")
    private String menuName;
    /**
     * 前端路由path
     */
    @TableField(value = "route_path")
    private String routePath;
    /**
     * 路由name，前端匹配用
     */
    @TableField(value = "route_name")
    private String routeName;
    /**
     * 前端图标
     */
    @TableField(value = "icon")
    private String icon;
    /**
     * 图标类型：1=iconify, 2=local
     */
    @TableField(value = "icon_type")
    private IconType iconType;
    /**
     * 菜单配置（JSON扩展）
     */
    @TableField(value = "meta")
    private String meta;
    /**
     * 目标组件
     */
    @TableField(value = "component")
    private String component;
    /**
     * 请求地址
     */
    @TableField(value = "url")
    private String url;
    /**
     * 状态：1=启用, 2=禁用
     */
    @TableField(value = "menu_status")
    private MenuStatus menuStatus;
    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;
    /**
     * 菜单级数
     */
    @TableField(value = "menu_level")
    private Integer menuLevel;
    /**
     * 菜单排序
     */
    @TableField(value = "order_num")
    private Integer orderNum;
    /**
     * 是否在菜单中隐藏
     */
    @TableField(value = "hide_in_menu")
    private Boolean hideInMenu;
    /**
     * 菜单类别：1=目录, 2=菜单
     */
    @TableField(value = "menu_category")
    private MenuCategory menuCategory;
    /**
     * 客户端类型：PC/MOBILE/ALL
     */
    @TableField(value = "client_type")
    private MenuType clientType;
    /**
     * 国际化key
     */
    @TableField(value = "i18n_key")
    private String i18nKey;
    /**
     * 是否缓存页面
     */
    @TableField(value = "keep_alive")
    private Boolean keepAlive;
    /**
     * 是否常量路由
     */
    @TableField(value = "constant")
    private Boolean constant;
    /**
     * 外链地址
     */
    @TableField(value = "href")
    private String href;
    /**
     * 高亮菜单路由名
     */
    @TableField(value = "active_menu")
    private String activeMenu;
    /**
     * 是否多标签页
     */
    @TableField(value = "multi_tab")
    private Boolean multiTab;
    /**
     * 标签页固定索引
     */
    @TableField(value = "fixed_index_in_tab")
    private Integer fixedIndexInTab;
    /**
     * 路由query参数（JSON）
     */
    @TableField(value = "route_query")
    private String routeQuery;
    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;
    /**
     * 修改人
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private Long updateBy;
    /**
     * 逻辑删除
     */
    @TableLogic
    @TableField(value = "is_deleted")
    private Boolean deleted;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
