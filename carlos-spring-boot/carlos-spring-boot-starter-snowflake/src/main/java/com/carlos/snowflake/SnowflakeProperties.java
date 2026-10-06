package com.carlos.snowflake;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.carlos.core.response.CommonErrorCode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * <p>
 * 雪花算法属性配置
 * </p>
 *
 * @author carlos
 * @date 2020/10/25 23:45
 */
@Slf4j
@Data
@ConfigurationProperties("carlos.snowflake")
public class SnowflakeProperties implements InitializingBean {

    /**
     * 当前服务标签
     */
    private String tag;

    /**
     * 缓存空间
     */
    private String namespace = SnowflakeConstant.NAMESPACE;

    /**
     * 缓存过期时间 默认24小时
     */
    private Duration redisExpire = Duration.ofHours(24);

    /**
     * 是否使用{@link SystemClock} 获取当前时间戳
     */
    private boolean useSystemClock = false;

    /**
     * 工作节点ID（0-31），本地模式下使用
     * 若不配置，则基于IP地址自动生成
     */
    private Long workerId;

    /**
     * 数据中心ID（0-31），本地模式下使用
     * 若不配置，则基于IP地址自动生成
     */
    private Long dataCenterId;

    @Override
    public void afterPropertiesSet() {
        if (tag == null) {
            tag = SpringUtil.getProperty("spring.application.name");
            if (StrUtil.isBlank(tag)) {
                log.error("snowflake tag is null, please check your application.yml, set 'spring.application.name' or 'carlos.snowflake.tag'");
                throw CommonErrorCode.CONFIG_LOAD_ERROR.exception("snowflake tag is null, please check your application.yml, set 'spring.application.name' or 'carlos.snowflake.tag'");
            }
        }
        // 校验 workerId 范围
        if (workerId != null && (workerId < 0 || workerId > SnowflakeConstant.MAX_WORKER_ID)) {
            throw CommonErrorCode.PARAM_OUT_OF_RANGE.exception("carlos.snowflake.workerId must be between 0 and " + SnowflakeConstant.MAX_WORKER_ID);
        }
        // 校验 dataCenterId 范围
        if (dataCenterId != null && (dataCenterId < 0 || dataCenterId > SnowflakeConstant.MAX_DATA_CENTER_ID)) {
            throw CommonErrorCode.PARAM_OUT_OF_RANGE.exception("carlos.snowflake.dataCenterId must be between 0 and " + SnowflakeConstant.MAX_DATA_CENTER_ID);
        }
        log.debug("Snowflake properties:{}", this);
    }
}
