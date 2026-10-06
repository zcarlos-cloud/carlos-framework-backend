package com.carlos.system.menu.service;

import com.carlos.security.auth.UserContext;
import com.carlos.security.auth.UserContextAuthenticationToken;
import com.carlos.system.enums.MenuStatus;
import com.carlos.system.menu.manager.SysMenuManager;
import com.carlos.system.menu.manager.SysOperationManager;
import com.carlos.system.menu.pojo.dto.SysMenuDTO;
import com.carlos.system.menu.pojo.dto.SysOperationDTO;
import com.carlos.system.menu.pojo.vo.RouteVO;
import com.carlos.system.menu.pojo.vo.UserRouteVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

/**
 * {@link RouteService} 单元测试
 *
 * @author Carlos
 * @date 2026-09-18
 */
@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private SysMenuManager menuManager;

    @Mock
    private SysOperationManager operationManager;

    @InjectMocks
    private RouteService routeService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("中间层菜单应清空 component，避免前端再嵌套一层布局")
    void should_clear_component_for_intermediate_menu() {
        SysMenuDTO leaf = menu(210L, "audit_log", "/audit/log", "view.audit_log");
        SysMenuDTO middle = menu(200L, "audit", "/audit", "layout.base");
        middle.setChildren(List.of(leaf));
        SysMenuDTO top = menu(100L, "manage", "/manage", "layout.base");
        top.setChildren(List.of(middle));

        when(menuManager.getMenuTree(any(), eq(true), isNull())).thenReturn(List.of(top));

        UserRouteVO vo = routeService.getUserRoutes();

        assertNotNull(vo);
        RouteVO manage = vo.getRoutes().get(0);
        assertEquals("layout.base", manage.getComponent(), "一级菜单应保留 layout");

        RouteVO audit = manage.getChildren().get(0);
        assertNull(audit.getComponent(), "中间层必须清空 component");

        RouteVO log = audit.getChildren().get(0);
        assertEquals("view.audit_log", log.getComponent(), "叶子菜单应保留 view");
    }

    @Test
    @DisplayName("中间层误填 view.* 时同样应被清空")
    void should_clear_view_component_for_intermediate_menu() {
        SysMenuDTO leaf = menu(210L, "audit_log", "/audit/log", "view.audit_log");
        SysMenuDTO middle = menu(200L, "audit", "/audit", "view.audit_log");
        middle.setChildren(List.of(leaf));
        SysMenuDTO top = menu(100L, "manage", "/manage", "layout.base");
        top.setChildren(List.of(middle));

        when(menuManager.getMenuTree(any(), eq(true), isNull())).thenReturn(List.of(top));

        RouteVO audit = routeService.getUserRoutes().getRoutes().get(0).getChildren().get(0);

        assertNull(audit.getComponent());
    }

    @Test
    @DisplayName("一级叶子菜单的 component 不应被清空")
    void should_keep_component_for_first_level_leaf() {
        SysMenuDTO top = menu(100L, "home", "/home", "layout.base$view.home");

        when(menuManager.getMenuTree(any(), eq(true), isNull())).thenReturn(List.of(top));

        RouteVO route = routeService.getUserRoutes().getRoutes().get(0);

        assertEquals("layout.base$view.home", route.getComponent());
    }

    @Test
    @DisplayName("非超管：只保留被授权菜单及其祖先链，constant 菜单不受限")
    void should_filter_menus_by_permission_for_normal_user() {
        // manage → audit → audit_log（授权）；manage → dict（未授权，应被剪掉）
        SysMenuDTO auditLog = menu(210L, "audit_log", "/audit/log", "view.audit_log");
        SysMenuDTO audit = menu(200L, "audit", "/audit", "layout.base");
        audit.setChildren(List.of(auditLog));
        SysMenuDTO dict = menu(220L, "manage_dict", "/manage/dict", "view.manage_dict");
        SysMenuDTO manage = menu(100L, "manage", "/manage", "layout.base");
        manage.setChildren(List.of(audit, dict));
        SysMenuDTO home = menu(50L, "home", "/home", "layout.base$view.home");
        home.setConstant(true);

        when(menuManager.getMenuTree(any(), eq(true), isNull())).thenReturn(List.of(home, manage));
        // 用户仅持有 audit_log:view 权限 → 对应操作 menuId=210
        when(operationManager.listByPermCodes(Set.of("audit_log:view")))
            .thenReturn(List.of(operation(210L, "audit_log:view")));
        authenticate(Set.of("audit_log:view"), Set.of("ORG_VIEWER"));

        UserRouteVO vo = routeService.getUserRoutes();
        List<RouteVO> routes = vo.getRoutes();

        // constant 的 home 恒保留；manage 因子孙被授权而保留；dict 被剪掉
        assertEquals(2, routes.size(), "应只保留 home 与 manage");
        RouteVO manageRoute = routes.stream().filter(r -> "manage".equals(r.getName())).findFirst().orElseThrow();
        assertEquals(1, manageRoute.getChildren().size(), "audit 下的 dict 应被剪掉");
        RouteVO auditRoute = manageRoute.getChildren().get(0);
        assertEquals("audit", auditRoute.getName(), "audit 因子孙被授权而保留");
        assertEquals(1, auditRoute.getChildren().size());
        assertEquals("audit_log", auditRoute.getChildren().get(0).getName(), "被授权的叶子应保留");
    }

    @Test
    @DisplayName("非超管且无任何权限时：只保留 constant 菜单")
    void should_keep_only_constant_menus_when_no_permission() {
        SysMenuDTO home = menu(50L, "home", "/home", "layout.base$view.home");
        home.setConstant(true);
        SysMenuDTO manage = menu(100L, "manage", "/manage", "layout.base");
        manage.setChildren(List.of(menu(210L, "manage_dict", "/manage/dict", "view.manage_dict")));

        when(menuManager.getMenuTree(any(), eq(true), isNull())).thenReturn(List.of(home, manage));
        authenticate(Set.of(), Set.of("ORG_VIEWER"));

        List<RouteVO> routes = routeService.getUserRoutes().getRoutes();

        assertEquals(1, routes.size(), "无权限时只应剩 constant 菜单");
        assertEquals("home", routes.get(0).getName());
    }

    @Test
    @DisplayName("超管：不按权限过滤，返回全部菜单")
    void should_not_filter_for_super_admin() {
        SysMenuDTO manage = menu(100L, "manage", "/manage", "layout.base");
        manage.setChildren(List.of(menu(210L, "manage_dict", "/manage/dict", "view.manage_dict")));

        when(menuManager.getMenuTree(any(), eq(true), isNull())).thenReturn(List.of(manage));
        authenticate(Set.of(), Set.of("SUPER_ADMIN"));

        List<RouteVO> routes = routeService.getUserRoutes().getRoutes();

        assertEquals(1, routes.size());
        assertEquals(1, routes.get(0).getChildren().size(), "超管应看到全部菜单");
    }

    /**
     * 将当前测试线程的 SecurityContext 置为指定权限/角色的用户
     */
    private void authenticate(Set<String> permissions, Set<String> roleCodes) {
        UserContext userContext = new UserContext();
        userContext.setUserId(1L);
        userContext.setUsername("tester");
        userContext.setPermissions(new java.util.HashSet<>(permissions));
        userContext.setRoleCodes(new java.util.HashSet<>(roleCodes));
        SecurityContextHolder.getContext()
            .setAuthentication(new UserContextAuthenticationToken(userContext));
    }

    private SysOperationDTO operation(Long menuId, String permCode) {
        SysOperationDTO dto = new SysOperationDTO();
        dto.setMenuId(menuId);
        dto.setPermCode(permCode);
        return dto;
    }

    private SysMenuDTO menu(Long id, String routeName, String routePath, String component) {
        SysMenuDTO dto = new SysMenuDTO();
        dto.setId(id);
        dto.setRouteName(routeName);
        dto.setRoutePath(routePath);
        dto.setComponent(component);
        dto.setMenuName(routeName);
        dto.setMenuStatus(MenuStatus.ENABLED);
        dto.setHideInMenu(false);
        return dto;
    }
}
