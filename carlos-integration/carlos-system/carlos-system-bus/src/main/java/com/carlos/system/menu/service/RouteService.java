package com.carlos.system.menu.service;

import com.carlos.core.constant.CoreConstant;
import com.carlos.security.auth.UserContext;
import com.carlos.security.util.SecurityUtils;
import com.carlos.system.enums.MenuStatus;
import com.carlos.system.menu.manager.SysMenuManager;
import com.carlos.system.menu.manager.SysOperationManager;
import com.carlos.system.menu.pojo.dto.SysMenuDTO;
import com.carlos.system.menu.pojo.dto.SysOperationDTO;
import com.carlos.system.menu.pojo.vo.RouteVO;
import com.carlos.system.menu.pojo.vo.UserRouteVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 前端路由 业务服务
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {

    /**
     * 默认首页路由 name（与前端 VITE_ROUTE_HOME 一致）
     */
    private static final String DEFAULT_HOME = "home";

    /**
     * 默认首页路由 path
     */
    private static final String DEFAULT_HOME_PATH = "/home";

    private final SysMenuManager menuManager;

    private final SysOperationManager operationManager;

    /**
     * 获取用户路由列表（ElegantConstRoute 格式）
     *
     * <p>返回 {@code { routes, home }} 结构，与前端 Api.Route.UserRoute 对应：
     * 前端动态路由模式依赖 home 指定登录后跳转的首页路由。</p>
     *
     * <p><b>菜单可见性按当前登录用户过滤</b>：非超管只保留「其角色被授权的操作所属的菜单」，
     * 且祖先链保留；{@code constant=true} 的菜单（首页/个人中心等）不走权限过滤。
     * 权限来源是 {@link SecurityUtils#getCurrentUser()} 上的 {@code permissions}
     * （由 X-User-Context 断言 + PermissionProvider 解析）。</p>
     *
     * @return 用户路由VO
     */
    public UserRouteVO getUserRoutes() {
        List<SysMenuDTO> menuTree = menuManager.getMenuTree(CoreConstant.PARENT_LONG_0, true, null);

        UserContext user = SecurityUtils.getCurrentUserOpt().orElse(null);
        if (user != null && !user.isSuperAdmin()) {
            Set<Long> grantedMenuIds = resolveGrantedMenuIds(user.getPermissions());
            menuTree = filterByPermission(menuTree, grantedMenuIds);
            log.debug("User routes filtered: userId={}, roles={}, grantedMenus={}",
                user.getUserId(), user.getRoleCodes(), grantedMenuIds.size());
        }

        List<RouteVO> routes = buildRouteTree(menuTree);
        return new UserRouteVO(routes, resolveHome(routes));
    }

    /**
     * 由用户权限码解析出「被授权的菜单 ID」集合
     *
     * <p>权限码 → 操作（sys_operation，带 menu_id）→ 菜单。API 类操作 menu_id 可能为空，忽略。</p>
     *
     * @param permissions 当前用户权限码集合
     * @return 被授权的菜单 ID 集合
     */
    private Set<Long> resolveGrantedMenuIds(Set<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Set.of();
        }
        List<SysOperationDTO> operations = operationManager.listByPermCodes(permissions);
        if (operations == null || operations.isEmpty()) {
            return Set.of();
        }
        return operations.stream()
            .map(SysOperationDTO::getMenuId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    /**
     * 按权限剪枝菜单树
     *
     * <p>保留规则：{@code constant=true} 的菜单恒保留；自身被授权保留；任一子孙被授权则父节点保留
     * （保证被授权菜单的祖先链不断裂，否则前端拿到孤儿节点无法渲染）。</p>
     *
     * @param menus          菜单（子树）
     * @param grantedMenuIds 被授权的菜单 ID 集合
     * @return 过滤后的菜单（子树）
     */
    private List<SysMenuDTO> filterByPermission(List<SysMenuDTO> menus, Set<Long> grantedMenuIds) {
        if (menus == null || menus.isEmpty()) {
            return List.of();
        }
        List<SysMenuDTO> kept = new ArrayList<>();
        for (SysMenuDTO menu : menus) {
            List<SysMenuDTO> keptChildren = filterByPermission(menu.getChildren(), grantedMenuIds);
            boolean visible = Boolean.TRUE.equals(menu.getConstant())
                || grantedMenuIds.contains(menu.getId())
                || !keptChildren.isEmpty();
            if (visible) {
                menu.setChildren(keptChildren);
                kept.add(menu);
            }
        }
        return kept;
    }

    /**
     * 解析首页路由 name
     *
     * <p>优先取 path 为 /home 或 name 为 home 的一级路由，其次取第一个一级路由，兜底 home。</p>
     *
     * @param routes 一级路由列表
     * @return 首页路由 name
     */
    private String resolveHome(List<RouteVO> routes) {
        if (routes == null || routes.isEmpty()) {
            return DEFAULT_HOME;
        }
        return routes.stream()
            .filter(route -> DEFAULT_HOME.equals(route.getName()) || DEFAULT_HOME_PATH.equals(route.getPath()))
            .map(RouteVO::getName)
            .filter(name -> name != null && !name.isEmpty())
            .findFirst()
            .orElseGet(() -> {
                String firstName = routes.get(0).getName();
                return firstName != null && !firstName.isEmpty() ? firstName : DEFAULT_HOME;
            });
    }

    /**
     * 获取常量路由列表
     *
     * @return 常量路由VO列表
     */
    public List<RouteVO> getConstantRoutes() {
        return List.of();
    }

    /**
     * 检查路由是否存在
     *
     * @param routeName 路由name
     * @return 是否存在
     */
    public boolean isRouteExist(String routeName) {
        return menuManager.getByRouteName(routeName) != null;
    }

    /**
     * 将菜单DTO树转换为路由VO树
     *
     * @param menus 菜单DTO列表
     * @return 路由VO列表
     */
    private List<RouteVO> buildRouteTree(List<SysMenuDTO> menus) {
        return buildRouteTree(menus, 0);
    }

    /**
     * 将菜单DTO树转换为路由VO树
     *
     * @param menus 菜单DTO列表
     * @param depth 当前层级深度，一级菜单为 0
     * @return 路由VO列表
     */
    private List<RouteVO> buildRouteTree(List<SysMenuDTO> menus, int depth) {
        if (menus == null || menus.isEmpty()) {
            return List.of();
        }
        List<RouteVO> routes = new ArrayList<>();
        for (SysMenuDTO menu : menus) {
            // 只按启用状态过滤。
            //
            // ⚠️ 切勿在此处再按 hideInMenu 过滤：hideInMenu 的语义是「不在侧边栏显示」，
            // 不是「不可路由」。前端本就按 meta.hideInMenu 隐藏菜单项，隐藏的页面仍必须注册
            // 到路由表，否则从下拉菜单/列表跳转过去会落到 not-found
            // （典型受害者：/user-center、/org/user-detail/:id、/function/multi-tab）。
            //
            // 早期实现把 hideInMenu 的菜单整条丢弃，而 isRouteExist 查的是 sys_menu、
            // 并无同样的过滤，两个接口结论相悖 —— 前端守卫于是判定为「路由存在但无权限」，
            // 跳转 403；403 又未注册，最终整个应用卡死在启动 loading 页。
            if (menu.getMenuStatus() != MenuStatus.ENABLED) {
                continue;
            }
            RouteVO route = convertToRoute(menu);
            boolean hasChildren = menu.getChildren() != null && !menu.getChildren().isEmpty();
            if (hasChildren) {
                route.setChildren(buildRouteTree(menu.getChildren(), depth + 1));
                // 中间层不得携带 component：约定只有顶层挂 layout、叶子挂 view。
                // 中间层若保留 layout.* 会让前端再嵌套一层布局（内容区出现重复的侧边栏与标签栏），
                // 保留 view.* 则因其内部没有 <RouterView> 导致子路由完全不渲染。
                if (depth > 0) {
                    route.setComponent(null);
                }
            }
            routes.add(route);
        }
        return routes;
    }

    /**
     * 单个菜单DTO转路由VO
     *
     * @param menu 菜单DTO
     * @return 路由VO
     */
    private RouteVO convertToRoute(SysMenuDTO menu) {
        RouteVO route = new RouteVO();
        route.setId(menu.getId());
        route.setName(menu.getRouteName());
        route.setPath(menu.getRoutePath());
        route.setComponent(menu.getComponent());

        RouteVO.RouteMetaVO meta = new RouteVO.RouteMetaVO();
        meta.setTitle(menu.getMenuName());
        meta.setI18nKey(menu.getI18nKey());
        meta.setIcon(menu.getIcon());
        meta.setOrderNum(menu.getOrderNum());
        meta.setHideInMenu(menu.getHideInMenu());
        meta.setKeepAlive(menu.getKeepAlive());
        meta.setConstant(menu.getConstant());
        meta.setHref(menu.getHref());
        meta.setActiveMenu(menu.getActiveMenu());
        meta.setMultiTab(menu.getMultiTab());
        meta.setFixedIndexInTab(menu.getFixedIndexInTab());
        route.setMeta(meta);

        return route;
    }

}
