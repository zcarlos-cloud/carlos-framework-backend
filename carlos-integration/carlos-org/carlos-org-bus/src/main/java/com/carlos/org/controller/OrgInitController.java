package com.carlos.org.controller;

import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.Result;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.org.config.OrgInitProperties;
import com.carlos.org.config.OrgProperties;
import com.carlos.org.pojo.param.OrgInitSuperAdminParam;
import com.carlos.org.service.OrgInitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.carlos.core.auth.SignatureUtil;

/**
 * <p>
 * 系统初始化 REST 接口
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("org/init")
@Validated
@Tag(name = "系统初始化")
public class OrgInitController {

    private final OrgInitService initService;

    private final OrgProperties orgProperties;


    /**
     * 初始化超级管理员
     *
     * <p>前置条件：</p>
     * <ul>
     *     <li>配置项 {@code carlos.org.init.enabled=true} 已开启</li>
     *     <li>请求头 {@code X-Init-Token} 与配置 Token 一致（常量时间比较）</li>
     *     <li>系统中不存在任何用户</li>
     * </ul>
     *
     * @param token 请求头中的初始化 Token
     * @param param 入参（必须传入初始密码）
     * @return 初始化结果
     */
    @Log(
        title = "系统初始化",
        logType = "ORG_INIT_SUPER_ADMIN",
        businessType = BusinessType.INSERT,
        operation = "'初始化超级管理员'",
        targetType = "ORG_USER",
        riskLevel = 70
    )
    @PostMapping("superAdmin")
    @Operation(summary = "初始化超级管理员")
    public Result<String> initSuperAdmin(
            @RequestHeader(value = "X-Init-Token", required = false) String token,
            @RequestBody @Valid OrgInitSuperAdminParam param) {
        OrgInitProperties initProperties = orgProperties.getInit();
        if (!initProperties.isEnabled()) {
            throw new BusinessException("初始化接口未启用");
        }
        if (token == null || token.isEmpty()) {
            throw new BusinessException("初始化 Token 不能为空");
        }
        if (!SignatureUtil.constantTimeEquals(token, initProperties.getToken())) {
            throw new BusinessException("初始化 Token 无效");
        }
        initService.initSuperAdmin(param.getPassword());
        return Result.success("超级管理员初始化完成");
    }

}
