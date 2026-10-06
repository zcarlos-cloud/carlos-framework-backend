package com.carlos.sms.config;

import cn.hutool.json.JSONUtil;
import com.carlos.sms.template.SmsTemplate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * <p>
 * 短信配置项
 * </p>
 *
 * @author carlos
 * @date 2021/6/10 13:21
 */
@Data
@Slf4j
@ConfigurationProperties(prefix = "carlos.sms")
public class SmsProperties implements InitializingBean {


    /**
     * 模板列表
     */
    private Map<String, SmsTemplate> templates;

    /**
     * 发送限流配置
     */
    private RateLimit rateLimit = new RateLimit();

    @Override
    public void afterPropertiesSet() {
        log.debug("sms properties:{}", JSONUtil.toJsonPrettyStr(this));
    }

    /**
     * 短信发送限流配置
     */
    @Data
    public static class RateLimit {

        /**
         * 是否启用限流
         */
        private boolean enabled = false;

        /**
         * 同一手机号每天最大发送次数
         */
        private int accountMax = 10;

        /**
         * 同一手机号每分钟最大发送次数
         */
        private int minuteMax = 1;
    }
}
