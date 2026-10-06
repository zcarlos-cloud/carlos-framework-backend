package com.carlos.boot.web.request;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * 用户客户端信息工具类（从 User-Agent 解析浏览器、操作系统、设备信息）。
 * </p>
 *
 * @author carlos
 * @date 2020/4/10 16:15
 */
@Slf4j
public final class UserAgentParser {

    private static final Pattern DEVICE_INFO_PATTERN = Pattern.compile(";\\s?(\\S*?\\s?\\S*?)\\s?Build/(\\S*?)[;)]");
    private static final Pattern DEVICE_INFO_PATTERN_1 = Pattern.compile(";\\s?(\\S*?\\s?\\S*?)\\s?\\)");

    private UserAgentParser() {
        throw new AssertionError();
    }

    /**
     * <p>
     * 用户客户端信息对象
     * </p>
     *
     * @author carlos
     * @date 2020/4/14 11:44
     */
    @Data
    public static class ClientInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * ip
         */
        private String ip;

        /**
         * 浏览器名称
         */
        private String browserName;

        /**
         * 浏览器版本
         */
        private String browserVersion;

        /**
         * 浏览器引擎名称
         */
        private String engineName;

        /**
         * 浏览器引擎版本
         */
        private String engineVersion;

        /**
         * 系统名称
         */
        private String osName;

        /**
         * 平台名称
         */
        private String platformName;

        /**
         * 移动端设备名称
         */
        private String mobileName;

        /**
         * 移动端设备型号
         */
        private String mobileModel;

    }

    /**
     * 获取访问客户端信息
     *
     * @param userAgentString User-Agent 头信息
     * @return ClientInfo 客户端信息，输入为 null 时返回 null
     * @author carlos
     * @date 2020/4/14 11:47
     */
    public static ClientInfo parse(String userAgentString) {
        if (userAgentString == null) {
            log.warn("The 'User-Agent' can't be null in request header!");
            return null;
        }
        ClientInfo clientInfo = new ClientInfo();
        UserAgent userAgent = UserAgentUtil.parse(userAgentString);
        // 浏览器名称
        clientInfo.setBrowserName(userAgent.getBrowser().getName());
        // 浏览器版本
        clientInfo.setBrowserVersion(userAgent.getVersion());
        // 浏览器引擎名称
        clientInfo.setEngineName(userAgent.getEngine().getName());
        // 浏览器引擎版本
        clientInfo.setEngineVersion(userAgent.getEngineVersion());
        // 用户操作系统名称
        clientInfo.setOsName(userAgent.getOs().getName());
        // 用户操作平台名称
        clientInfo.setPlatformName(userAgent.getPlatform().getName());
        if (userAgent.isMobile()) {
            // 追加移动设备名称和机型
            appendMobileInfo(clientInfo, userAgentString);
        } else {
            clientInfo.setMobileModel("");
            clientInfo.setMobileName("");
        }
        return clientInfo;
    }

    /**
     * <p>
     * 获取移动端用户设备的名称和机型。
     * </p>
     *
     * @param clientInfo      客户端信息对象
     * @param userAgentString User-Agent 字符串
     * @author carlos
     * @date 2020/4/14 11:45
     */
    private static void appendMobileInfo(ClientInfo clientInfo, String userAgentString) {
        try {
            Matcher matcher = DEVICE_INFO_PATTERN.matcher(userAgentString);
            String model = null;
            String name = null;

            if (matcher.find()) {
                model = matcher.group(1);
                name = matcher.group(2);
            }

            if (model == null && name == null) {
                matcher = DEVICE_INFO_PATTERN_1.matcher(userAgentString);
                if (matcher.find()) {
                    model = matcher.group(1);
                }
            }
            clientInfo.setMobileName(name);
            clientInfo.setMobileModel(model);
        } catch (Exception e) {
            log.error("解析移动端设备信息失败", e);
        }
    }

}
