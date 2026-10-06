package com.carlos.system.fallback;

import com.carlos.core.response.Result;
import com.carlos.system.api.ApiSysMenu;
import com.carlos.system.enums.MenuType;
import com.carlos.system.pojo.ao.SysMenuAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.List;
import java.util.Set;


/**
 * <p>
 * 用户降级服务
 * </p>
 *
 * @author Carlos
 * @date 2022/11/16 10:57
 */

@Slf4j
public class FeignSysMenuFallbackFactory implements FallbackFactory<ApiSysMenu> {

    @Override
    public ApiSysMenu create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("系统服务调用失败: message:{}", message);
        return new ApiSysMenu() {

            @Override
            public Result<SysMenuAO> getMenuById(Long id) {
                return Result.error();
            }

            @Override
            public Result<List<SysMenuAO>> listMenus(Set<String> ids) {
                return Result.error();
            }

            @Override
            public Result<List<SysMenuAO>> allMenu() {
                return Result.error();
            }

            @Override
            public Result<List<SysMenuAO>> listMenuByType(MenuType menuType) {
                return Result.error();
            }
        };
    }
}
