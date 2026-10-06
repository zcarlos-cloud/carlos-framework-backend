package com.carlos.org.config;

import lombok.Data;

/**
 * <p>
 * 组织模块初始化配置
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
public class OrgInitProperties {

    /**
     * 是否启用初始化接口，默认关闭。初始化完成后应置为 false。
     */
    private boolean enabled = true;

    /**
     * 初始化接口校验 Token，必须配置后接口才可用。
     * 生产环境建议通过环境变量注入，且长度不少于 32 字符。
     */
    private String token = "nT1nD5aH6eO9jR1eE2nS4vS5wF8uB7jZ";

}
