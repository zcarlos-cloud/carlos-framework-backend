package com.carlos.auth.oauth2.web;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.auth.config.OAuth2Properties;
import com.carlos.auth.qrcode.QrCodeProperties;
import com.carlos.integration.module.dingtalk.service.DingtalkRestClientService;
import com.yubico.webauthn.RelyingParty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 登录方式可用性校验器
 *
 * <p>校验各登录方式的开关与依赖配置是否完整。若某方式已开启但关键配置缺失，
 * 则视为该方式不可用，由 {@link LoginController} 在渲染登录页时自动降级（不展示）。</p>
 *
 * <p>设计目的：避免运维误开启某个登录方式后，用户点击却得到错误响应。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginMethodValidator {

    private static final String QRCODE_DEFAULT_SECRET = "carlos-qrcode-default-secret";

    private final OAuth2Properties oauth2Properties;
    private final QrCodeProperties qrCodeProperties;
    private final ApplicationContext applicationContext;

    /**
     * 钉钉登录是否真正可用
     *
     * <p>需要满足：</p>
     * <ol>
     *   <li>enabled = true</li>
     *   <li>当前应用自身配置了 appKey/appSecret/corpId，或 carlos-integration 钉钉模块提供了
     *       DingtalkRestClientService Bean（复用其配置）</li>
     * </ol>
     *
     * @return true 表示钉钉登录可正常展示
     */
    public boolean isDingtalkReady() {
        OAuth2Properties.DingTalk dingTalk = oauth2Properties.getLogin().getMethods().getSocial().getDingtalk();
        if (!dingTalk.isEnabled()) {
            return false;
        }

        boolean hasLocalConfig = StrUtil.isAllNotBlank(dingTalk.getAppKey(), dingTalk.getAppSecret(), dingTalk.getCorpId());
        boolean hasIntegrationBean = applicationContext.getBeansOfType(DingtalkRestClientService.class).size() > 0;

        if (!hasLocalConfig && !hasIntegrationBean) {
            log.warn("钉钉登录开关已启用，但缺少必要配置："
                    + "carlos.auth.login.methods.social.dingtalk.app-key/app-secret/corp-id 未配置，"
                    + "且未检测到 DingtalkRestClientService Bean（carlos-integration 钉钉模块未启用或配置不完整）");
            return false;
        }

        return true;
    }

    /**
     * 微信登录是否真正可用
     *
     * <p>需要满足 enabled = true 且 appId/appSecret 齐全。</p>
     *
     * @return true 表示微信登录可正常展示
     */
    public boolean isWechatReady() {
        OAuth2Properties.WeChat wechat = oauth2Properties.getLogin().getMethods().getSocial().getWechat();
        if (!wechat.isEnabled()) {
            return false;
        }

        if (StrUtil.isAllBlank(wechat.getAppId(), wechat.getAppSecret())) {
            log.warn("微信登录开关已启用，但缺少必要配置："
                    + "carlos.auth.login.methods.social.wechat.app-id/app-secret 未配置");
            return false;
        }

        return true;
    }

    /**
     * 扫码登录是否真正可用
     *
     * <p>需要满足：</p>
     * <ol>
     *   <li>login.methods.qrCode = true</li>
     *   <li>carlos.auth.qrcode.sign-secret 已配置且不是默认占位值</li>
     * </ol>
     *
     * @return true 表示扫码登录可正常展示
     */
    public boolean isQrCodeReady() {
        OAuth2Properties.Methods methods = oauth2Properties.getLogin().getMethods();
        if (!methods.isQrCode()) {
            return false;
        }

        String signSecret = qrCodeProperties.getSignSecret();
        if (StrUtil.isBlank(signSecret)) {
            log.warn("扫码登录开关已启用，但 carlos.auth.qrcode.sign-secret 未配置");
            return false;
        }

        // 未解析的占位符（形如 ${XXX:default} 但 default 仍为空）或默认弱密钥均视为未配置
        if (signSecret.contains("${")) {
            log.warn("扫码登录开关已启用，但 carlos.auth.qrcode.sign-secret 包含未解析的占位符");
            return false;
        }

        if (QRCODE_DEFAULT_SECRET.equals(signSecret)) {
            log.warn("扫码登录开关已启用，但 carlos.auth.qrcode.sign-secret 仍为默认值，请配置正式密钥");
            return false;
        }

        return true;
    }

    /**
     * Passkey 登录是否真正可用
     *
     * <p>需要满足：</p>
     * <ol>
     *   <li>login.methods.passkey = true</li>
     *   <li>passkey-config.rpId 已配置</li>
     *   <li>passkey-config.origins 非空</li>
     *   <li>classpath 有 com.yubico:webauthn-server-core（RelyingParty Bean 可用）</li>
     * </ol>
     *
     * @return true 表示 Passkey 登录可正常展示
     */
    public boolean isPasskeyReady() {
        if (!oauth2Properties.getLogin().getMethods().isPasskey()) {
            return false;
        }

        OAuth2Properties.PasskeyConfig config =
                oauth2Properties.getLogin().getMethods().getPasskeyConfig();
        if (config == null || StrUtil.isBlank(config.getRpId())
                || CollUtil.isEmpty(config.getOrigins())) {
            log.warn("Passkey 登录开关已启用，但缺少 RP 配置（rpId/origins 未配置），已自动降级");
            return false;
        }

        try {
            applicationContext.getBean(RelyingParty.class);
        } catch (NoSuchBeanDefinitionException e) {
            log.warn("Passkey 登录开关已启用，但 RelyingParty Bean 未就绪"
                    + "（缺少 com.yubico:webauthn-server-core 依赖），已自动降级");
            return false;
        }

        return true;
    }
}
