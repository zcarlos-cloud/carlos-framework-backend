package com.carlos.audit.config;

import com.carlos.audit.disruptor.AuditLogEventHandler;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.disruptor.core.DisruptorManager;
import com.carlos.disruptor.core.DisruptorTemplate;
import com.carlos.disruptor.core.DisruptorWaitStrategyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>
 * Disruptor 配置类（基于通用 Disruptor 框架）
 * </p>
 *
 * @author Carlos
 * @date 2026-03-06
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "carlos.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditDisruptorConfig {

    private final AuditProperties auditProperties;
    private final AuditLogEventHandler eventHandler;

    /**
     * 审计日志 Disruptor 模板
     */
    @Bean("auditDisruptorTemplate")
    @ConditionalOnMissingBean(name = "auditDisruptorTemplate")
    public DisruptorTemplate<AuditLogMainDTO> auditDisruptorTemplate(DisruptorManager disruptorManager) {
        AuditProperties.Disruptor disruptorProps = auditProperties.getDisruptor();

        int bufferSize = disruptorProps.getBufferSize();
        DisruptorWaitStrategyType waitStrategy = disruptorProps.getWaitStrategy();
        String threadNamePrefix = disruptorProps.getName();

        log.info("初始化审计日志 Disruptor， name:{} bufferSize={}, waitStrategy={}", threadNamePrefix, bufferSize, waitStrategy);

        return disruptorManager.create(threadNamePrefix,
            bufferSize, waitStrategy.toStrategy(),
            eventHandler
        );
    }


}
