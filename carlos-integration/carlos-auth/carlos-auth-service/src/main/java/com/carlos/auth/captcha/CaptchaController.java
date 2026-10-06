package com.carlos.auth.captcha;

import com.carlos.auth.constant.AuthRedisKey;
import com.carlos.auth.util.SensitiveDataUtil;
import com.carlos.core.response.Result;
import com.carlos.redis.ratelimit.RateLimitUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 验证码控制器
 * </p>
 *
 * <p>提供短信/邮箱验证码的发送、验证功能</p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/captcha")
@Tag(name = "验证码", description = "短信/邮箱验证码发送和验证")
public class CaptchaController {

    private final CaptchaService captchaService;

    /**
     * 发送短信验证码
     *
     * @param request 发送请求
     * @return 发送结果
     */
    @Operation(summary = "发送短信验证码", description = "向指定手机号发送6位数字验证码，60秒间隔，每日最多10条")
    @PostMapping("/sms/send")
    public Result<String> sendSmsCaptcha(@Valid @RequestBody SendCaptchaRequest request) {
        String phone = request.getPhone();
        log.info("SMS captcha send request for phone: {}", SensitiveDataUtil.maskPhone(phone));

        // 检查发送限制（3次/分钟）
        String rateLimitKey = AuthRedisKey.rateCaptchaSms(phone);
        if (!RateLimitUtil.tryAcquire(rateLimitKey, 3, 1, TimeUnit.MINUTES)) {
            log.warn("SMS captcha send rate limit exceeded for phone: {}", SensitiveDataUtil.maskPhone(phone));
            return Result.error("发送过于频繁，请稍后再试");
        }

        // 检查每日上限
        if (!captchaService.canSendCaptcha("sms", phone)) {
            log.warn("SMS captcha daily limit exceeded for phone: {}", SensitiveDataUtil.maskPhone(phone));
            return Result.error("今日发送次数已达上限");
        }

        try {
            // 发送验证码
            boolean success = captchaService.sendSmsCaptcha(phone);

            if (success) {
                // 增加发送计数
                captchaService.incrementSendCount("sms", phone);

                log.info("SMS captcha sent successfully to: {}", SensitiveDataUtil.maskPhone(phone));
                return Result.success("验证码发送成功");
            } else {
                log.error("Failed to send SMS captcha to: {}", SensitiveDataUtil.maskPhone(phone));
                return Result.error("验证码发送失败");
            }

        } catch (Exception e) {
            log.error("Error sending SMS captcha to: {}", SensitiveDataUtil.maskPhone(phone), e);
            return Result.error("验证码发送失败，请稍后重试");
        }
    }

    /**
     * 发送邮箱验证码
     *
     * @param request 发送请求
     * @return 发送结果
     */
    @Operation(summary = "发送邮箱验证码", description = "向指定邮箱发送6位数字验证码，60秒间隔，每日最多10条")
    @PostMapping("/email/send")
    public Result<String> sendEmailCaptcha(@Valid @RequestBody SendEmailCaptchaRequest request) {
        String email = request.getEmail();
        log.info("Email captcha send request for: {}", SensitiveDataUtil.maskEmail(email));

        // 检查发送限制（3次/分钟）
        String rateLimitKey = AuthRedisKey.rateCaptchaEmail(email);
        if (!RateLimitUtil.tryAcquire(rateLimitKey, 3, 1, TimeUnit.MINUTES)) {
            log.warn("Email captcha send rate limit exceeded for: {}", SensitiveDataUtil.maskEmail(email));
            return Result.error("发送过于频繁，请稍后再试");
        }

        // 检查每日上限
        if (!captchaService.canSendCaptcha("email", email)) {
            log.warn("Email captcha daily limit exceeded for: {}", SensitiveDataUtil.maskEmail(email));
            return Result.error("今日发送次数已达上限");
        }

        try {
            boolean success = captchaService.sendEmailCaptcha(email);

            if (success) {
                captchaService.incrementSendCount("email", email);

                log.info("Email captcha sent successfully to: {}", SensitiveDataUtil.maskEmail(email));
                return Result.success("验证码发送成功");
            } else {
                log.error("Failed to send email captcha to: {}", SensitiveDataUtil.maskEmail(email));
                return Result.error("验证码发送失败");
            }

        } catch (Exception e) {
            log.error("Error sending email captcha to: {}", SensitiveDataUtil.maskEmail(email), e);
            return Result.error("验证码发送失败，请稍后重试");
        }
    }

    /**
     * 验证短信验证码
     *
     * @param request 验证请求
     * @return 验证结果
     */
    @Operation(summary = "验证短信验证码", description = "验证短信验证码并自动注册用户（如果不存在）")
    @PostMapping("/sms/verify")
    public Result<CaptchaLoginResponse> verifySmsCaptcha(@Valid @RequestBody VerifyCaptchaRequest request) {
        String phone = request.getPhone();
        String captcha = request.getCaptcha();

        log.info("SMS captcha verification request for phone: {}", SensitiveDataUtil.maskPhone(phone));

        try {
            // 验证验证码
            boolean isValid = captchaService.verifySmsCaptcha(phone, captcha);

            if (!isValid) {
                log.warn("SMS captcha verification failed for phone: {}", SensitiveDataUtil.maskPhone(phone));
                return Result.error("验证码错误或已过期");
            }

            log.info("SMS captcha verified successfully for phone: {}", SensitiveDataUtil.maskPhone(phone));

            // TODO: 查询用户，如果不存在则自动注册
            // TODO: 生成并返回JWT令牌

            return Result.success(
                CaptchaLoginResponse.builder()
                    .message("验证成功")
                    .requiresRegistration(false) // 根据实际逻辑判断
                    .build(),
                "验证成功"
            );

        } catch (Exception e) {
            log.error("Error verifying SMS captcha for phone: {}", SensitiveDataUtil.maskPhone(phone), e);
            return Result.error("验证失败，请稍后重试");
        }
    }

    /**
     * 发送验证码请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendCaptchaRequest {

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        @Parameter(description = "手机号", example = "13800000000")
        private String phone;
    }

    /**
     * 发送邮箱验证码请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendEmailCaptchaRequest {

        @NotBlank(message = "邮箱不能为空")
        @Pattern(regexp = "^[\\w-]+(\\.[\\w-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$", message = "邮箱格式不正确")
        @Parameter(description = "邮箱", example = "user@example.com")
        private String email;
    }

    /**
     * 验证验证码请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyCaptchaRequest {

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        @Parameter(description = "手机号", example = "13800000000")
        private String phone;

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "\\d{4,6}", message = "验证码必须为4-6位数字")
        @Parameter(description = "验证码（4-6位数字）", example = "123456")
        private String captcha;
    }

    /**
     * 验证码登录响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CaptchaLoginResponse {

        @Parameter(description = "消息", example = "验证成功")
        private String message;

        @Parameter(description = "是否需要注册", example = "false")
        private Boolean requiresRegistration;

        @Parameter(description = "访问令牌", example = "eyJhb...")
        private String accessToken;

        @Parameter(description = "刷新令牌", example = "eyJhb...")
        private String refreshToken;
    }
}
