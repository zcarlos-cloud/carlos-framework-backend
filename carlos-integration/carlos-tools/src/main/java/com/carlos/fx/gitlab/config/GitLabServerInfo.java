package com.carlos.fx.gitlab.config;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * gitlab server info
 * </p>
 *
 * @author Carlos
 * @date 2024/4/29 11:31
 */
@Accessors(chain = true)
@Data
public class GitLabServerInfo {

    /**
     * 名称
     */
    private String serverName;
    /**
     * 地址
     */
    private String serverHost;
    /**
     * 令牌
     */
    private String serverKey;
}
