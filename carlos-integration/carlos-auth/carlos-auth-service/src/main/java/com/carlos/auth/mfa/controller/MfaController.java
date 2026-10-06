package com.carlos.auth.mfa.controller;

import com.carlos.auth.login.UserLoginService;
import com.carlos.auth.login.dto.LoginResponse;
import com.carlos.auth.mfa.convert.MfaConvert;
import com.carlos.auth.mfa.pojo.dto.MfaSetupDTO;
import com.carlos.auth.mfa.pojo.param.MfaStepUpVerifyParam;
import com.carlos.auth.mfa.pojo.param.MfaVerifyParam;
import com.carlos.auth.mfa.pojo.vo.MfaSetupVO;
import com.carlos.auth.mfa.pojo.vo.MfaStatusVO;
import com.carlos.auth.mfa.service.MfaService;
import com.carlos.auth.provider.UserInfo;
import com.carlos.auth.provider.UserProvider;
import com.carlos.auth.util.IpLocationUtil;
import com.carlos.core.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * MFA 控制器
 *
 * <p>提供多因素认证相关接口，包括 MFA 设置、验证、启用/禁用等。</p>
 *
 * <p><strong>分层职责：</strong></p>
 * <ul>
 *   <li>Controller (此类): 接收 Web 请求，参数校验，调用 Service</li>
 *   <li>Service ({@link com.carlos.auth.mfa.service.MfaService}): 处理业务逻辑</li>
 *   <li>Manager ({@link com.carlos.auth.mfa.manager.MfaManager}): 数据查询封装</li>
 *   <li>Mapper: 数据访问</li>
 * </ul>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-02-26
 * @see com.carlos.auth.mfa.service.MfaService
 * @see com.carlos.auth.mfa.manager.MfaManager
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/mfa")
@Tag(name = "多因素认证(MFA)", description = "MFA设置、验证、恢复码管理")
public class MfaController {

    private final MfaService mfaService;
    private final UserLoginService userLoginService;
    private final UserProvider userProvider;
    private final IpLocationUtil ipLocationUtil;

    /**
     * 获取MFA设置信息（密钥和QR码）
     *
     * @param authentication 当前认证信息
     * @return MFA设置信息
     */
    @Operation(summary = "获取MFA设置信息", description = "生成MFA密钥和QR码，用于绑定Google Authenticator")
    @PreAuthorize("hasPermission('auth:mfa', 'bind')")
    @PostMapping("/setup")
    public Result<MfaSetupVO> setupMfa(Authentication authentication) {
        String username = authentication.getName();
        log.info("MFA setup requested for user: {}", username);

        MfaSetupDTO setupDTO = mfaService.generateMfaSetup(username);
        MfaSetupVO response = MfaConvert.INSTANCE.dtoToVo(setupDTO);

        return Result.success(response, "MFA设置信息生成成功");
    }

    /**
     * 验证并启用MFA
     *
     * @param authentication 当前认证信息
     * @param param 验证请求参数
     * @return 验证结果
     */
    @Operation(summary = "验证并启用MFA", description = "验证6位验证码，验证通过后启用MFA")
    @PreAuthorize("hasPermission('auth:mfa', 'bind')")
    @PostMapping("/verify")
    public Result<String> verifyMfa(Authentication authentication, @Valid @RequestBody MfaVerifyParam param) {
        String username = authentication.getName();
        log.info("MFA verification requested for user: {}", username);

        boolean success = mfaService.enableMfa(username, param.getSecret(), param.getCode());

        if (success) {
            log.info("MFA enabled successfully for user: {}", username);
            return Result.success("MFA启用成功");
        } else {
            log.warn("MFA verification failed for user: {}", username);
            return Result.error("验证码错误，请重试");
        }
    }

    /**
     * 禁用MFA
     *
     * @param authentication 当前认证信息
     * @return 禁用结果
     */
    @Operation(summary = "禁用MFA", description = "禁用MFA功能")
    @PreAuthorize("hasPermission('auth:mfa', 'unbind')")
    @PostMapping("/disable")
    public Result<String> disableMfa(Authentication authentication) {
        String username = authentication.getName();
        log.info("MFA disable requested for user: {}", username);

        mfaService.disableMfa(username);
        log.info("MFA disabled successfully for user: {}", username);
        return Result.success("MFA禁用成功");
    }

