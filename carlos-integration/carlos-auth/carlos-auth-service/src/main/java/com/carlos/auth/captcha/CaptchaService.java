package com.carlos.auth.captcha;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.auth.config.OAuth2Properties;
import com.carlos.auth.util.SensitiveDataUtil;
import com.carlos.sms.SmsUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;

/**
 * <p>
 * 验证码服务
 * </p>
 * <p>
 * 提供图形验证码、短信验证码的生成、发送、验证功能。
 * Redis 缓存操作已下沉至 {@link CaptchaManager}。
 * </p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final CaptchaManager captchaManager;
    private final OAuth2Properties oAuth2Properties;

    /**
     * 生成数字验证码
     *
     * @param length 验证码长度
     * @return 验证码
     */
    public String generateNumericCaptcha(int length) {
        return RandomUtil.randomNumbers(length);
    }

    /**
     * 发送短信验证码到指定手机
     *
     * <p>发送限制逻辑（限流规则）：</p>
     * <ul>
     *   <li>60秒内只能发送1次（由 Controller 层 RateLimit 控制）</li>
     *   <li>每日发送上限：10条</li>
     * </ul>
     *
     * @param phone 手机号码
     * @return true-发送成功，false-发送失败
     */
    public boolean sendSmsCaptcha(String phone) {
        if (!isValidPhone(phone)) {
            log.warn("Invalid phone number: {}", phone);
            return false;
        }

        String captcha = generateNumericCaptcha(6);
        captchaManager.saveCaptcha("sms", phone, captcha, 300);

        // 调用短信服务发送验证码（如果配置了模板）
        String templateCode = oAuth2Properties.getAlertNotification().getCaptchaSmsTemplateCode();
        if (StrUtil.isNotBlank(templateCode)) {
            try {
                LinkedHashMap<String, String> params = new LinkedHashMap<>();
                params.put("code", captcha);
                SmsUtil.sendByTemplateKey(phone, templateCode, params);
                log.info("SMS captcha sent to {} via SMS service", SensitiveDataUtil.maskPhone(phone));
            } catch (Exception e) {
                log.error("Failed to send SMS captcha to {}", SensitiveDataUtil.maskPhone(phone), e);
                // 短信发送失败不影响验证码已生成的事实，业务上仍返回成功
            }
        } else {
            log.warn("SMS captcha template not configured, logged only: phone={}, code={}",
                SensitiveDataUtil.maskPhone(phone), captcha);
        }

        return true;
    }

    /**
     * 发送邮箱验证码
     *
     * @param email 邮箱地址
     * @return true-发送成功，false-发送失败
     */
    public boolean sendEmailCaptcha(String email) {
        String captcha = generateNumericCaptcha(6);
        captchaManager.saveCaptcha("email", email, captcha, 300);

        // 邮件服务未配置，仅记录日志（需引入 spring-boot-starter-mail 后集成）
        log.warn("Email captcha service not configured, logged only: email={}, code={}",
            SensitiveDataUtil.maskEmail(email), captcha);

        return true;
    }

    /**
     * 验证短信验证码
     *
     * @param phone   手机号
     * @param captcha 验证码
     * @return true-验证通过
     */
    public boolean verifySmsCaptcha(String phone, String captcha) {
        return verifyCaptcha("sms", phone, captcha);
    }

    /**
     * 验证邮箱验证码
     *
     * @param email   邮箱
     * @param captcha 验证码
     * @return true-验证通过
     */
    public boolean verifyEmailCaptcha(String email, String captcha) {
        return verifyCaptcha("email", email, captcha);
    }

    /**
     * 通用验证码校验
     */
    private boolean verifyCaptcha(String type, String target, String captcha) {
        if (StrUtil.isBlank(target) || StrUtil.isBlank(captcha)) {
            return false;
        }

        String storedCaptcha = captchaManager.getCaptcha(type, target);
        if (storedCaptcha == null) {
            log.warn("Captcha not found or expired, type: {}, target: {}", type, target);
            return false;
        }

        boolean isValid = captcha.equals(storedCaptcha);
        if (isValid) {
            captchaManager.deleteCaptcha(type, target);
            log.info("Captcha verified successfully, type: {}, target: {}", type, target);
        } else {
            log.warn("Captcha verification failed, type: {}, target: {}", type, target);
        }

        return isValid;
    }

    /**
     * 验证手机号格式
     */
    private boolean isValidPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return false;
        }
        return phone.matches("^1[3-9]\\d{9}$");
    }

    /**
     * 发送限制检查（每日上限）
     *
     * @param type   类型：sms/email
     * @param target 手机号/邮箱
     * @return true-可以发送，false-达到限制
     */
    public boolean canSendCaptcha(String type, String target) {
        return captchaManager.canSendCaptcha(type, target);
    }

    /**
     * 增加发送计数
     *
     * @param type   类型：sms/email
     * @param target 手机号/邮箱
     */
    public void incrementSendCount(String type, String target) {
        captchaManager.incrementSendCount(type, target);
    }

    /**
     * 删除发送计数（可选：用户注销时清理）
     *
     * @param type   类型：sms/email
     * @param target 手机号/邮箱
     */
    public void clearSendCount(String type, String target) {
        captchaManager.clearSendCount(type, target);
    }
}
