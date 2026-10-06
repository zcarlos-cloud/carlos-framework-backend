package com.carlos.system.menu.apiimpl;

import cn.hutool.core.collection.CollectionUtil;
import com.carlos.core.response.Result;
import com.carlos.system.api.ApiSysMenu;
import com.carlos.system.enums.MenuType;
import com.carlos.system.menu.convert.SysMenuConvert;
import com.carlos.system.menu.manager.SysMenuManager;
import com.carlos.system.menu.pojo.dto.SysMenuDTO;
import com.carlos.system.menu.service.SysMenuService;
import com.carlos.system.pojo.ao.SysMenuAO;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 系统菜单 api接口
 * </p>
 *
 * @author carlos
 * @date 2021-12-28 15:26:57
 */
@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sys/menu")
@Tag(name = "系统菜单Feign接口")
public class ApiSysMenuImpl implements ApiSysMenu {

    private final SysMenuManager menuManager;
    private final SysMenuService menuService;

    @Override
    @GetMapping
    @Operation(summary = "获取菜单信息")
    public Result<SysMenuAO> getMenuById(@RequestParam("id") Long id) {
        SysMenuDTO dto = menuManager.getDtoById(id);
        SysMenuAO ao = SysMenuConvert.INSTANCE.toAO(dto);
        return Result.success(ao);
    }


    @Override
    @PostMapping("list")
    @Operation(summary = "获取菜单信息")
    public Result<List<SysMenuAO>> listMenus(@RequestBody Set<String> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            return Result.success(Collections.emptyList());
        }
        List<SysMenuDTO> menus = menuService.getByIds(ids);
        return Result.success(SysMenuConvert.INSTANCE.toAOList(menus));
    }


    @GetMapping("all")
    @Operation(summary = "获取所有菜单")
    @Override
    public Result<List<SysMenuAO>> allMenu() {
        List<SysMenuDTO> menus = menuService.all();
        return Result.success(SysMenuConvert.INSTANCE.toAOList(menus));
    }

    /**
     * 获取某个类型所有菜单
     *
     * @return com.carlos.core.response.Result<java.util.List < com.carlos.system.pojo.ao.SysMenuAO>>
     * @author Carlos
     * @date 2023/7/4 9:04
     */
    @Override
    @GetMapping(value = "type")
    public Result<List<SysMenuAO>> listMenuByType(MenuType menuType) {
        List<SysMenuDTO> menus = menuService.listByType(menuType);
        return Result.success(SysMenuConvert.INSTANCE.toAOList(menus));
    }
}