    /**
     * 重新生成MFA密钥
     *
     * @param authentication 当前认证信息
     * @return 新的MFA设置信息
     */
    @Operation(summary = "重新生成MFA密钥", description = "重置MFA密钥，需要重新绑定验证器")
    @PreAuthorize("hasPermission('auth:mfa', 'reset')")
    @PostMapping("/reset")
    public Result<MfaSetupVO> resetMfa(Authentication authentication) {
        String username = authentication.getName();
        log.info("MFA reset requested for user: {}", username);

        MfaSetupDTO setupDTO = mfaService.resetMfaSecret(username);
        MfaSetupVO response = MfaConvert.INSTANCE.dtoToVo(setupDTO);

        return Result.success(response, "MFA密钥重置成功");
    }

    /**
     * 检查MFA状态
     *
     * @param authentication 当前认证信息
     * @return MFA状态
     */
    @Operation(summary = "检查MFA状态", description = "检查当前用户是否启用了MFA")
    @PreAuthorize("hasPermission('auth:mfa', 'view')")
    @GetMapping("/status")
    public Result<MfaStatusVO> getMfaStatus(Authentication authentication) {
        String username = authentication.getName();

        boolean enabled = mfaService.isMfaEnabled(username);
        MfaStatusVO response = MfaConvert.INSTANCE.buildStatusVO(enabled);

        return Result.success(response);
    }

    /**
     * MFA Step-Up 验证（登录第二因素）
     *
     * <p>密码认证通过后，如果需要 MFA，前端携带 mfaToken + MFA code 调用此接口。
     * 验证通过后签发正式 Access Token + Refresh Token。</p>
     *
     * <p><strong>无需认证</strong>（mfaToken 本身作为认证凭证）。</p>
     *
     * @param param       验证参数
     * @param httpRequest HTTP 请求
     * @return 登录响应（含 Token）
     */
    @Operation(summary = "MFA Step-Up 验证", description = "登录第二因素验证，通过后签发 Token")
    @PostMapping("/step-up-verify")
    public Result<LoginResponse> stepUpVerify(@Valid @RequestBody MfaStepUpVerifyParam param,
                                               HttpServletRequest httpRequest) {
        log.info("MFA step-up verify requested");

        // 验证 mfaToken + MFA code
        String verifiedPayload = mfaService.verifyMfaStepUp(param.getMfaToken(), param.getCode());
        if (verifiedPayload == null) {
            return Result.error("MFA 验证失败，验证码错误或令牌已过期");
        }

        // 解析 username 和 clientId
        String username = verifiedPayload.contains(":")
            ? verifiedPayload.substring(0, verifiedPayload.indexOf(':')) : verifiedPayload;
        String clientId = verifiedPayload.contains(":")
            ? verifiedPayload.substring(verifiedPayload.indexOf(':') + 1) : "default";

        // 加载用户信息
        UserInfo user = userProvider.loadUserByIdentifier(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 标记为受信任设备（如果请求要求）
        if (Boolean.TRUE.equals(param.getTrustDevice())
            && param.getDeviceFingerprint() != null
            && !param.getDeviceFingerprint().isBlank()) {
            String clientIp = ipLocationUtil.getClientIp(httpRequest);
            String location = ipLocationUtil.getLocation(clientIp);
            mfaService.addTrustedDevice(
                user.getUserId(),
                param.getDeviceFingerprint(),
                param.getDeviceName(),
                clientIp,
                location,
                httpRequest.getHeader("User-Agent")
            );
        }

        // 签发正式 Token
        LoginResponse response = userLoginService.issueTokenAfterMfa(user, clientId, httpRequest);

        log.info("MFA step-up verify successful, token issued for user: {}", username);
        return Result.success(response, "MFA 验证成功");
    }

    /**
     * 获取受信任设备列表
     *
     * @param authentication 当前认证信息
     * @return 受信任设备列表
     */
    @Operation(summary = "获取受信任设备列表", description = "查询当前用户的所有受信任设备")
    @PreAuthorize("hasPermission('auth:mfa', 'view')")
    @GetMapping("/devices")
    public Result<?> listTrustedDevices(Authentication authentication) {
        UserInfo user = userProvider.loadUserByIdentifier(authentication.getName());
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(mfaService.getTrustedDevices(user.getUserId()));
    }

    /**
     * 移除受信任设备
     *
     * @param authentication 当前认证信息
     * @param deviceId       设备 ID
     * @return 操作结果
     */
    @Operation(summary = "移除受信任设备", description = "撤销指定受信任设备（踢出设备）")
    @PreAuthorize("hasPermission('auth:mfa', 'unbind')")
    @DeleteMapping("/devices/{deviceId}")
    public Result<String> removeTrustedDevice(Authentication authentication,
                                                @PathVariable Long deviceId) {
        UserInfo user = userProvider.loadUserByIdentifier(authentication.getName());
        if (user == null) {
            return Result.error("用户不存在");
        }
        mfaService.removeTrustedDevice(user.getUserId(), deviceId);
        return Result.success("设备已移除");
    }
}
