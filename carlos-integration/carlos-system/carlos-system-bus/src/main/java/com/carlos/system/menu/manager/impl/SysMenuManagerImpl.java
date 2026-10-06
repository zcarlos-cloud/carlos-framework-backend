package com.carlos.system.menu.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.system.enums.MenuCategory;
import com.carlos.system.enums.MenuStatus;
import com.carlos.system.enums.MenuType;
import com.carlos.system.menu.convert.SysMenuConvert;
import com.carlos.system.menu.manager.SysMenuManager;
import com.carlos.system.menu.mapper.SysMenuMapper;
import com.carlos.system.menu.pojo.dto.SysMenuDTO;
import com.carlos.system.menu.pojo.entity.SysMenu;
import com.carlos.system.menu.pojo.param.SysMenuPageParam;
import com.carlos.system.menu.pojo.vo.SysMenuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 系统菜单 查询封装实现
 * </p>
 *
 * @author carlos
 * @date 2021-12-28 15:26:57
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SysMenuManagerImpl extends BaseServiceImpl<SysMenuMapper, SysMenu> implements SysMenuManager {

    @Override
    public boolean add(SysMenuDTO dto) {
        SysMenu entity = SysMenuConvert.INSTANCE.toDO(dto);
        boolean success = this.save(entity);
        if (!success) {
            log.warn("Insert menu data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        log.debug("Insert menu data: id:{}", entity.getId());
        return true;
    }

    @Override
    public boolean delete(Serializable id) {
        if (id == null) {
            log.warn("id can't be null");
            return false;
        }
        boolean success = this.removeById(id);
        if (!success) {
            log.warn("Remove menu data fail, id:{}", id);
            return false;
        }
        log.debug("Remove menu data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(SysMenuDTO dto) {
        SysMenu entity = SysMenuConvert.INSTANCE.toDO(dto);
        boolean success = this.updateById(entity);
        if (!success) {
            log.warn("Update menu data fail, entity:{}", entity);
            return false;
        }
        log.debug("Update menu data by id:{}", dto.getId());
        return true;
    }

    @Override
    public SysMenuDTO getDtoById(Serializable id) {
        SysMenu entity = this.lambdaQuery()
            .select(SysMenu::getId, SysMenu::getParentId, SysMenu::getMenuName, SysMenu::getRoutePath,
                SysMenu::getRouteName, SysMenu::getIcon, SysMenu::getIconType, SysMenu::getUrl,
                SysMenu::getMenuStatus, SysMenu::getRemark, SysMenu::getOrderNum, SysMenu::getMeta,
                SysMenu::getComponent, SysMenu::getMenuLevel, SysMenu::getHideInMenu, SysMenu::getMenuCategory,
                SysMenu::getClientType, SysMenu::getI18nKey, SysMenu::getKeepAlive, SysMenu::getConstant,
                SysMenu::getHref, SysMenu::getActiveMenu, SysMenu::getMultiTab, SysMenu::getFixedIndexInTab,
                SysMenu::getRouteQuery, SysMenu::getCreateBy, SysMenu::getUpdateBy,
                SysMenu::getCreateTime, SysMenu::getUpdateTime)
            .eq(SysMenu::getId, id)
            .one();
        return SysMenuConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<SysMenuVO> getPage(SysMenuPageParam param) {
        LambdaQueryWrapper<SysMenu> wrapper = this.queryWrapper();
        wrapper.orderByDesc(SysMenu::getCreateTime);
        wrapper.select(SysMenu::getId, SysMenu::getParentId, SysMenu::getMenuName, SysMenu::getRoutePath,
                SysMenu::getRouteName, SysMenu::getIcon, SysMenu::getIconType, SysMenu::getMeta,
                SysMenu::getComponent, SysMenu::getMenuLevel, SysMenu::getOrderNum, SysMenu::getHideInMenu,
                SysMenu::getMenuStatus, SysMenu::getMenuCategory, SysMenu::getClientType, SysMenu::getI18nKey,
                SysMenu::getKeepAlive, SysMenu::getConstant, SysMenu::getHref, SysMenu::getActiveMenu,
                SysMenu::getMultiTab, SysMenu::getFixedIndexInTab, SysMenu::getRouteQuery,
                SysMenu::getCreateBy, SysMenu::getUpdateBy, SysMenu::getCreateTime, SysMenu::getUpdateTime)
            .eq(param.getParentId() != null, SysMenu::getParentId, param.getParentId());
        if (param.getClientType() != null) {
            // client_type = ALL 的菜单在所有端均展示，因此按 PC/MOBILE/MANAGE 过滤时
            // 必须一并命中 ALL，否则（当前所有菜单的 client_type 都是 ALL）该过滤条件
            // 恒为空结果
            wrapper.in(SysMenu::getClientType, param.getClientType(), MenuType.ALL);
        }
        if (StringUtils.isNotBlank(param.getKeyword())) {
            // 必须用 and(...) 包裹：mp 的 or() 是与前一个条件平级拼接的，
            // 而 SQL 中 AND 优先级高于 OR，直接 .like().or().like() 会让整个 WHERE
            // 退化成 (A AND B AND C) OR (D AND ...)，等于绕开 parentId / clientType 过滤
            String keyword = param.getKeyword();
            wrapper.and(w -> w.like(SysMenu::getMenuName, keyword)
                .or()
                .like(SysMenu::getRouteName, keyword));
        }
        if (param.getHideInMenu() != null) {
            wrapper.eq(SysMenu::getHideInMenu, param.getHideInMenu());
        }
        if (param.getMenuStatus() != null) {
            wrapper.eq(SysMenu::getMenuStatus, param.getMenuStatus());
        }
        if (param.getMenuCategory() != null) {
            wrapper.eq(SysMenu::getMenuCategory, param.getMenuCategory());
        }
        if (param.getStart() != null) {
            wrapper.gt(SysMenu::getCreateTime, param.getStart());
        }
        if (param.getEnd() != null) {
            wrapper.lt(SysMenu::getCreateTime, param.getEnd());
        }
        wrapper.orderByAsc(SysMenu::getParentId);
        PageInfo<SysMenu> page = this.page(this.pageInfo(param), wrapper);
        return MybatisPage.convert(page, SysMenuConvert.INSTANCE::toVO);
    }

    @Override
    public List<SysMenuDTO> getMenusByParentId(Serializable parentId, boolean detail, MenuType menuType) {
        LambdaQueryWrapper<SysMenu> wrapper = this.queryWrapper()
            .eq(SysMenu::getParentId, parentId == null ? "0" : parentId)
            .eq(menuType != null, SysMenu::getClientType, menuType)
            .orderByAsc(SysMenu::getOrderNum)
            .orderByDesc(SysMenu::getCreateTime);
        if (detail) {
            wrapper.select(SysMenu::getId,
                SysMenu::getParentId,
                SysMenu::getMenuName,
                SysMenu::getRoutePath,
                SysMenu::getRouteName,
                SysMenu::getIcon,
                SysMenu::getIconType,
                SysMenu::getMeta,
                SysMenu::getComponent,
                SysMenu::getMenuLevel,
                SysMenu::getOrderNum,
                SysMenu::getMenuStatus,
                SysMenu::getUrl,
                SysMenu::getHideInMenu,
                SysMenu::getMenuCategory,
                SysMenu::getClientType,
                SysMenu::getI18nKey,
                SysMenu::getKeepAlive,
                SysMenu::getConstant,
                SysMenu::getHref,
                SysMenu::getActiveMenu,
                SysMenu::getMultiTab,
                SysMenu::getFixedIndexInTab,
                SysMenu::getRouteQuery,
                SysMenu::getCreateBy, SysMenu::getUpdateBy,
                SysMenu::getCreateTime,
                SysMenu::getUpdateTime);
        } else {
            wrapper.select(SysMenu::getId, SysMenu::getParentId, SysMenu::getMenuName, SysMenu::getClientType);
        }
        List<SysMenu> menus = this.list(wrapper);
        return SysMenuConvert.INSTANCE.toDTO(menus);
    }

    @Override
    public long getSubMenuCount(Serializable id) {
        if (id == null) {
            return 0;
        }
        return this.count(this.queryWrapper().eq(SysMenu::getParentId, id));
    }

    @Override
    public List<SysMenuDTO> getMenuTree(Serializable menuId, boolean detail, MenuType menuType) {
        List<SysMenuDTO> menus = this.getMenusByParentId(menuId, detail, menuType);
        for (SysMenuDTO menu : menus) {
            Serializable id = menu.getId();
            List<SysMenuDTO> children = this.getMenuTree(id, detail, menuType);
            menu.setChildren(children);
        }
        return menus;
    }

    @Override
    public String getNameById(Serializable menuId) {
        LambdaQueryWrapper<SysMenu> wrapper = this.queryWrapper()
            .eq(SysMenu::getId, menuId)
            .select(SysMenu::getRouteName);
        SysMenu entity = this.getOne(wrapper);
        if (entity == null) {
            return null;
        }
        return entity.getRouteName();
    }

    @Override
    public List<SysMenuDTO> listByTitle(String title, MenuType menuType) {
        LambdaQueryWrapper<SysMenu> wrapper = this.queryWrapper()
            .eq(SysMenu::getClientType, menuType)
            .like(StringUtils.isNotBlank(title), SysMenu::getMenuName, title)
            .orderByAsc(SysMenu::getOrderNum)
            .orderByDesc(SysMenu::getCreateTime);
        wrapper.select(SysMenu::getId, SysMenu::getParentId, SysMenu::getMenuName, SysMenu::getRoutePath,
            SysMenu::getRouteName, SysMenu::getIcon, SysMenu::getIconType, SysMenu::getMeta,
            SysMenu::getComponent, SysMenu::getMenuLevel, SysMenu::getOrderNum, SysMenu::getMenuStatus,
            SysMenu::getUrl, SysMenu::getHideInMenu, SysMenu::getMenuCategory, SysMenu::getClientType,
            SysMenu::getI18nKey, SysMenu::getKeepAlive, SysMenu::getConstant, SysMenu::getHref,
            SysMenu::getActiveMenu, SysMenu::getMultiTab, SysMenu::getFixedIndexInTab,
            SysMenu::getRouteQuery, SysMenu::getCreateBy, SysMenu::getUpdateBy,
            SysMenu::getCreateTime, SysMenu::getUpdateTime);

        List<SysMenu> menus = this.list(wrapper);
        return SysMenuConvert.INSTANCE.toDTO(menus);
    }

    @Override
    public List<SysMenuDTO> getDtoByIds(Set<String> ids) {
        List<SysMenu> list = this.lambdaQuery().in(SysMenu::getId, ids).list();
        return SysMenuConvert.INSTANCE.toDTO(list);
    }

    @Override
    public List<SysMenuDTO> listAll() {
        List<SysMenu> list = list();
        return SysMenuConvert.INSTANCE.toDTO(list);
    }

    @Override
    public List<SysMenuDTO> listByType(MenuType menuType) {
        List<SysMenu> list = this.lambdaQuery()
            .eq(menuType != null, SysMenu::getClientType, menuType)
            .list();
        return SysMenuConvert.INSTANCE.toDTO(list);
    }

    @Override
    public List<SysMenuDTO> listByCategory(MenuCategory menuCategory) {
        LambdaQueryWrapper<SysMenu> wrapper = this.queryWrapper();
        if (menuCategory != null) {
            wrapper.eq(SysMenu::getMenuCategory, menuCategory);
        }
        wrapper.orderByAsc(SysMenu::getOrderNum).orderByDesc(SysMenu::getCreateTime);
        List<SysMenu> list = this.list(wrapper);
        return SysMenuConvert.INSTANCE.toDTO(list);
    }

    @Override
    public SysMenuDTO getByRouteName(String routeName) {
        if (StringUtils.isBlank(routeName)) {
            return null;
        }
        SysMenu entity = this.lambdaQuery()
            .eq(SysMenu::getRouteName, routeName)
            .one();
        return SysMenuConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public List<String> listAllPageNames() {
        LambdaQueryWrapper<SysMenu> wrapper = this.queryWrapper()
            .eq(SysMenu::getMenuCategory, MenuCategory.MENU)
            .eq(SysMenu::getMenuStatus, MenuStatus.ENABLED)
            .select(SysMenu::getRouteName)
            .orderByAsc(SysMenu::getOrderNum);
        List<SysMenu> list = this.list(wrapper);
        return list.stream()
            .map(SysMenu::getRouteName)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
    }
}
