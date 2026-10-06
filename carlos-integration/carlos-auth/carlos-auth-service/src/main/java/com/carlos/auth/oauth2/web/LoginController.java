package com.carlos.auth.oauth2.web;

import com.carlos.auth.config.OAuth2Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.stream.Stream;

/**
 * OAuth2 登录页控制器
 *
 * <p>提供授权码模式所需的浏览器端登录页面渲染。</p>
 *
 * <p><strong>流程说明：</strong></p>
 * <ol>
 *   <li>用户浏览器访问第三方应用的授权链接</li>
 *   <li>重定向到 {@code /oauth2/authorize}，若未认证则 Spring Security 重定向到 {@code /login}</li>
 *   <li>本控制器渲染登录表单页面</li>
 *   <li>用户提交表单，由 Spring Security {@code UsernamePasswordAuthenticationFilter} 处理 POST {@code /login}</li>
 *   <li>认证成功后重定向回 {@code /oauth2/authorize} 继续授权流程</li>
 * </ol>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-05-19
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class LoginController {

    private final OAuth2Properties oauth2Properties;
    private final LoginMethodValidator methodValidator;

    /**
     * 渲染登录页面
     *
     * @param error 登录失败错误标识（由 Spring Security 在认证失败后重定向附带）
     * @param model Spring MVC 模型
     * @return 登录视图名
     */
    @GetMapping("/login")
    public String login(
            @RequestParam(name = "error", required = false) String error,
            Model model) {

        if (error != null) {
            log.debug("Login page rendered with error flag");
            model.addAttribute("error", "用户名或密码错误，请重试");
        }

        OAuth2Properties.Methods methods = oauth2Properties.getLogin().getMethods();
        OAuth2Properties.Social social = methods.getSocial();

        // 基础本地登录方式
        boolean passwordEnabled = methods.isPassword();
        boolean smsEnabled = methods.isSmsCode();

        // 需要额外依赖的登录方式：开关 + 配置完整性校验，缺配置时自动降级
        boolean qrcodeEnabled = methods.isQrCode() && methodValidator.isQrCodeReady();
        boolean passkeyEnabled = methods.isPasskey() && methodValidator.isPasskeyReady();
        boolean dingtalkEnabled = methodValidator.isDingtalkReady();
        boolean wechatEnabled = methodValidator.isWechatReady();

        // 记录被自动降级的登录方式，便于运维排查
        logDowngradedMethods(methods, qrcodeEnabled, passkeyEnabled, dingtalkEnabled, wechatEnabled);

        model.addAttribute("passwordEnabled", passwordEnabled);
        model.addAttribute("smsEnabled", smsEnabled);
        model.addAttribute("qrcodeEnabled", qrcodeEnabled);
        model.addAttribute("passkeyEnabled", passkeyEnabled);
        model.addAttribute("dingtalkEnabled", dingtalkEnabled);
        model.addAttribute("wechatEnabled", wechatEnabled);
        model.addAttribute("socialEnabled", dingtalkEnabled || wechatEnabled);

        // 是否显示 Tab 栏（多于一种本地登录方式可用时才显示）
        long enabledCount = Stream.of(passwordEnabled, smsEnabled, qrcodeEnabled, passkeyEnabled)
                .filter(Boolean::booleanValue)
                .count();
        model.addAttribute("showTabs", enabledCount > 1);

        return "login";
    }

    /**
     * 记录被自动降级的登录方式
     *
     * <p>仅在方法开关已打开但实际不可用时输出 WARN，帮助运维快速定位配置缺失。</p>
     */
    private void logDowngradedMethods(
            OAuth2Properties.Methods methods,
            boolean qrcodeEnabled,
            boolean passkeyEnabled,
            boolean dingtalkEnabled,
            boolean wechatEnabled) {

        OAuth2Properties.Social social = methods.getSocial();

        if (methods.isQrCode() && !qrcodeEnabled) {
            log.warn("登录页自动降级：扫码登录开关已开启但配置不完整，本次请求不展示扫码登录");
        }
        if (methods.isPasskey() && !passkeyEnabled) {
            log.warn("登录页自动降级：Passkey 开关已开启但 WebAuthn 配置不完整，本次请求不展示 Passkey 登录");
        }
        if (social.getDingtalk().isEnabled() && !dingtalkEnabled) {
            log.warn("登录页自动降级：钉钉登录开关已开启但配置不完整，本次请求不展示钉钉登录");
        }
        if (social.getWechat().isEnabled() && !wechatEnabled) {
            log.warn("登录页自动降级：微信登录开关已开启但配置不完整，本次请求不展示微信登录");
        }
    }
}
