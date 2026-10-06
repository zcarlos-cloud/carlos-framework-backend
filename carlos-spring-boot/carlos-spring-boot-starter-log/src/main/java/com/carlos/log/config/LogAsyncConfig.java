package com.carlos.log.config;

import cn.hutool.core.util.StrUtil;
import com.carlos.disruptor.core.DisruptorManager;
import com.carlos.disruptor.core.DisruptorTemplate;
import com.carlos.log.disruptor.LogDisruptorHandler;
import com.carlos.log.entity.OperationLog;
import com.carlos.log.storage.LogStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 异步日志自动配置（基于通用 Disruptor）
 * <p>
 * 仅在 carlos.log.async=true 时启用
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "carlos.log", name = "async", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(DisruptorManager.class)
public class LogAsyncConfig {

    /**
     * 日志 Disruptor 模板
     */
    @Bean("logDisruptorTemplate")
    @ConditionalOnMissingBean(name = "logDisruptorTemplate")
    public DisruptorTemplate<OperationLog> logDisruptorTemplate(
        DisruptorManager disruptorManager,
        LogProperties properties,
        LogDisruptorHandler handler) {

        LogProperties.Disruptor disruptorProps = properties.getDisruptor();

        log.info("初始化日志 Disruptor，bufferSize={}", disruptorProps.getRingBufferSize());

        return disruptorManager.create(
            StrUtil.blankToDefault(properties.getStorage().getDefaultStorage(), "log-disruptor"),
            disruptorProps.getRingBufferSize(),
            disruptorProps.getWaitStrategy().toStrategy(),
            handler
        );
    }

    @Bean
    public LogDisruptorHandler logDisruptorHandler(LogStorage logStorage) {
        return new LogDisruptorHandler(logStorage);
    }


}
