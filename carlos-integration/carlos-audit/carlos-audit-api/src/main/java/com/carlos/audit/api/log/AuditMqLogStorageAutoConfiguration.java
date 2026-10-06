package com.carlos.audit.api.log;

import com.carlos.audit.api.constant.AuditMqTopics;
import com.carlos.log.storage.LogStorage;
import com.carlos.mq.config.MqAutoConfiguration;
import com.carlos.mq.core.MqTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 跨服务操作日志桥接的自动配置
 *
 * <h2>生效条件</h2>
 * <ul>
 *   <li>类路径上同时有 {@code starter-log}（{@link LogStorage}）与 {@code starter-mq}
 *       （{@link MqTemplate}）——当业务服务同时依赖 {@code carlos-audit-api} 与这两个 starter 时自动生效；</li>
 *   <li>容器内<b>还没有</b>任何 {@link LogStorage}。这一条是为了让 carlos-audit 自己不受影响：
 *       {@code AuditLogStorage} 是 {@code @Component}，组件扫描发生在自动配置之前，
 *       因此 carlos-audit 的 JVM 里本配置会直接退让，继续走原有的直写 ClickHouse 路径。</li>
 * </ul>
 *
 * <h2>为什么必须 {@code before = LogConfig}</h2>
 * <p>
 * {@code com.carlos.log.config.LogConfig#logStorage()} 同样声明了
 * {@code @ConditionalOnMissingBean(LogStorage.class)}，即「谁先注册谁生效、另一个退让」。
 * 两个都是自动配置，若 {@code LogConfig} 被先处理，它会注册兜底的 {@code LoggingLogStorage}，
 * 本配置随即退让 —— 结果是<b>日志只出现在应用日志里、审计库一条都没有，而且没有任何报错</b>，
 * 正是这个功能要消除的那种静默失效。用 {@code before} 显式钉死顺序。
 * </p>
 * <p>
 * 用字符串形式（{@code beforeName}）而非类字面量：{@code starter-log} 是 optional 依赖，
 * 缺席时不应因本类而被加载。
 * </p>
 *
 * <h2>为什么 {@code after = MqAutoConfiguration}</h2>
 * <p>
 * {@code @ConditionalOnBean} 依赖自动配置的处理顺序：只有 MqAutoConfiguration 先跑完，
 * {@code MqTemplate} 的 BeanDefinition 才存在，本条件才能看到它。
 * </p>
 *
 * <h2>关闭开关</h2>
 * <p>
 * {@code carlos.audit.mq-log-storage.enabled=false} 可退回兜底存储器（日志只落应用日志）。
 * 用于审计链路整体故障时的应急止损，不需要回滚代码。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@AutoConfiguration(after = MqAutoConfiguration.class, beforeName = "com.carlos.log.config.LogConfig")
@ConditionalOnClass({LogStorage.class, MqTemplate.class})
@ConditionalOnProperty(prefix = "carlos.audit.mq-log-storage", name = "enabled",
    havingValue = "true", matchIfMissing = true)
public class AuditMqLogStorageAutoConfiguration {

    /**
     * 跨服务操作日志存储器
     */
    @Bean
    @ConditionalOnMissingBean(LogStorage.class)
    @ConditionalOnBean(MqTemplate.class)
    public LogStorage auditMqLogStorage(MqTemplate mqTemplate) {
        AuditMqLogStorage storage = new AuditMqLogStorage(mqTemplate);
        // 显式声明「日志会去哪」：本功能最危险的失效模式是静默退化（日志只落应用日志、
        // 审计库没有），启动期打一行名字，排障时一眼可辨
        log.info("操作日志存储器已装配：{}（主题 {}）", storage.getName(), AuditMqTopics.AUDIT_LOG);
        return storage;
    }
}
