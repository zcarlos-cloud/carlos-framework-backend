package com.carlos.system.api;

import com.carlos.core.response.Result;
import com.carlos.system.ServiceNameConstant;
import com.carlos.system.enums.MenuType;
import com.carlos.system.fallback.FeignSysMenuFallbackFactory;
import com.carlos.system.pojo.ao.SysMenuAO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 系统菜单 feign 提供接口
 * </p>
 *
 * @author carlos
 * @date 2021-12-28 15:26:57
 */
@FeignClient(value = ServiceNameConstant.SYSTEM, contextId = "menu", path = "/api/sys/menu", fallbackFactory = FeignSysMenuFallbackFactory.class)
public interface ApiSysMenu {

    /**
     * 获取菜单信息
     *
     * <p>⚠️ 此处必须用无值 {@code @GetMapping}，与实现 {@code ApiSysMenuImpl#getMenuById} 保持一致。
     * 曾写成 {@code @GetMapping(value = "/")}，拼出的请求路径是 {@code /api/sys/menu/}，
     * 而实现侧注册的是 {@code /api/sys/menu}；Spring Framework 6 起已移除尾斜杠匹配，
     * 两者不再等价 —— 表现为调用方拿到
     * {@code No static resource api/sys/menu for request '/api/sys/menu/'} 的 500。</p>
     *
     * @param id 菜单id
     * @return com.carlos.voice.common.dto.sys.SysMenuDTO
     * @author carlos
     * @date 2022/1/6 14:52
     */
    @GetMapping
    Result<SysMenuAO> getMenuById(@RequestParam("id") Long id);

    /**
     * 批量获取菜单信息
     *
     * @param ids 参数0
     * @return com.carlos.core.response.Result<java.util.List < com.carlos.system.pojo.ao.SysMenuAO>>
     * @author Carlos
     * @date 2023/7/3 23:19
     */
    @PostMapping(value = "list")
    Result<List<SysMenuAO>> listMenus(@RequestBody Set<String> ids);


    /**
     * 获取所有菜单
     *
     * @return com.carlos.core.response.Result<java.util.List < com.carlos.system.pojo.ao.SysMenuAO>>
     * @author Carlos
     * @date 2023/7/4 9:04
     */
    @GetMapping(value = "all")
    Result<List<SysMenuAO>> allMenu();

    /**
     * 获取某个类型所有菜单
     *
     * @return com.carlos.core.response.Result<java.util.List < com.carlos.system.pojo.ao.SysMenuAO>>
     * @author Carlos
     * @date 2023/7/4 9:04
     */
    @GetMapping(value = "type")
    Result<List<SysMenuAO>> listMenuByType(MenuType menuType);
}
