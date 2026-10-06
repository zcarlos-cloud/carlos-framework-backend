package com.carlos.audit.config;

import com.carlos.disruptor.core.DisruptorWaitStrategyType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 审计日志配置属性
 * </p>
 *
 * @author Carlos
 * @date 2026-03-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "carlos.audit")
public class AuditProperties {

    /**
     * 是否启用审计日志
     */
    private boolean enabled = true;

    /**
     * Disruptor 配置
     */
    private Disruptor disruptor = new Disruptor();

    /**
     * ClickHouse 配置
     */
    private ClickHouse clickhouse = new ClickHouse();

    /**
     * 批量写入配置
     */
    private BatchWriter batchWriter = new BatchWriter();

    /**
     * 归档配置
     */
    private Archive archive = new Archive();

    /**
     * MQ 消费端配置
     */
    private Mq mq = new Mq();

    /**
     * MQ 消费端配置
     */
    @Data
    public static class Mq {

        /**
         * 是否启用审计日志的 MQ 消费端
         * <p>
         * <b>默认开启</b>：carlos-audit 此前已通过 starter-security / starter-snowflake
         * 传递获得 Redis 依赖与连接配置，因此启用消费端<b>不引入新的部署前提</b>。
         * </p>
         * <p>关闭后，生产端投递的审计消息将无人消费（会在 Stream 中堆积直至被长度上限修剪）。</p>
         */
        private boolean enabled = true;
    }

    @Data
    public static class Disruptor {
        /**
         * RingBuffer 大小（2的幂次）
         */
        private int bufferSize = 1 << 20; // 1,048,576

        /**
         * 消费者线程数
         */
        private int consumerCount = 4;

        /** 名称 */
        private String name = "audit-disruptor";

        /**
         * 等待策略：blocking, busy_spin, lite_blocking, sleeping, yielding
         */
        private DisruptorWaitStrategyType waitStrategy = DisruptorWaitStrategyType.BLOCKING;

        /**
         * 是否启用多生产者模式
         */
        private boolean multiProducer = true;
    }

    @Data
    public static class ClickHouse {
        /**
         * 是否启用 ClickHouse
         */
        private boolean enabled = true;

        /**
         * 主机地址
         */
        private String host = "localhost";

        /**
         * 端口
         */
        private int port = 8123;

        /**
         * 数据库名
         */
        private String database = "default";

        /**
         * 用户名
         */
        private String username = "default";

        /**
         * 密码
         */
        private String password = "";

        /**
         * Socket 超时时间（毫秒）
         */
        private int socketTimeout = 300000;

        /**
         * 连接超时时间（毫秒）
         */
        private int connectionTimeout = 10000;

        /**
         * 查询最大执行时间（秒）
         */
        private int maxExecutionTimeSeconds = 10;

        /**
         * 单次查询最大读取行数
         */
        private long maxRowsToRead = 10_000_000L;
    }

    @Data
    public static class BatchWriter {
        /**
         * 批次大小
         */
        private int batchSize = 500;

        /**
         * 刷新间隔（毫秒）
         */
        private int flushInterval = 1000;

        /**
         * 最大缓冲区大小
         */
        private int maxBufferSize = 10000;

        /**
         * 背压策略
         */
        private BackPressureStrategy backPressureStrategy = BackPressureStrategy.DROP_OLDEST;

        /**
         * 背压阻塞等待间隔（毫秒），仅 BLOCK 策略生效
         */
        private int backPressureBlockTimeoutMs = 10;

        /**
         * 背压采样率（0~1），仅 SAMPLE 策略生效
         */
        private double backPressureSampleRate = 0.1D;

        /**
         * 重试次数
         */
        private int retryTimes = 3;

        /**
         * 最大并发刷新任务数
         */
        private int maxConcurrentFlushes = 4;

        /**
         * 失败时是否写入本地文件备份
         */
        private boolean localBackupOnFailure = true;

        /**
         * 本地备份路径
         */
        private String localBackupPath = "./audit-log-backup";

        /**
         * 应用启动时是否自动恢复备份文件
         */
        private boolean recoveryOnStartup = false;

        /**
         * 是否启用自适应批量大小
         */
        private boolean adaptiveBatchSize = false;

        /**
         * 自适应批量大小最小值
         */
        private int adaptiveBatchMin = 100;

        /**
         * 自适应批量大小最大值
         */
        private int adaptiveBatchMax = 5000;

        /**
         * 自适应批量大小调整步长
         */
        private int adaptiveBatchStep = 100;

        /**
         * 自适应批量大小目标刷新耗时（毫秒），低于此值增大批次，高于此值减小批次
         */
        private long adaptiveTargetFlushMs = 500;
    }

    @Data
    public static class Archive {
        /**
         * 是否启用归档
         */
        private boolean enabled = true;

        /**
         * 冷数据天数（超过此天数的数据将被归档）
         */
        private int coldDays = 7;

        /**
         * 归档定时任务 Cron 表达式，默认每天凌晨 2 点
         */
        private String cron = "0 0 2 * * ?";

        /**
         * 归档文件格式：CSV, PARQUET
         */
        private String format = "CSV";

        /**
         * 存储类型：OSS, S3, MINIO, LOCAL
         */
        private String storageType = "OSS";

        /**
         * OSS 配置
         */
        private Oss oss = new Oss();

        @Data
        public static class Oss {
            private String endpoint;
            private String accessKeyId;
            private String accessKeySecret;
            private String bucketName;
            private String prefix = "audit-logs/";
        }
    }
}
