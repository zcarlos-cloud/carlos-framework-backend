package com.carlos.integration.module.dingtalk.config;

import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * <p>
 * 钉钉属性配
 * </p>
 *
 * @author Carlos
 * @date 2023/4/6 9:34
 */
@Slf4j
@Data
@ConfigurationProperties(prefix = "carlos.integration.dingtalk")
public class DingtalkProperties implements InitializingBean {


    /**
     * 是否开
     */
    private boolean enabled = false;
    /**
     * 钉钉服务地址
     */
    private String host;
    /**
     * 应用的唯一标识key
     */
    private String appkey;
    /**
     * 应用的密
     */
    private String appsecret;
    /**
     * agentId
     */
    private String agentId;
    /**
     * 企业id
     */
    private String corpId;
    /**
     * AccessToken过期时间 默认两小
     */
    private Duration tokenDuration = Duration.ofHours(2L);

    /**
     * token刷新时间
     */
    private String accessTokenRefreshCorn = "0 0/59 * * * *";

    @Override
    public void afterPropertiesSet() throws Exception {
        log.debug("Dingtalk config:{}", JSONUtil.toJsonPrettyStr(this));
    }
}
