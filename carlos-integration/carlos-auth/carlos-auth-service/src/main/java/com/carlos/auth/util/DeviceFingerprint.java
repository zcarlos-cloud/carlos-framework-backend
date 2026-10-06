package com.carlos.auth.util;

import cn.hutool.crypto.SecureUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 设备指纹工具类
 * </p>
 *
 * <p>根据User-Agent、IP等信息生成设备指纹，用于识别设备</p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
@Component
@RequiredArgsConstructor
public class DeviceFingerprint {

    /**
     * 生成设备指纹（简化版）
     * <p>
     * 注意：完整的设备指纹需要前端配合提供更多信息（屏幕分辨率、时区、Canvas指纹等）
     * 这里根据请求头生成基础指纹
     *
     * @param request HTTP请求
     * @return 设备指纹（MD5哈希）
     */
    public String generate(HttpServletRequest request) {
        String userAgent = getUserAgent(request);
        String accept = request.getHeader("Accept");
        String acceptLanguage = request.getHeader("Accept-Language");
        String acceptEncoding = request.getHeader("Accept-Encoding");

        // 组合设备特征字符串
        String fingerprintData = "%s|%s|%s|%s".formatted(
            userAgent != null ? userAgent : "",
            accept != null ? accept : "",
            acceptLanguage != null ? acceptLanguage : "",
            acceptEncoding != null ? acceptEncoding : ""
        );

        // 生成MD5指纹
        return SecureUtil.md5(fingerprintData);
    }

    /**
     * 生成增强版设备指纹（建议前端提供额外信息）
     *
     * @param request HTTP请求
     * @param screenWidth 屏幕宽度（前端提供）
     * @param screenHeight 屏幕高度（前端提供）
     * @param timezone 时区（前端提供）
     * @param platform 平台（前端提供）
     * @return 设备指纹
     */
    public String generateEnhanced(HttpServletRequest request,
                                   Integer screenWidth,
                                   Integer screenHeight,
                                   String timezone,
                                   String platform) {
        String basicFingerprint = generate(request);
        String enhancedData = "%s|%s|%s|%s|%s".formatted(
            basicFingerprint,
            screenWidth != null ? screenWidth : "",
            screenHeight != null ? screenHeight : "",
            timezone != null ? timezone : "",
            platform != null ? platform : ""
        );

        return SecureUtil.md5(enhancedData);
    }

    /**
     * 获取User-Agent
     *
     * @param request HTTP请求
     * @return User-Agent字符串
     */
    public String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    /**
     * 判断是否为移动设备
     *
     * @param userAgent User-Agent字符串
     * @return true-移动设备，false-桌面设备
     */
    public boolean isMobileDevice(String userAgent) {
        if (userAgent == null) {
            return false;
        }
        String lowerUserAgent = userAgent.toLowerCase();
        return lowerUserAgent.contains("mobile") ||
            lowerUserAgent.contains("android") ||
            lowerUserAgent.contains("iphone") ||
            lowerUserAgent.contains("ipad");
    }

    /**
     * 判断是否为常用浏览器
     *
     * @param userAgent User-Agent字符串
     * @return 浏览器名称，unknown-未知
     */
    public String getBrowserName(String userAgent) {
        if (userAgent == null) {
            return "unknown";
        }
        String lower = userAgent.toLowerCase();
        if (lower.contains("chrome")) {
            return "Chrome";
        }
        if (lower.contains("firefox")) {
            return "Firefox";
        }
        if (lower.contains("safari") && !lower.contains("chrome")) {
            return "Safari";
        }
        if (lower.contains("edge") || lower.contains("edg")) {
            return "Edge";
        }
        return "unknown";
    }

    /**
     * 生成简化设备描述
     *
     * @param request HTTP请求
     * @return 设备描述（如：Chrome on Windows）
     */
    public String getDeviceDescription(HttpServletRequest request) {
        String userAgent = getUserAgent(request);
        if (userAgent == null) {
            return "Unknown Device";
        }

        String browser = getBrowserName(userAgent);
        String os = getOperatingSystem(userAgent);

        return "%s on %s".formatted(browser, os);
    }

    /**
     * 获取操作系统
     *
     * @param userAgent User-Agent字符串
     * @return 操作系统名称
     */
    private String getOperatingSystem(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }
        String lower = userAgent.toLowerCase();
        if (lower.contains("windows")) {
            return "Windows";
        }
        if (lower.contains("mac")) {
            return "macOS";
        }
        if (lower.contains("linux")) {
            return "Linux";
        }
        if (lower.contains("android")) {
            return "Android";
        }
        if (lower.contains("iphone") || lower.contains("ipad")) {
            return "iOS";
        }
        return "Unknown";
    }
}
