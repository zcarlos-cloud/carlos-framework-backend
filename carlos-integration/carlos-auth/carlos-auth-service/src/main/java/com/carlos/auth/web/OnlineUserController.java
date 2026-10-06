package com.carlos.auth.web;

import com.carlos.auth.session.pojo.param.OnlineUserKickParam;
import com.carlos.auth.session.pojo.param.OnlineUserPageParam;
import com.carlos.auth.session.pojo.vo.OnlineUserVO;
import com.carlos.auth.session.service.OnlineUserService;
import com.carlos.core.pagination.Paging;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 在线用户 rest服务接口
 * </p>
 *
 * <p>在线会话枚举（Redis 授权存储）+ 踢下线（删授权记录并同步清网关内省缓存）。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/online-user")
@Tag(name = "在线用户")
public class OnlineUserController {

    private final OnlineUserService onlineUserService;

    @PreAuthorize("hasPermission('manage:online-user', 'view')")
    @GetMapping("page")
    @Operation(summary = "在线用户分页")
    public Paging<OnlineUserVO> page(@Validated OnlineUserPageParam param) {
        return this.onlineUserService.page(param);
    }

    @PreAuthorize("hasPermission('manage:online-user', 'kick')")
    @PostMapping("kick")
    @Operation(summary = "踢下线（按会话批量或按用户全部会话）")
    public int kick(@RequestBody @Validated OnlineUserKickParam param) {
        return this.onlineUserService.kick(param);
    }

}
