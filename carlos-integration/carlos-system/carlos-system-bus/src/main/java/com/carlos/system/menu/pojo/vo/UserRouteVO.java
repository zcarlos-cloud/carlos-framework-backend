package com.carlos.system.menu.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 用户动态路由 VO（对应前端 Api.Route.UserRoute：{ routes, home }）
 * </p>
 *
 * @author carlos
 * @date 2026-09-15
 */
@Data
@Accessors(chain = true)
public class UserRouteVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前用户可见的路由树")
    private List<RouteVO> routes;

    @Schema(description = "首页路由 name，前端登录后跳转用")
    private String home;

    public UserRouteVO() {
    }

    public UserRouteVO(List<RouteVO> routes, String home) {
        this.routes = routes;
        this.home = home;
    }
}
