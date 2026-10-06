package com.carlos.system.menu.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.system.enums.MenuCategory;
import com.carlos.system.enums.MenuType;
import com.carlos.system.menu.pojo.dto.SysMenuDTO;
import com.carlos.system.menu.pojo.entity.SysMenu;
import com.carlos.system.menu.pojo.param.SysMenuPageParam;
import com.carlos.system.menu.pojo.vo.SysMenuVO;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 系统菜单 查询封装接口
 * </p>
 *
 * @author carlos
 * @date 2021-12-28 15:26:57
 */
public interface SysMenuManager extends BaseService<SysMenu> {

    /**
     * 新增系统菜单
     *
     * @param dto 系统菜单数据
     * @return boolean
     * @author carlos
     * @date 2021-12-28 15:26:57
     */
    boolean add(SysMenuDTO dto);

    /**
     * 删除系统菜单
     *
     * @param id 系统菜单id
     * @return boolean
     * @author carlos
     * @date 2021-12-28 15:26:57
     */
    boolean delete(Serializable id);

    /**
     * 修改系统菜单
     *
     * @param dto 菜单数据
     * @return boolean
     * @author carlos
     * @date 2021-12-28 15:26:57
     */
    boolean modify(SysMenuDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.user.pojo.dto.SysMenuDTO
     * @author carlos
     * @date 2021-12-28 15:26:57
     */
    SysMenuDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param param 分页参数
     * @author carlos
     * @date 2021-12-28 15:26:57
     */
    Paging<SysMenuVO> getPage(SysMenuPageParam param);

    /**
     * 获取指定id的菜单的子级菜单,如果为 null 则返回所有一级菜单
     *
     * @param parentId 父id值
     * @param detail   是否获取详情
     * @return java.util.List<com.carlos.user.pojo.dto.SysMenuDTO>
     * @date 2021/12/27 17:25
     */
    List<SysMenuDTO> getMenusByParentId(Serializable parentId, boolean detail, MenuType menuType);

    /**
     * 获取某个菜单的直接子菜单数目
     *
     * @param id 菜单id
     * @author carlos
     * @date 2021/12/28 17:00
     */
    long getSubMenuCount(Serializable id);

    /**
     * 获取菜单的树形结构
     *
     * @param menuId   父id
     * @param detail   是否获取详情
     * @param menuType
     * @return java.util.List<com.carlos.user.pojo.dto.MenuTreeDTO>
     * @author carlos
     * @date 2021/12/29 15:26
     */
    List<SysMenuDTO> getMenuTree(Serializable menuId, boolean detail, MenuType menuType);

    /**
     * 获取菜单名称
     *
     * @param menuId 菜单id
     * @return java.lang.String
     * @author carlos
     * @date 2021/12/29 11:22
     */
    String getNameById(Serializable menuId);

    /**
     * 根据标题查找菜单
     *
     * @param title 菜单标题
     * @return java.util.List<com.carlos.common.dto.sys.SysMenuDTO>
     * @author Carlos
     * @date 2022/11/24 12:53
     */
    List<SysMenuDTO> listByTitle(String title, MenuType menuType);

    /**
     * @Title: getDtoByIds
     * @Description: 根据菜单ids获取列表
     * @Date: 2023/3/31 16:01
     * @Parameters: [ids]
     * @Return java.util.List<com.carlos.system.pojo.dto.SysMenuDTO>
     */
    List<SysMenuDTO> getDtoByIds(Set<String> ids);

    /**
     * 获取所有菜单
     *
     * @return java.util.List<com.carlos.system.pojo.dto.SysMenuDTO>
     * @author Carlos
     * @date 2023/5/25 18:30
     */
    List<SysMenuDTO> listAll();

    /**
     * 获取某个类型菜单
     *
     * @param menuType 菜单类型
     * @return java.util.List<com.carlos.system.menu.pojo.dto.SysMenuDTO>
     * @author Carlos
     * @date 2025-04-17 13:58
     */
    List<SysMenuDTO> listByType(MenuType menuType);

    /**
     * 根据菜单类别查询
     *
     * @param menuCategory 菜单类别
     * @return 菜单列表
     */
    List<SysMenuDTO> listByCategory(MenuCategory menuCategory);

    /**
     * 根据路由名称查询菜单
     *
     * @param routeName 路由name
     * @return 菜单DTO
     */
    SysMenuDTO getByRouteName(String routeName);

    /**
     * 查询所有已启用的页面菜单路由名（menuCategory=2）
     *
     * @return 路由名列表
     */
    List<String> listAllPageNames();
}
