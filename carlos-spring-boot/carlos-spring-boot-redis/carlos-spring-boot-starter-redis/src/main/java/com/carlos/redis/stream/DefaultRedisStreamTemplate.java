package com.carlos.redis.stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link RedisStreamTemplate} 的默认实现，基于 {@link StringRedisTemplate} 的 Stream 操作。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
public class DefaultRedisStreamTemplate implements RedisStreamTemplate {

    /** Redis 在消费组已存在时返回的错误标识 */
    private static final String BUSYGROUP = "BUSYGROUP";

    private final StringRedisTemplate redisTemplate;
    private final RedisStreamProperties properties;

    /** 仅在「未配置长度上限」时告警一次，避免刷日志 */
    private final AtomicBoolean unlimitedWarned = new AtomicBoolean(false);

    public DefaultRedisStreamTemplate(StringRedisTemplate redisTemplate, RedisStreamProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /* ==================== 写 ==================== */

    /**
     * {@inheritDoc}
     * <p>
     * <b>长度上限是本实现的默认行为</b>：优先取 {@code options.maxLength}，
     * 其次取全局配置；两者都为空才不做修剪（并告警一次）。
     * </p>
     */
    @Override
    public String add(String key, Map<String, String> fields, StreamAddOptions options) {
        Long maxLength = options == null ? null : options.maxLength();
        boolean approximate = options == null || options.approximateTrim();
        if (maxLength == null) {
            maxLength = properties.getMaxLength();
        }

        RedisStreamCommands.XAddOptions addOptions;
        if (maxLength == null || maxLength <= 0) {
            if (unlimitedWarned.compareAndSet(false, true)) {
                log.warn("Redis Stream 未配置长度上限（carlos.redis.stream.max-length），"
                    + "在共用 Redis 实例场景下可能导致内存无界增长，建议显式设置");
            }
            addOptions = RedisStreamCommands.XAddOptions.none();
        } else {
            addOptions = RedisStreamCommands.XAddOptions.maxlen(maxLength)
                .approximateTrimming(approximate);
        }

        try {
            RecordId id = streamOps().add(key, fields, addOptions);
            return id == null ? null : id.getValue();
        } catch (Exception e) {
            throw new RedisStreamException(RedisStreamErrorCode.REDIS_STREAM_UNAVAILABLE,
                "写入 Stream 失败：key=" + key, e);
        }
    }

    @Override
    public long trim(String key, long maxLength, boolean approximate) {
        try {
            Long removed = streamOps().trim(key, maxLength, approximate);
            return removed == null ? 0L : removed;
        } catch (Exception e) {
            throw new RedisStreamException(RedisStreamErrorCode.STREAM_TRIM_FAILED,
                "修剪 Stream 失败：key=" + key, e);
        }
    }

    /* ==================== 消费组 ==================== */

    @Override
    public boolean createGroupIfAbsent(String key, String group, String startOffset) {
        String offset = startOffset == null || startOffset.isBlank()
            ? properties.getGroupStartOffset() : startOffset;
        try {
            // 用 XGROUP CREATE ... MKSTREAM 一步完成「流不存在则一并创建」，key 已存在时
            // MKSTREAM 被忽略，故两个分支合一。
            //
            // ⚠️ 不要改回「写占位条目 → 建组 → 删掉占位条目」的引导方式：占位条目是流里
            // 唯一一条，删掉它会让 Redis 移除整个 stream key，**消费组随之消失**，而本方法
            // 仍返回 true。此后每一次 XREADGROUP 都恒报 NOGROUP，消费链路静默失效——
            // 2026-09-26 已在真机实测确认（createGroupIfAbsent 返回 true，XLEN=0，
            // 紧接着 XPENDING 即报组不存在）。
            redisTemplate.execute((RedisCallback<String>) connection ->
                connection.streamCommands().xGroupCreate(
                    key.getBytes(StandardCharsets.UTF_8), group, ReadOffset.from(offset), true));
            return true;
        } catch (Exception e) {
            if (isGroupAlreadyExists(e)) {
                log.debug("消费组已存在，跳过创建：key={}, group={}", key, group);
                return false;
            }
            throw new RedisStreamException(RedisStreamErrorCode.STREAM_GROUP_OPERATION_FAILED,
                "创建消费组失败：key=%s, group=%s".formatted(key, group), e);
        }
    }

    @Override
    public List<StreamEntry> readGroup(String key, String group, String consumer, StreamReadOptions options) {
        StreamReadOptions readOptions = options == null
            ? StreamReadOptions.of(properties.getClaimBatchSize(), properties.getBlockTimeout())
            : options;
        try {
            // 注：此处用全限定名是项目规范允许的「同名类冲突」例外
            //（本包已有自己的 StreamReadOptions，故不能再 import Spring 的同名类）
            org.springframework.data.redis.connection.stream.StreamReadOptions springOptions =
                org.springframework.data.redis.connection.stream.StreamReadOptions.empty()
                    .count(readOptions.count());
            if (readOptions.blockTimeout() != null) {
                springOptions = springOptions.block(readOptions.blockTimeout());
            }

            List<MapRecord<String, String, String>> records = streamOps().read(
                Consumer.from(group, consumer),
                springOptions,
                StreamOffset.create(key, ReadOffset.lastConsumed()));
            return toEntries(records);
        } catch (Exception e) {
            throw new RedisStreamException(RedisStreamErrorCode.REDIS_STREAM_UNAVAILABLE,
                "读取 Stream 失败：key=%s, group=%s".formatted(key, group), e);
        }
    }

    @Override
    public long ack(String key, String group, List<String> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return 0L;
        }
        try {
            Long acked = streamOps().acknowledge(key, group, recordIds.toArray(String[]::new));
            return acked == null ? 0L : acked;
        } catch (Exception e) {
            throw new RedisStreamException(RedisStreamErrorCode.REDIS_STREAM_UNAVAILABLE,
                "确认 Stream 条目失败：key=%s, group=%s".formatted(key, group), e);
        }
    }

