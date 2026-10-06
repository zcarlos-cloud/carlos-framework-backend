package com.carlos.auth.passkey;

import com.carlos.auth.login.UserLoginService;
import com.carlos.auth.login.dto.LoginResponse;
import com.carlos.auth.passkey.pojo.param.PasskeyAuthenticateFinishRequest;
import com.carlos.auth.passkey.pojo.param.PasskeyRegisterFinishRequest;
import com.carlos.core.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * WebAuthn/Passkey 控制器
 *
 * <p>实现 FIDO2/WebAuthn 的注册和认证流程，支持 Passkey 无密码登录。</p>
 *
 * <h3>注册流程：</h3>
 * <ol>
 *   <li>前端调用 {@code POST /auth/passkey/register/begin} 获取 challenge</li>
 *   <li>前端调用 {@code navigator.credentials.create()} 创建凭据</li>
 *   <li>前端将结果传给 {@code POST /auth/passkey/register/finish} 完成注册</li>
 * </ol>
 *
 * <h3>认证流程：</h3>
 * <ol>
 *   <li>前端调用 {@code POST /auth/passkey/authenticate/begin} 获取 challenge</li>
 *   <li>前端调用 {@code navigator.credentials.get()} 签名 challenge</li>
 *   <li>前端将结果传给 {@code POST /auth/passkey/authenticate/finish} 完成认证并获取 Token</li>
 * </ol>
 *
 * <p><strong>依赖：</strong>com.yubico:webauthn-server-core</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/passkey")
@ConditionalOnProperty(prefix = "carlos.auth.login.methods", name = "passkey", havingValue = "true")
@Tag(name = "Passkey/WebAuthn", description = "FIDO2 无密码认证：注册与登录")
public class PasskeyController {

    private final PasskeyService passkeyService;
    private final UserLoginService userLoginService;

    /**
     * 开始 Passkey 注册
     */
    @Operation(summary = "开始Passkey注册", description = "生成注册challenge返回给前端")
    @PreAuthorize("hasPermission('auth:passkey', 'register')")
    @PostMapping("/register/begin")
    public Result<Map<String, Object>> registerBegin(Authentication authentication) {
        String account = authentication.getName();
        log.info("Passkey registration begin for user: {}", account);
        Map<String, Object> options = passkeyService.beginRegistration(account);
        return Result.success(options, "请在设备上完成 Passkey 注册");
    }

    /**
     * 完成 Passkey 注册
     */
    @Operation(summary = "完成Passkey注册", description = "验证并保存Passkey凭据")
    @PreAuthorize("hasPermission('auth:passkey', 'register')")
    @PostMapping("/register/finish")
    public Result<String> registerFinish(Authentication authentication,
                                          @RequestBody @Valid PasskeyRegisterFinishRequest body) {
        String account = authentication.getName();
        log.info("Passkey registration finish for user: {}", account);
        boolean success = passkeyService.finishRegistration(account, body);
        if (success) {
            return Result.success("Passkey 注册成功");
        }
        return Result.error("Passkey 注册失败，请重试");
    }

    /**
     * 开始 Passkey 认证
     */
    @Operation(summary = "开始Passkey认证", description = "生成认证challenge返回给前端（无需登录态）")
    @PostMapping("/authenticate/begin")
    public Result<Map<String, Object>> authenticateBegin(
            @RequestParam(required = false) String username) {
        log.info("Passkey authentication begin for user: {}", username);
        Map<String, Object> options = passkeyService.beginAuthentication(username);
        return Result.success(options, "请使用 Passkey 进行验证");
    }

    /**
     * 完成 Passkey 认证 —— 验证签名并签发 Token
     */
    @Operation(summary = "完成Passkey认证", description = "验证Passkey签名并签发Token（无需登录态）")
    @PostMapping("/authenticate/finish")
    public Result<LoginResponse> authenticateFinish(
            @RequestBody @Valid PasskeyAuthenticateFinishRequest body) {
        log.info("Passkey authentication finish");
        String username = passkeyService.finishAuthentication(body);
        if (username == null) {
            return Result.error("Passkey 验证失败");
        }
        // 通过身份验证，签发 OAuth2 Token
        LoginResponse loginResponse = userLoginService.issueTokenForUser(username,
                userLoginService.getDefaultClientId());
        return Result.success(loginResponse);
    }

    /**
     * 获取用户已注册的 Passkey 列表
     */
    @Operation(summary = "获取Passkey列表", description = "查询当前用户已注册的Passkey凭据")
    @PreAuthorize("hasPermission('auth:passkey', 'view')")
    @GetMapping("/list")
    public Result<?> listPasskeys(Authentication authentication) {
        String account = authentication.getName();
        return Result.success(passkeyService.listCredentials(account));
    }

    /**
     * 删除 Passkey 凭据
     */
    @Operation(summary = "删除Passkey", description = "移除指定的Passkey凭据")
    @PreAuthorize("hasPermission('auth:passkey', 'delete')")
    @DeleteMapping("/{credentialId}")
    public Result<String> deletePasskey(Authentication authentication,
                                         @PathVariable String credentialId) {
        String account = authentication.getName();
        passkeyService.deleteCredential(account, credentialId);
        return Result.success("Passkey 已删除");
    }
}
