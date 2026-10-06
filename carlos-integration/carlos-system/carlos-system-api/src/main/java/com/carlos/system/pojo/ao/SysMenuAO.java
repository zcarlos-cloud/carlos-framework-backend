package com.carlos.system.pojo.ao;

import com.carlos.system.enums.IconType;
import com.carlos.system.enums.MenuCategory;
import com.carlos.system.enums.MenuStatus;
import com.carlos.system.enums.MenuType;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 系统菜单 API传输对象
 *
 * @author carlos
 * @date 2021-12-28 15:26:57
 */
@Data
@Accessors(chain = true)
public class SysMenuAO {

    /**
     * 主键
     */
    private Long id;
    /**
     * 父级ID
     */
    private Long parentId;
    /**
     * 菜单名称
     */
    private String menuName;
    /**
     * 前端路由path
     */
    private String routePath;
    /**
     * 路由name，前端匹配用
     */
    private String routeName;
    /**
     * 前端图标
     */
    private String icon;
    /**
     * 图标类型
     */
    private IconType iconType;
    /**
     * 菜单配置（JSON扩展）
     */
    private String meta;
    /**
     * 目标组件
     */
    private String component;
    /**
     * 请求地址
     */
    private String url;
    /**
     * 状态
     */
    private MenuStatus menuStatus;
    /**
     * 菜单级数
     */
    private Integer menuLevel;
    /**
     * 菜单排序
     */
    private Integer orderNum;
    /**
     * 是否在菜单中隐藏
     */
    private Boolean hideInMenu;
    /**
     * 备注
     */
    private String remark;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
    /**
     * 子菜单
     */
    private List<SysMenuAO> children;
    /**
     * 菜单类别
     */
    private MenuCategory menuCategory;
    /**
     * 客户端类型
     */
    private MenuType clientType;
    /**
     * 国际化key
     */
    private String i18nKey;
    /**
     * 是否缓存页面
     */
    private Boolean keepAlive;
    /**
     * 是否常量路由
     */
    private Boolean constant;
    /**
     * 外链地址
     */
    private String href;
    /**
     * 高亮菜单路由名
     */
    private String activeMenu;
    /**
     * 是否多标签页
     */
    private Boolean multiTab;
    /**
     * 标签页固定索引
     */
    private Integer fixedIndexInTab;
    /**
     * 路由query参数（JSON）
     */
    private String routeQuery;
    /**
     * 创建人
     */
    private Long createBy;
    /**
     * 修改人
     */
    private Long updateBy;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final SysMenuAO menuAO = (SysMenuAO) o;
        return this.id.equals(menuAO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
