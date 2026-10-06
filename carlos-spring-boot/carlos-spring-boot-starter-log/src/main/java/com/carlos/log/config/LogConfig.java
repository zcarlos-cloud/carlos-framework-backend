package com.carlos.log.config;

import com.carlos.disruptor.core.DisruptorTemplate;
import com.carlos.log.aspect.LogAspect;
import com.carlos.log.entity.OperationLog;
import com.carlos.log.storage.LogStorage;
import com.carlos.log.storage.LoggingLogStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

/**
 * 日志模块自动配置
 * <p>
 * 核心配置：LogStorage、LogAspect
 * 异步配置：通过 {@link LogAsyncConfig} 导入
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@AutoConfiguration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableConfigurationProperties(LogProperties.class)
@ConditionalOnProperty(prefix = "carlos.log", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(LogAsyncConfig.class)
public class LogConfig {

    /**
     * 日志切面
     * 使用 ObjectProvider 使 DisruptorTemplate 变为可选依赖
     * 同步模式下 DisruptorTemplate 不存在，直接通过 LogStorage 存储
     */
    @Bean
    @ConditionalOnMissingBean(LogAspect.class)
    public LogAspect logAspect(ObjectProvider<DisruptorTemplate<OperationLog>> disruptorTemplateProvider,
                               LogStorage logStorage) {
        return new LogAspect(disruptorTemplateProvider, logStorage);
    }

    /**
     * 日志存储器
     * 如果应用没有自定义存储器，使用默认的 LoggingLogStorage
     */
    @Bean
    @ConditionalOnMissingBean(LogStorage.class)
    public LogStorage logStorage() {
        log.info("未检测到自定义 LogStorage，使用默认的 LoggingLogStorage");
        return new LoggingLogStorage();
    }
}
