package com.carlos.datacenter.config;


import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据平台对接配置
 */
@Data
@Slf4j
@ConfigurationProperties(prefix = "carlos.integration.datacenter")
public class DatacenterProperties implements InitializingBean {


    /** 数据平台实例配置 */
    private Map<String, InstanceConfig> instances;

    /**
     * 数据平台实例配置
     */
    @Data
    public static class InstanceConfig {

        /**
         * 实例标识（配置键本身即为 instanceId）
         */
        private String instanceId;

        /**
         * 供应商类型
         */
        private String supplier;

        /**
         * 其他扩展属性
         */
        private Map<String, Object> properties = new HashMap<>();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.debug("datacenter config:{}", JSONUtil.toJsonPrettyStr(this));
    }
}