    /* ==================== 待处理条目（PEL） ==================== */

    @Override
    public StreamPendingSummary pendingSummary(String key, String group) {
        try {
            PendingMessagesSummary summary = streamOps().pending(key, group);
            if (summary == null) {
                return new StreamPendingSummary(group, 0L, null, null, Map.of());
            }
            return new StreamPendingSummary(
                summary.getGroupName(),
                summary.getTotalPendingMessages(),
                summary.minMessageId(),
                summary.maxMessageId(),
                summary.getPendingMessagesPerConsumer());
        } catch (NoSuchElementException e) {
            return emptyPendingSummary(key, group);
        } catch (Exception e) {
            throw new RedisStreamException(RedisStreamErrorCode.REDIS_STREAM_UNAVAILABLE,
                "查询待处理汇总失败：key=%s, group=%s".formatted(key, group), e);
        }
    }

    @Override
    public List<StreamPendingEntry> pending(String key, String group, Duration minIdle, int count) {
        int bounded = count <= 0 ? properties.getClaimBatchSize() : count;
        Duration idle = minIdle == null ? Duration.ZERO : minIdle;
        try {
            PendingMessages messages = idle.isZero()
                ? streamOps().pending(key, group, Range.unbounded(), (long) bounded)
                : streamOps().pending(key, group, Range.unbounded(), (long) bounded, idle);
            if (messages == null || messages.isEmpty()) {
                return List.of();
            }
            return messages.stream()
                .map(this::toPendingEntry)
                .toList();
        } catch (Exception e) {
            throw new RedisStreamException(RedisStreamErrorCode.REDIS_STREAM_UNAVAILABLE,
                "查询待处理明细失败：key=%s, group=%s".formatted(key, group), e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 刻意<b>不</b>传 {@code retryCount}：Redis 在未显式指定时会自行递增投递次数，
     * 这正是死信判定所依赖的语义。
     * </p>
     */
    @Override
    public List<StreamEntry> claim(String key, String group, String consumer,
                                   Duration minIdle, List<String> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return List.of();
        }
        Duration idle = minIdle == null ? Duration.ZERO : minIdle;
        try {
            RecordId[] ids = recordIds.stream().map(RecordId::of).toArray(RecordId[]::new);
            List<MapRecord<String, String, String>> records =
                streamOps().claim(key, group, consumer, idle, ids);
            return toEntries(records);
        } catch (Exception e) {
            throw new RedisStreamException(RedisStreamErrorCode.REDIS_STREAM_UNAVAILABLE,
                "认领待处理条目失败：key=%s, group=%s".formatted(key, group), e);
        }
    }

    /* ==================== 观测 ==================== */

    @Override
    public long length(String key) {
        try {
            Long size = streamOps().size(key);
            return size == null ? 0L : size;
        } catch (Exception e) {
            throw new RedisStreamException(RedisStreamErrorCode.REDIS_STREAM_UNAVAILABLE,
                "查询 Stream 长度失败：key=" + key, e);
        }
    }

    @Override
    public List<StreamEntry> range(String key, int count) {
        int bounded = count <= 0 ? properties.getClaimBatchSize() : count;
        try {
            List<MapRecord<String, String, String>> records =
                streamOps().range(key, Range.unbounded(), Limit.limit().count(bounded));
            return toEntries(records);
        } catch (Exception e) {
            throw new RedisStreamException(RedisStreamErrorCode.REDIS_STREAM_UNAVAILABLE,
                "读取 Stream 区间失败：key=" + key, e);
        }
    }

    @Override
    public long delete(String key, List<String> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return 0L;
        }
        try {
            RecordId[] ids = recordIds.stream().map(RecordId::of).toArray(RecordId[]::new);
            Long removed = streamOps().delete(key, ids);
            return removed == null ? 0L : removed;
        } catch (Exception e) {
            throw new RedisStreamException(RedisStreamErrorCode.REDIS_STREAM_UNAVAILABLE,
                "删除 Stream 条目失败：key=" + key, e);
        }
    }

    @Override
    public long usedMemoryBytes() {
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        if (factory == null) {
            return -1L;
        }
        try (RedisConnection connection = factory.getConnection()) {
            Properties info = connection.serverCommands().info("memory");
            if (info == null) {
                return -1L;
            }
            String raw = info.getProperty("used_memory");
            return raw == null ? -1L : Long.parseLong(raw.trim());
        } catch (Exception e) {
            log.warn("读取 Redis used_memory 失败（不影响主流程）", e);
            return -1L;
        }
    }

    /* ==================== 内部工具 ==================== */

    @SuppressWarnings("unchecked")
    private StreamOperations<String, String, String> streamOps() {
        return redisTemplate.opsForStream();
    }

    private List<StreamEntry> toEntries(List<MapRecord<String, String, String>> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        return records.stream()
            .map(record -> StreamEntry.of(record.getId().getValue(), record.getValue()))
            .toList();
    }

    private StreamPendingEntry toPendingEntry(PendingMessage message) {
        return new StreamPendingEntry(
            message.getIdAsString(),
            message.getConsumerName(),
            message.getElapsedTimeSinceLastDelivery(),
            message.getTotalDeliveryCount());
    }

    /**
     * 「待处理队列为空」的语义结果
     * <p>
     * <b>为什么需要它</b>：本环境的连接工厂是 {@code RedissonConnectionFactory}，其
     * {@code XPENDING} <b>汇总形式</b>在<b>待处理队列为空</b>时会对空 {@code Optional} 取值而抛
     * {@code NoSuchElementException: No value present}（真机实测）。而「PEL 为空」是<b>常态</b>，
     * 把它报成 {@code REDIS_STREAM_UNAVAILABLE}（503）属文案误导——真实情况是「没有待处理条目」。
     * </p>
     * <p>
     * 注意：<b>明细形式</b>（{@link #pending}）在同样的空 PEL 场景下实测<b>正常</b>，
     * 故未做同等处理——不给未经证实的失败模式加兜底。
     * </p>
     * <p>
     * 语义上「查不到待处理条目」就是「没有待处理条目」，故按空结果返回。
     * 其他异常仍照常上抛——不把真正的故障一并吞掉。
     * </p>
     */
    private StreamPendingSummary emptyPendingSummary(String key, String group) {
        log.debug("待处理队列为空：key={}, group={}", key, group);
        return new StreamPendingSummary(group, 0L, null, null, Map.of());
    }

    /**
     * 判断异常链上是否出现「消费组已存在」
     * <p>
     * 必须走完整条 cause 链：底层命令经 {@code RedisCallback} 执行时会被逐层包装
     * （{@code RedisSystemException} 包着 {@code RedisCommandExecutionException}），
     * 只看最外层 message 会漏判，导致「组已存在」被当成真正的失败抛出去。
     * </p>
     */
    private boolean isGroupAlreadyExists(Exception e) {
        for (Throwable current = e; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message != null && message.contains(BUSYGROUP)) {
                return true;
            }
            if (current.getCause() == current) {
                // 理论不会发生，但避免自引用导致死循环
                break;
            }
        }
        return false;
    }
}
