package com.carlos.boot.web.request;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * 获取IP地址工具类
 *
 * @author carlos
 * @date 2018-11-08
 */
@Slf4j
public final class IpUtils {

    private static final String UNKNOWN = "unknown";
    private static final String IPV6_LOCAL = "0:0:0:0:0:0:0:1";

    private IpUtils() {
        throw new AssertionError();
    }

    /**
     * 获取当前请求的ip地址
     *
     * @param request 当前请求
     * @return ip 地址
     * @author carlos
     * @date 2020/4/14 11:55
     */
    public static String getRequestIp(HttpServletRequest request) {

        if (request == null) {
            return "unknown";
        }

        String ip = null;
        // X-Forwarded-For：Squid 服务代理
        String ipAddresses = request.getHeader("X-Forwarded-For");
        if (StringUtils.isEmpty(ipAddresses) || UNKNOWN.equalsIgnoreCase(ipAddresses)) {
            // Proxy-Client-IP：apache 服务代理
            ipAddresses = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isEmpty(ipAddresses) || UNKNOWN.equalsIgnoreCase(ipAddresses)) {
            // WL-Proxy-Client-IP：weblogic 服务代理
            ipAddresses = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isEmpty(ipAddresses) || UNKNOWN.equalsIgnoreCase(ipAddresses)) {
            // HTTP_CLIENT_IP：有些代理服务器
            ipAddresses = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StringUtils.isEmpty(ipAddresses) || UNKNOWN.equalsIgnoreCase(ipAddresses)) {
            // X-Real-IP：nginx服务代理
            ipAddresses = request.getHeader("X-Real-IP");
        }

        // 有些网络通过多层代理，那么获取到的ip就会有多个，一般都是通过逗号（,）分割开来，并且第一个ip为客户端的真实IP
        if (ipAddresses != null && ipAddresses.length() != 0) {
            ip = ipAddresses.split(",")[0];
        }

        // 还是不能获取到，最后再通过request.getRemoteAddr();获取
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ipAddresses)) {
            ip = request.getRemoteAddr();
        }
        return IPV6_LOCAL.equals(ip) ? "127.0.0.1" : ip;
    }

    /**
     * 获取本机地址
     *
     * @return 本机 IP
     * @author carlos
     * @date 2020/4/14 11:58
     */
    public static String getLocalhostIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("本机ip地址获取失败！", e);
        }
        return null;
    }

    /**
     * 获取本机ip地址，优先获取非回环、非私有的 IPv4 地址
     *
     * @return 本机ip地址
     * @author carlos
     * @date 2020-2-18 10:11:05
     */
    public static String getLocalAddress() {
        try {
            // Traversal Network interface to get the first non-loopback and non-private address
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            // ipv4 地址集
            ArrayList<String> ipv4Result = new ArrayList<>();
            // ipv6 地址集
            ArrayList<String> ipv6Result = new ArrayList<>();

            // 遍历网卡
            while (enumeration.hasMoreElements()) {
                NetworkInterface networkInterface = enumeration.nextElement();
                Enumeration<InetAddress> en = networkInterface.getInetAddresses();
                while (en.hasMoreElements()) {
                    InetAddress address = en.nextElement();
                    if (!address.isLoopbackAddress()) {
                        if (address instanceof Inet6Address) {
                            ipv6Result.add(normalizeHostAddress(address));
                        } else {
                            ipv4Result.add(normalizeHostAddress(address));
                        }
                    }
                }
            }

            // prefer ipv4
            if (!ipv4Result.isEmpty()) {
                for (String ip : ipv4Result) {
                    if (ip.startsWith("127.0") || ip.startsWith("192.168")) {
                        continue;
                    }
                    return ip;
                }
                // 存在多个ip的时候，获取最后一个
                return ipv4Result.getLast();
            } else if (!ipv6Result.isEmpty()) {
                return ipv6Result.getFirst();
            }
            // If failed to find,fall back to localhost
            InetAddress localHost = InetAddress.getLocalHost();
            return normalizeHostAddress(localHost);
        } catch (Exception e) {
            log.error("本机ip地址获取失败！", e);
        }
        return null;
    }

    /**
     * 获取服务器名称
     *
     * @return 主机名
     */
    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "未知";
        }
    }

    /**
     * 规范化主机地址，IPv6 地址使用方括号包裹
     *
     * @param localHost InetAddress
     * @return 规范化后的地址
     */
    private static String normalizeHostAddress(InetAddress localHost) {
        if (localHost instanceof Inet6Address) {
            return "[" + localHost.getHostAddress() + "]";
        } else {
            return localHost.getHostAddress();
        }
    }

}
