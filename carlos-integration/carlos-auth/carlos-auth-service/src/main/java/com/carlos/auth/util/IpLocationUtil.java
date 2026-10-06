package com.carlos.auth.util;

import cn.hutool.core.util.StrUtil;
import com.carlos.auth.constant.AuthRedisKey;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * IP地址工具类
 * </p>
 *
 * <p>提供IP地址解析、地理位置查询等功能</p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpLocationUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取客户端真实IP
     *
     * <p>支持代理服务器、负载均衡器环境</p>
     *
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    public String getClientIp(HttpServletRequest request) {
        List<String> headerNames = Arrays.asList(
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        );

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // 对于通过多个代理的情况，第一个IP为客户端真实IP
                return ip.split(",")[0].trim();
            }
        }

        // 如果没有代理，直接使用远程IP
        return request.getRemoteAddr();
    }

    /**
     * 判断是否为内网IP
     *
     * @param ip IP地址
     * @return true-内网IP，false-公网IP
     */
    public boolean isInternalIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return true;
        }

        byte[] address = ipToBytes(ip);
        if (address == null) {
            return false;
        }

        // 10.0.0.0/8
        if (address[0] == 10) {
            return true;
        }
        // 172.16.0.0/12
        if (address[0] == (byte) 172 && address[1] >= 16 && address[1] <= 31) {
            return true;
        }
        // 192.168.0.0/16
        if (address[0] == (byte) 192 && address[1] == (byte) 168) {
            return true;
        }
        // 127.0.0.0/8
        if (address[0] == (byte) 127) {
            return true;
        }

        return false;
    }

    /**
     * 获取IP地理位置（简化版）
     *
     * <p>
     * 注意：生产环境建议使用IP2Region或纯真IP库获取更精确的位置信息
     * 这里实现简化版本，仅返回IP段
     * </p>
     *
     * @param ip IP地址
     * @return 地理位置信息（如：北京、上海、广州）
     */
    public String getLocation(String ip) {
        if (StrUtil.isBlank(ip) || isInternalIp(ip)) {
            return "内网IP";
        }

        // 生产环境：集成IP2Region库
        // String dbPath = "classpath:ip2region.xdb";
        // Ip2Region ip2Region = new Ip2Region(dbPath);
        // try {
        //     DataBlock dataBlock = ip2Region.memorySearch(ip);
        //     return dataBlock.getRegion();

        // 简化版：根据IP段粗略判断（仅演示）
        String firstOctet = ip.substring(0, ip.indexOf("."));
        int firstByte = Integer.parseInt(firstOctet);

        if (firstByte >= 1 && firstByte <= 50) {
            return "北美";
        } else if (firstByte >= 101 && firstByte <= 120) {
            return "亚太";
        } else if (firstByte >= 202 && firstByte <= 206) {
            return "中国";
        } else if (firstByte >= 211 && firstByte <= 218) {
            return "中国";
        } else if (firstByte >= 220 && firstByte <= 223) {
            return "中国";
        }

        return "未知";
    }

    /**
     * 检查IP是否为常用登录地点
     *
     * @param userId 用户ID
     * @param ip IP地址
     * @return true-常用地点，false-异地登录
     */
    public boolean isCommonLocation(Long userId, String ip) {
        if (userId == null || StrUtil.isBlank(ip)) {
            return false;
        }

        String location = getLocation(ip);
        String commonLocationsKey = AuthRedisKey.commonLocations(userId);

        // 从Redis获取用户的常用登录地点
        List<Object> commonLocations = redisTemplate.opsForList().range(commonLocationsKey, 0, -1);

        if (commonLocations == null || commonLocations.isEmpty()) {
            // 首次登录，记录为常用地点
            redisTemplate.opsForList().rightPush(commonLocationsKey, location);
            redisTemplate.expire(commonLocationsKey, java.time.Duration.ofDays(90));
            return true;
        }

        // 检查当前地点是否在常用地点列表中
        return commonLocations.stream()
            .map(Object::toString)
            .anyMatch(loc -> loc.equals(location));
    }

    /**
     * 判断两个IP是否在同一地区
     *
     * @param ip1 IP地址1
     * @param ip2 IP地址2
     * @return true-同一地区，false-不同地区
     */
    public boolean isSameRegion(String ip1, String ip2) {
        String loc1 = getLocation(ip1);
        String loc2 = getLocation(ip2);
        return loc1.equals(loc2);
    }

    /**
     * 将IP地址转换为字节数组
     *
     * @param ip IP地址
     * @return 字节数组
     */
    private byte[] ipToBytes(String ip) {
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return null;
            }

            byte[] bytes = new byte[4];
            for (int i = 0; i < 4; i++) {
                int value = Integer.parseInt(parts[i]);
                if (value < 0 || value > 255) {
                    return null;
                }
                bytes[i] = (byte) value;
            }
            return bytes;
        } catch (Exception e) {
            return null;
        }
    }
}
