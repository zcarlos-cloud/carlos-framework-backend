package com.carlos.system.menu.controller;


import cn.hutool.core.util.StrUtil;
import com.carlos.core.constant.CoreConstant;
import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.system.enums.MenuCategory;
import com.carlos.system.enums.MenuType;
import com.carlos.system.menu.convert.SysMenuConvert;
import com.carlos.system.menu.manager.SysMenuManager;
import com.carlos.system.menu.pojo.dto.SysMenuDTO;
import com.carlos.system.menu.pojo.param.SysMenuCreateParam;
import com.carlos.system.menu.pojo.param.SysMenuPageParam;
import com.carlos.system.menu.pojo.param.SysMenuUpdateParam;
import com.carlos.system.menu.pojo.vo.SysMenuRecursionVO;
import com.carlos.system.menu.pojo.vo.SysMenuTreeVO;
import com.carlos.system.menu.pojo.vo.SysMenuVO;
import com.carlos.system.menu.service.SysMenuService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 系统菜单 rest服务接口
 * </p>
 *
 * @author carlos
 * @date 2021-12-28 15:26:57
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/menu")
@Tag(name = "系统菜单")
public class SysMenuController {

    public static final String BASE_NAME = "系统菜单";

    private final SysMenuService menuService;

    private final SysMenuManager menuManager;


    @Log(
        title = "菜单管理",
        logType = "SYS_MENU_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增菜单: ' + #param.menuName",
        targetType = "SYS_MENU",
        riskLevel = 40)
    @PreAuthorize("hasPermission('manage:menu', 'create')")
    @PostMapping

    @Operation(summary = "新增" + BASE_NAME)

    public void add(@RequestBody @Validated SysMenuCreateParam param) {
        SysMenuDTO dto = SysMenuConvert.INSTANCE.toDTO(param);
        this.menuService.addMenu(dto);
    }


    @Log(
        title = "菜单管理",
        logType = "SYS_MENU_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除菜单: ' + #param.ids",
        targetType = "SYS_MENU",
        targetId = "#param.ids",
        riskLevel = 60)
    @PreAuthorize("hasPermission('manage:menu', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)

    public void delete(@RequestBody @Validated ParamIdSet<Long> param) {
        this.menuService.deleteMenu(param.getIds());
    }


    @Log(
        title = "菜单管理",
        logType = "SYS_MENU_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'修改菜单: ' + #param.menuName",
        targetType = "SYS_MENU",
        targetId = "#param.id",
        riskLevel = 40)
    @PreAuthorize("hasPermission('manage:menu', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)

    public void update(@RequestBody @Validated SysMenuUpdateParam param) {
        SysMenuDTO dto = SysMenuConvert.INSTANCE.toDTO(param);
        this.menuService.updateMenu(dto);
    }


    @PreAuthorize("hasPermission('manage:menu', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")

    public SysMenuVO detail(@RequestParam Long id) {
        return SysMenuConvert.INSTANCE.toVO(this.menuManager.getDtoById(id));
    }


    @PreAuthorize("hasPermission('manage:menu', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    @Hidden
    public Paging<SysMenuVO> page(SysMenuPageParam param) {
        return this.menuManager.getPage(param);
    }

    @PreAuthorize("hasPermission('manage:menu', 'view')")
    @GetMapping("pageWithButtons")
    @Operation(summary = BASE_NAME + "分页列表（含按钮权限）")
    public Paging<SysMenuVO> pageWithButtons(SysMenuPageParam param) {
        return this.menuService.getMenuPageWithButtons(param);
    }

    /**
     * 全部菜单（含按钮权限）
     *
     * <p>
     * 管理后台的菜单树需要**全量**菜单才能建出完整层级并支持客户端搜索：后端 keyword 过滤
     * 只返回命中行、父级不在结果里，客户端必须补出祖先链，所以无论如何都得先有全量数据。
     * </p>
     *
     * <p>
     * 此前前端是拿 {@code /sys/menu/pageWithButtons?size=50} 顶的，而库中菜单已超过 50 条 ——
     * **第 51 条起静默消失**，既不在树上也搜不到。命名对齐 {@code /sys/module/all}。
     * </p>
     */
    @PreAuthorize("hasPermission('manage:menu', 'view')")
    @GetMapping("all")
    @Operation(summary = BASE_NAME + "全部列表（含按钮权限）")
    public List<SysMenuVO> all() {
        return this.menuService.getAllMenusWithButtons();
    }


    @PreAuthorize("hasPermission('manage:menu', 'view')")
    @GetMapping("tree/select")
    @Operation(summary = "菜单树形下拉列表", description = "菜单树形列表，包含详情信息")

    public List<SysMenuTreeVO> select(@RequestParam(value = "menuType", required = false) MenuType menuType) {
        List<SysMenuDTO> menuTree = this.menuManager.getMenuTree(CoreConstant.PARENT_LONG_0, false, menuType);
        return SysMenuConvert.INSTANCE.toTreeListVO(menuTree);
    }


    @PreAuthorize("hasPermission('manage:menu', 'view')")
    @GetMapping("tree/list")
    @Operation(summary = "菜单树形列表", description = "菜单下拉列表，仅包含基本信息")
    public List<SysMenuRecursionVO> treeList(String parentId, String title, @RequestParam(value = "menuType", required = false) MenuType menuType) {
        List<SysMenuDTO> menus;
        if (StrUtil.isNotBlank(title)) {
            menus = this.menuManager.listByTitle(title, menuType);
        } else {
            menus = this.menuManager.getMenuTree(parentId, true, menuType);
        }
        return SysMenuConvert.INSTANCE.toRecursionListVO(menus);
    }


    @PreAuthorize("hasPermission('manage:menu', 'view')")
    @GetMapping("list")
    @Operation(summary = "菜单列表", description = "逐级请求")
    public List<SysMenuVO> list(String parentId, @RequestParam(value = "menuType", required = false) MenuType menuType) {
        List<SysMenuDTO> menuTree = this.menuManager.getMenusByParentId(parentId, true, menuType);
        return SysMenuConvert.INSTANCE.toListVO(menuTree);
    }

    @PreAuthorize("hasPermission('manage:menu', 'view')")
    @GetMapping("allPages")
    @Operation(summary = "获取所有页面路由名")
    public List<String> allPages() {
        return this.menuService.getAllPages();
    }

    @PreAuthorize("hasPermission('manage:menu', 'view')")
    @GetMapping("listWithButtons")
    @Operation(summary = "获取菜单列表（含按钮权限）")
    public List<SysMenuVO> listWithButtons(@RequestParam(value = "menuCategory", required = false) MenuCategory menuCategory) {
        List<SysMenuDTO> menus = this.menuService.listWithButtons(menuCategory);
        return SysMenuConvert.INSTANCE.toListVO(menus);
    }
}
