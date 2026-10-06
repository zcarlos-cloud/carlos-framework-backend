package com.carlos.gateway.gray;

import lombok.Data;

import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 灰度策略
 * </p>
 *
 * @author carlos
 * @date 2026/3/16
 */
@Data
public class GrayStrategy {

    private boolean enabled = false;

    // 基于权重的灰度（0-100）
    private int weight = 0;

    // 基于用户的灰度
    private Set<String> userIds;

    // 基于 IP 的灰度
    private Set<String> ipRanges;

    // 基于请求头的灰度
    private Map<String, String> headers;

    // 基于版本的灰度
    private String version;

    // 哈希键类型
    private HashKeyType hashKey = HashKeyType.IP;

    // 自定义 Header 名称
    private String headerName;

    public enum HashKeyType {
        IP, USER, HEADER, RANDOM
    }
}
