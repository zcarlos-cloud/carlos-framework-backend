package com.carlos.monitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * 系统监控配置属性（前缀 carlos.monitor）
 * 字段对齐《DESIGN-20260930-系统监控模块功能设计》第 5 节配置设计；
 * 由 MonitorAutoConfiguration 的 @EnableConfigurationProperties 注册
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Data
@ConfigurationProperties(prefix = "carlos.monitor")
public class MonitorProperties {

    /**
     * 采集配置
     */
    private Collect collect = new Collect();

    /**
     * 指标写入（sink）配置
     */
    private Sink sink = new Sink();

    /**
     * 实例连接管理配置
     */
    private Connection connection = new Connection();

    /**
     * Redis 数据管理配置
     */
    private Redis redis = new Redis();

    /**
     * SQL 控制台配置
     */
    private SqlConsole sqlConsole = new SqlConsole();

    /**
     * 告警配置
     */
    private Alarm alarm = new Alarm();

    /**
     * 微服务监控配置
     */
    private ServiceMonitor serviceMonitor = new ServiceMonitor();

    @Data
    public static class Collect {

        /**
         * 是否启用采集
         */
        private boolean enabled = true;

        /**
         * job 分片数
         */
        private int shardCount = 8;

        /**
         * 单任务内并发采集数（虚拟线程）
         */
        private int parallelism = 32;

        /**
         * Redis SCAN COUNT
         */
        private int scanCount = 500;

        /**
         * SCAN 批间隔（毫秒）
         */
        private int scanIntervalMs = 20;

        /**
         * 大 key 扫描配置
         */
        private Bigkey bigkey = new Bigkey();

        @Data
        public static class Bigkey {

            /**
             * 大 key 扫描 cron，默认每日 02:30
             */
            private String cron = "0 30 2 * * ?";

            /**
             * 报告 Top N
             */
            private int topN = 500;

            /**
             * 批间休眠（毫秒）
             */
            private int batchSleepMs = 50;
        }
    }

    @Data
    public static class Sink {

        /**
         * ClickHouse 写入配置
         */
        private ClickHouse clickhouse = new ClickHouse();

        /**
         * Disruptor 环形缓冲配置
         */
        private Disruptor disruptor = new Disruptor();

        @Data
        public static class ClickHouse {

            /**
             * ClickHouse JDBC 地址
             */
            private String url = "jdbc:clickhouse://localhost:8123/default";

            /**
             * 用户名
             */
            private String username = "default";

            /**
             * 密码
             */
            private String password = "";

            /**
             * 批次大小
             */
            private int batchSize = 2000;

            /**
             * 刷新间隔（毫秒）
             */
            private int flushIntervalMs = 3000;

            /**
             * 写入失败本地备份目录
             */
            private String backupDir = "logs/monitor/backup";
        }

        @Data
        public static class Disruptor {

            /**
             * RingBuffer 大小（2 的幂次）
             */
            private int ringSize = 65536;
        }
    }

    @Data
    public static class Connection {

        /**
         * 实例连接注册中心最大容量
         */
        private int registryMaxSize = 500;

        /**
         * 连接空闲驱逐时间（分钟）
         */
        private int expireAfterAccessMin = 30;

        /**
         * 被监控 DB 实例连接池最大活跃连接数（监控专用小池）
         */
        private int dbPoolMaxActive = 8;
    }

    @Data
    public static class Redis {

        /**
         * 高危操作确认令牌有效期（秒）
         */
        private int confirmTokenTtlSec = 60;

        /**
         * 批量删除每批数量
         */
        private int batchDeleteSize = 500;

        /**
         * 批量删除批间休眠（毫秒）
         */
        private int batchDeleteSleepMs = 100;

        /**
         * key 浏览单次接口调用最多推进的 SCAN 批次数（防长阻塞）
         */
        private int scanMaxBatches = 20;
    }

    @Data
    public static class SqlConsole {

        /**
         * SELECT 默认包裹 LIMIT
         */
        private int defaultLimit = 500;

        /**
         * SELECT 硬上限 LIMIT
         */
        private int maxLimit = 5000;

        /**
         * 语句超时（秒）
         */
        private int statementTimeoutSec = 10;

        /**
         * 写执行影响行数保护阈值（超出回滚）
         */
        private int writeMaxAffectedRows = 10000;
    }

    @Data
    public static class Alarm {

        /**
         * 告警规则缓存时间（秒）
         */
        private int ruleCacheSec = 60;

        /**
         * 恢复时是否发送通知
         */
        private boolean recoverNotify = true;
    }

    @Data
    public static class ServiceMonitor {

        /**
         * Actuator 基础路径
         */
        private String actuatorBasePath = "/actuator";

        /**
         * Actuator 连接超时（毫秒）
         */
        private int connectTimeoutMs = 2000;

        /**
         * Actuator 读取超时（毫秒）
         */
        private int readTimeoutMs = 3000;

        /**
         * Actuator 鉴权头名称（内网免鉴权时留空；配置后随请求携带）
         */
        private String authHeaderName;

        /**
         * Actuator 鉴权头值
         */
        private String authHeaderValue;

        /**
         * SkyWalking 跳转链接模板（含 {service} 占位符，空表示不展示入口）
         */
        private String skywalkingUrlTemplate;
    }
}
