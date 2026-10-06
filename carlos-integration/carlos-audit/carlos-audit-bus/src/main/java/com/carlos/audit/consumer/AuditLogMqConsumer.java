package com.carlos.audit.consumer;

import com.carlos.audit.api.constant.AuditMqTopics;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import com.carlos.audit.convert.AuditLogMainConvert;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.audit.service.AuditLogMainService;
import com.carlos.mq.annotation.MqListener;
import com.carlos.mq.core.ConsumeResult;
import com.carlos.mq.core.MqMessage;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 审计日志 MQ 消费端
 *
 * <h2>与 HTTP 入口共用同一条写入路径</h2>
 * <p>
 * 本消费端复用既有的 {@link AuditLogMainService#addAuditLogMain(AuditLogMainDTO)}
 * （经 {@code AuditLogMainConvert} 转换），与 Feign 入口
 * {@code ApiAuditLogMainImpl.saveAuditLog} <b>走同一段 service 代码</b>。
 * 行为一致性因此由「复用」保证，而不是靠两处各自维护。
 * 下游链路（Disruptor → {@code ClickHouseBatchWriter}）完全不变。
 * </p>
 *
 * <h2>幂等：用信封的 messageId，不用业务字段</h2>
 * <p>
 * {@code ApiAuditLogMainParam} <b>没有任何请求级唯一标识</b>，而它的 {@code id}
 * 是<b>消费侧</b>用雪花生成的（{@code ClickHouseBatchWriter} 兜底），
 * 因此 {@code id} 天然不能作为跨端幂等键。
 * </p>
 * <p>
 * 这里改用 MQ 信封自带的 {@code messageId}：它由发送端生成，且<b>重投时不变</b>
 * （同一条 Stream 记录被认领重投时仍是同一个 messageId）。这恰好覆盖真实的重复场景
 * ——「确认前崩溃 → 之后被认领重投」，同时<b>零业务字段改动</b>。
 * </p>
 * <p>
 * <b>诚实边界</b>：本地去重窗挡不住「跨实例的重复」与「实例重启后窗丢失」。
 * 即审计日志<b>允许极小概率重复</b>——对审计用途而言，多一条不影响正确性。
 * 若将来需要严格去重，需在生产端生成稳定 id 并在 ClickHouse 侧用
 * {@code ReplacingMergeTree}，不在本次范围。
 * </p>
 *
 * <h2>三项显式声明的丢失边界</h2>
 * <ol>
 *   <li>{@code XADD MAXLEN ~ N} 修剪掉尚未消费的条目——这是把「无界积压」变成
 *       「固定预算」的必然代价（共用 Redis 实例下的核心保护项）；</li>
 *   <li>背压策略为拒绝时，超阈值不投递；</li>
 *   <li>Redis 崩溃且未开 AOF 时丢失最近写入。</li>
 * </ol>
 * <p>三者对登录审计均可接受——丢失一条审计不影响认证与业务。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "carlos.audit.mq", name = "enabled",
    havingValue = "true", matchIfMissing = true)
public class AuditLogMqConsumer {

    /** 去重窗容量 */
    private static final int DEDUP_MAX_SIZE = 100_000;

    /** 去重窗存活时长：需覆盖「确认前崩溃 → 认领空闲阈值（默认 60 秒）」的典型重投间隔 */
    private static final Duration DEDUP_TTL = Duration.ofMinutes(10);

    private final AuditLogMainService logMainService;

    /**
     * 本地去重窗（Guava Cache；carlos-audit-bus 已直接依赖 guava，零新增依赖）
     * <p>
     * 刻意不用 Redis {@code SETNX}：那会把「消费去重」变成新的 Redis 压力源，
     * 而本模块与缓存共用同一实例，正是要尽力减少的额外往返。
     * </p>
     */
    private final Cache<String, Boolean> dedupWindow = CacheBuilder.newBuilder()
        .maximumSize(DEDUP_MAX_SIZE)
        .expireAfterWrite(DEDUP_TTL)
        .build();

    /**
     * 消费审计日志
     * <p>
     * 入参使用「负载 + 信封」双参形状：既拿到业务参数，也拿到信封中的 {@code messageId}
     * 与 {@code deliveryCount}（后者可用于观测重投频率）。
     * </p>
     *
     * @param param    审计日志参数
     * @param envelope 消息信封
     * @return 消费结果
     */
    @MqListener(topic = AuditMqTopics.AUDIT_LOG, group = AuditMqTopics.AUDIT_LOG_GROUP, concurrency = 2)
    public ConsumeResult onAuditLog(ApiAuditLogMainParam param, MqMessage envelope) {
        if (isDuplicate(envelope)) {
            // 重复投递是正常路径（至少一次语义），已处理过即视为完成：
            // 返回 success 而不是 drop —— drop 会被容器写入死信，把重复噪声灌进死信队列
            log.debug("审计日志重复投递，已忽略：messageId={}, logType={}",
                envelope.messageId(), param == null ? null : param.getLogType());
            return ConsumeResult.success(envelope.messageId(), envelope.topic());
        }
        if (param == null) {
            // 负载缺失属于无法修复的毒消息：放弃并落死信，避免反复重投阻塞后续
            log.error("审计日志负载为空，转入放弃分支：messageId={}", envelope.messageId());
            return ConsumeResult.drop(envelope.messageId(), envelope.topic(), "负载为空");
        }

        AuditLogMainDTO dto = AuditLogMainConvert.INSTANCE.toDTO(param);
        logMainService.addAuditLogMain(dto);
        if (envelope.isRedelivery()) {
            log.warn("审计日志为重投递（第 {} 次）：messageId={}, logType={}",
                envelope.deliveryCount(), envelope.messageId(), param.getLogType());
        }
        return ConsumeResult.success(envelope.messageId(), envelope.topic());
    }

    /**
     * 是否重复投递
     *
     * @param envelope 信封
     * @return 已处理过返回 true
     */
    private boolean isDuplicate(MqMessage envelope) {
        String messageId = envelope.messageId();
        if (messageId == null) {
            // 无 messageId 无法去重：保守选择是继续处理（审计多一条优于少一条）
            return false;
        }
        return dedupWindow.asMap().putIfAbsent(messageId, Boolean.TRUE) != null;
    }
}
