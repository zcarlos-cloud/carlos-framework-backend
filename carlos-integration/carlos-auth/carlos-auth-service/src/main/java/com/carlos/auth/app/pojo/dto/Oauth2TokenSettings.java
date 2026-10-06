package com.carlos.auth.app.pojo.dto;


import lombok.Data;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;

import java.io.Serializable;

@Data
public class Oauth2TokenSettings implements Serializable {

    /**
     * 单位 分钟
     *
     * <p>无默认值：DB 未配置时必须保持 null，否则会被伪造成"已显式配置"，
     * 导致 CustomizeRegisteredClientRepository 的全局兜底（carlos.auth.login.*-ttl）永远不生效。</p>
     */
    private Long authorizationCodeTimeToLive;

    /** 单位 分钟，无默认值，未配置时由 carlos.auth.login.access-token-ttl 兜底 */
    private Long accessTokenTimeToLive;

    /** 默认 @see OAuth2TokenFormat.SELF_CONTAINED */
    private String accessTokenFormat = OAuth2TokenFormat.SELF_CONTAINED.getValue();

    /** 默认true */
    private Boolean reuseRefreshTokens = true;

    /** 单位 分钟，无默认值，未配置时由 carlos.auth.login.refresh-token-ttl 兜底 */
    private Long refreshTokenTimeToLive;

}
