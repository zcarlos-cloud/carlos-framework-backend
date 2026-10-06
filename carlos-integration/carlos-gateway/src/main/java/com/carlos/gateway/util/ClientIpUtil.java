package com.carlos.gateway.util;

import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * <p>
 * 客户端 IP 获取工具类
 * </p>
 * <p>
 * 在网关部署于反向代理（Nginx、K8s Ingress、SLB）后的场景下，
 * {@code request.getRemoteAddress()} 返回的是代理服务器 IP 而非真实客户端 IP。
 * 本工具优先读取可信的代理头获取真实客户端 IP。
 * </p>
 * <p>
 * 支持的代理头（按优先级排序）：
 * <ol>
 *   <li>X-Forwarded-For（可能包含多个 IP，取第一个最原始的客户端）</li>
 *   <li>X-Real-IP</li>
 *   <li>Proxy-Client-IP</li>
 *   <li>WL-Proxy-Client-IP</li>
 * </ol>
 * </p>
 *
 * @author carlos
 * @date 2026/6/6
 */
public final class ClientIpUtil {

    private ClientIpUtil() {
        // 工具类禁止实例化
    }

    /**
     * 代理头列表（按优先级）
     */
    private static final List<String> PROXY_HEADERS = List.of(
        "X-Forwarded-For",
        "X-Real-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED"
    );

    /**
     * 未知 IP 标记
     */
    private static final String UNKNOWN = "unknown";

    /**
     * 获取客户端真实 IP 地址
     *
     * @param request ServerHttpRequest
     * @return 客户端 IP，无法获取时返回 "unknown"
     */
    public static String getClientIp(ServerHttpRequest request) {
        // 1. 优先从代理头获取
        for (String headerName : PROXY_HEADERS) {
            String headerValue = request.getHeaders().getFirst(headerName);
            if (headerValue != null && !headerValue.isBlank() && !UNKNOWN.equalsIgnoreCase(headerValue)) {
                // X-Forwarded-For 可能包含多个 IP: "client, proxy1, proxy2"
                // 取第一个（最原始的客户端 IP）
                String firstIp = headerValue.split(",")[0].trim();
                if (isValidIp(firstIp)) {
                    return firstIp;
                }
            }
        }

        // 2. 从远程地址获取
        if (request.getRemoteAddress() != null
            && request.getRemoteAddress().getAddress() != null) {
            String remoteIp = request.getRemoteAddress().getAddress().getHostAddress();
            if (remoteIp != null && !remoteIp.isBlank()) {
                return remoteIp;
            }
        }

        return UNKNOWN;
    }

    /**
     * 验证 IP 地址是否有效
     * <p>
     * 排除内网 IP 和无效格式，返回 false 时回退到下一个候选
     *
     * @param ip IP 地址
     * @return true 如果格式有效且不是 "unknown"
     */
    private static boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank() || UNKNOWN.equalsIgnoreCase(ip)) {
            return false;
        }
        // 基本格式校验：包含至少一个点或冒号（IPv6）
        return ip.contains(".") || ip.contains(":");
    }

}
