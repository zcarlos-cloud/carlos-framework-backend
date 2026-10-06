package com.carlos.system.menu.controller;

import com.carlos.core.response.Result;
import com.carlos.system.menu.pojo.vo.RouteVO;
import com.carlos.system.menu.pojo.vo.UserRouteVO;
import com.carlos.system.menu.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 前端路由接口
 * </p>
 *
 * <p>提供符合 ElegantConstRoute 格式的路由数据，供前端动态生成菜单。</p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/route")
@Tag(name = "前端路由")
public class RouteController {

    private final RouteService routeService;

    @GetMapping("getUserRoutes")
    @Operation(summary = "获取用户路由", description = "返回当前用户可见的菜单路由（{ routes, home } 结构）")
    public Result<UserRouteVO> getUserRoutes() {
        return Result.success(routeService.getUserRoutes());
    }

    @GetMapping("getConstantRoutes")
    @Operation(summary = "获取常量路由", description = "返回不依赖权限的静态路由（如登录页、404等）")
    public Result<List<RouteVO>> getConstantRoutes() {
        return Result.success(routeService.getConstantRoutes());
    }

    @GetMapping("isRouteExist")
    @Operation(summary = "检查路由是否存在")
    public Result<Boolean> isRouteExist(@RequestParam String routeName) {
        return Result.success(routeService.isRouteExist(routeName));
    }

}
