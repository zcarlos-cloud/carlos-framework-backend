package com.carlos.auth.app.controller;

import com.carlos.auth.app.pojo.vo.AppPortalCategoryVO;
import com.carlos.auth.app.pojo.vo.AppPortalGroupVO;
import com.carlos.auth.app.service.AppPortalService;
import com.carlos.core.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * 应用门户接口（面向用户）
 *
 * <p>提供门户首页所需的应用列表和分类数据。
 * 应用可见性基于当前用户的角色自动过滤。</p>
 *
 * @author Carlos
 * @date 2026-07-02
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/app/portal")
@Tag(name = "应用门户", description = "门户首页应用展示接口")
public class AppPortalController {

    private final AppPortalService appPortalService;

    /**
     * 获取当前用户可见的应用列表（按分类分组）
     */
    @PreAuthorize("hasPermission('manage:app-portal', 'view')")
    @GetMapping("/apps")
    @Operation(summary = "我的应用列表", description = "返回当前用户有权限访问的应用，按分类分组展示")
    public Result<List<AppPortalGroupVO>> listMyApps() {
        List<String> userRoles = getCurrentUserRoles();
        List<AppPortalGroupVO> result = appPortalService.listMyApps(userRoles);
        return Result.success(result);
    }

    /**
     * 获取应用分类列表
     */
    @PreAuthorize("hasPermission('manage:app-portal', 'view')")
    @GetMapping("/categories")
    @Operation(summary = "应用分类列表", description = "返回所有应用分类，用于前端筛选")
    public Result<List<AppPortalCategoryVO>> listCategories() {
        List<AppPortalCategoryVO> result = appPortalService.listCategories();
        return Result.success(result);
    }

    /**
     * 从 SecurityContext 获取当前用户的角色列表
     */
    private List<String> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Collections.emptyList();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
