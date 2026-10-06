package com.carlos.auth.web;

import com.carlos.auth.log.LoginLogService;
import com.carlos.auth.log.pojo.param.LoginLogPageParam;
import com.carlos.auth.log.pojo.vo.LoginLogDetailVO;
import com.carlos.auth.log.pojo.vo.LoginLogVO;
import com.carlos.core.pagination.Paging;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 登录日志 rest服务接口
 * </p>
 *
 * <p>数据来自 carlos-audit（ClickHouse），本控制器只做查询聚合，不落本地表。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/login-log")
@Tag(name = "登录日志")
public class LoginLogController {

    private final LoginLogService loginLogService;

    @PreAuthorize("hasPermission('manage:login-log', 'view')")
    @GetMapping("page")
    @Operation(summary = "登录日志分页")
    public Paging<LoginLogVO> page(@Validated LoginLogPageParam param) {
        return this.loginLogService.page(param);
    }

    @PreAuthorize("hasPermission('manage:login-log', 'view')")
    @GetMapping("{id}")
    @Operation(summary = "登录日志详情")
    public LoginLogDetailVO detail(@PathVariable("id") Long id) {
        return this.loginLogService.getById(id);
    }

}
