package com.carlos.auth.qrcode;

import com.carlos.auth.login.UserLoginService;
import com.carlos.auth.login.dto.LoginResponse;
import com.carlos.auth.provider.UserInfo;
import com.carlos.auth.provider.UserProvider;
import com.carlos.core.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 扫码登录控制器
 *
 * <p>提供 PC 端生成二维码、SSE 状态推送，APP 端扫码/确认/取消接口。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-01
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/auth/qrcode")
@Tag(name = "扫码登录", description = "PC端生成二维码、SSE状态推送、APP端扫码确认")
public class QrCodeController {

    private final QrCodeService qrCodeService;
    private final UserProvider userProvider;
    private final UserLoginService userLoginService;
    private final QrCodeProperties qrCodeProperties;

    private final ScheduledExecutorService sseExecutor = Executors.newScheduledThreadPool(4);

    /**
     * PC 端：生成扫码登录二维码
     *
     * @param clientId 客户端 ID（可选，默认使用系统配置）
     * @return 二维码信息
     */
    @Operation(summary = "生成扫码二维码", description = "PC端调用，返回二维码ID和URL")
    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@RequestParam(required = false) String clientId,
                                                 @RequestParam(required = false) String deviceFingerprint) {
        QrCodeSession session = qrCodeService.generate(clientId, deviceFingerprint);

        Map<String, Object> result = new HashMap<>();
        result.put("qrcodeId", session.getQrcodeId());
        result.put("qrcodeUrl", session.getQrcodeUrl());
        result.put("expiresAt", session.getExpiresAt());
        result.put("ttl", qrCodeProperties.getTtlSeconds());

        return Result.success(result, "二维码生成成功");
    }

    /**
     * PC 端：SSE 订阅二维码状态变更
     *
     * <p>Server-Sent Events 推送，状态变更时立即推送，无变更时每 2 秒发送心跳。</p>
     *
     * @param qrcodeId 二维码 ID
     * @return SSE 流
     */
    @Operation(summary = "订阅扫码状态", description = "SSE推送二维码状态变更")
    @GetMapping(value = "/status/{qrcodeId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter status(@PathVariable String qrcodeId) {
        // 超时设置为二维码 TTL + 10s 缓冲
        SseEmitter emitter = new SseEmitter((qrCodeProperties.getTtlSeconds() + 10) * 1000);

        sseExecutor.scheduleAtFixedRate(() -> {
            try {
                QrCodeSession session = qrCodeService.getSession(qrcodeId);
                if (session == null) {
                    // 会话不存在或已过期清理
                    emitter.send(SseEmitter.event()
                        .name("status")
                        .data(Map.of("state", "EXPIRED", "qrcodeId", qrcodeId)));
                    emitter.complete();
                    return;
                }

                Map<String, Object> data = new HashMap<>();
                data.put("state", session.getState().name());
                data.put("qrcodeId", qrcodeId);

                // CONFIRMED 状态附带 Token（PC 端拿到后即可登录）
                if (session.getState() == QrCodeState.CONFIRMED) {
                    UserInfo user = userProvider.loadUserByIdentifier(session.getScannedUsername());
                    if (user != null) {
                        LoginResponse tokenResp = userLoginService.issueTokenAfterMfa(
                            user, session.getClientId(), null);
                        data.put("accessToken", tokenResp.getAccessToken());
                        data.put("refreshToken", tokenResp.getRefreshToken());
                        data.put("expiresIn", tokenResp.getExpiresIn());
                    }
                }

                emitter.send(SseEmitter.event().name("status").data(data));

                // 终态关闭 SSE
                if (session.getState().isTerminal()) {
                    emitter.complete();
                }
            } catch (Exception e) {
                log.debug("SSE推送异常（客户端可能已断开）: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        }, 0, qrCodeProperties.getSsePollIntervalMs(), TimeUnit.MILLISECONDS);

        emitter.onCompletion(() -> log.debug("SSE completed: {}", qrcodeId));
        emitter.onTimeout(() -> {
            log.debug("SSE timeout: {}", qrcodeId);
            emitter.complete();
        });

        return emitter;
    }

    /**
     * APP 端：扫码
     *
     * <p>APP 用户已登录，扫描 PC 端二维码后调用此接口。</p>
     *
     * @param qrcodeId     二维码 ID
     * @param authentication APP 端用户认证信息
     * @return 操作结果
     */
    @Operation(summary = "APP扫码", description = "APP端已登录用户扫码")
    @PostMapping("/scan")
    public Result<String> scan(@RequestParam @NotBlank(message = "二维码ID不能为空") @Size(max = 64, message = "二维码ID长度不能超过64") String qrcodeId, Authentication authentication) {
        UserInfo user = userProvider.loadUserByIdentifier(authentication.getName());
        if (user == null) {
            return Result.error("用户不存在");
        }

        QrCodeSession session = qrCodeService.scan(qrcodeId, user.getUserId(), user.getUsername());
        if (session == null) {
            return Result.error("二维码无效或状态不可用");
        }

        return Result.success("扫码成功，请在 APP 中确认登录");
    }

    /**
     * APP 端：确认登录
     *
     * @param qrcodeId     二维码 ID
     * @param authentication APP 端用户认证信息
     * @return 操作结果
     */
    @Operation(summary = "APP确认登录", description = "APP端确认在PC端登录")
    @PostMapping("/confirm")
    public Result<String> confirm(@RequestParam @NotBlank(message = "二维码ID不能为空") @Size(max = 64, message = "二维码ID长度不能超过64") String qrcodeId, Authentication authentication) {
        UserInfo user = userProvider.loadUserByIdentifier(authentication.getName());
        if (user == null) {
            return Result.error("用户不存在");
        }

        QrCodeSession session = qrCodeService.confirm(qrcodeId, user.getUserId());
        if (session == null) {
            return Result.error("确认失败，二维码无效或状态不可用");
        }

        return Result.success("已确认登录");
    }

    /**
     * APP 端：取消登录
     *
     * @param qrcodeId 二维码 ID
     * @return 操作结果
     */
    @Operation(summary = "APP取消登录", description = "APP端取消在PC端登录")
    @PostMapping("/cancel")
    public Result<String> cancel(@RequestParam @NotBlank(message = "二维码ID不能为空") @Size(max = 64, message = "二维码ID长度不能超过64") String qrcodeId) {
        QrCodeSession session = qrCodeService.cancel(qrcodeId);
        if (session == null) {
            return Result.error("取消失败，二维码无效");
        }
        return Result.success("已取消登录");
    }
}
