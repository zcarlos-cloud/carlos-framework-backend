package com.carlos.redis.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.resource.ClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientOptionsBuilderCustomizer;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.RedisStaticMasterReplicaConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * <p>
 * Redis 连接配置
 * </p>
 *
 * <p><strong>为什么本类需要显式提供 Lettuce 连接工厂：</strong></p>
 * <p>Spring Boot 自带的 {@code LettuceConnectionConfiguration} 带
 * {@code @ConditionalOnMissingBean(RedisConnectionFactory.class)}，而
 * {@code redisson-spring-boot-starter} 的 {@code RedissonAutoConfigurationV4}
 * 声明了 {@code @AutoConfiguration(before = DataRedisAutoConfiguration.class)}，
 * 故意抢在 Boot 之前注册 {@code RedissonConnectionFactory} —— 这是 Redisson
 * 「接管默认连接工厂」的既定策略。结果 Boot 的 Lettuce 工厂永远不注册。</p>
 * <p>而 {@code RedisUtil} 必须走 Lettuce：{@code RedissonConnection.eval} 在管道 /
 * 事务模式下直接抛 {@code UnsupportedOperationException}，{@code select} 亦无条件抛出，
 * 无法承载 Lua、bitOp、多 DB 等用法。故在此显式补齐，<strong>本 bean 不可删除</strong>。</p>
 *
 * <p><strong>与 Boot 的关系（重要）：</strong></p>
 * <p>Boot 的 {@code LettuceConnectionConfiguration} 是 package-private（核心逻辑还在
 * 同样不可见的父类 {@code DataRedisConnectionConfiguration} 里），<strong>无法
 * {@code @Import} 复用</strong>，因此本类按其逻辑自行构造，覆盖
 * {@code applyProperties} / {@code createClientOptions} / {@code PoolBuilderFactory} /
 * URL 处理等全部配置路径，力求不丢失任何 {@code spring.data.redis.*} 配置项。</p>
 * <p><strong>维护提示：</strong>本类与 Boot 存在代码重复，Boot 升级时需人工比对
 * {@code LettuceConnectionConfiguration} 与 {@code DataRedisConnectionConfiguration}
 * 是否新增了配置项。</p>
 *
 * <p><strong>拓扑兼容性：</strong>单机 / 静态主从 / 哨兵 / 集群四种部署方式，全部由
 * {@code spring.data.redis.*} 驱动，判定优先级与 Boot 一致：哨兵 → 集群 → 静态主从 → 单机。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@AutoConfiguration
@ConditionalOnClass(RedisClient.class)
@ConditionalOnProperty(name = "spring.data.redis.client-type", havingValue = "lettuce", matchIfMissing = true)
public class RedisConnectionConfig {

    private static final boolean COMMONS_POOL2_AVAILABLE = ClassUtils.isPresent(
        "org.apache.commons.pool2.ObjectPool", RedisConnectionConfig.class.getClassLoader());

    /**
     * 读写分离默认值：读操作优先走副本（REPLICA_PREFERRED）。
     *
     * <p><strong>仅在 {@code spring.data.redis.lettuce.read-from} 未配置时生效</strong> ——
     * 该属性一旦显式配置，以配置为准。原因：本 customizer 在 {@code applyProperties} 之后执行，
     * 而 {@code ReadFrom} 是 setter 语义，若不判断就会把用户的配置覆盖掉。</p>
     *
     * <p>各拓扑下的行为：单机无副本可用时自动回退主节点；主从 / 静态主从从 master 的
     * {@code INFO replication} 自动发现副本；哨兵经哨兵解析拓扑。</p>
     *
     * <p>写、EVAL、事务不受此设置影响：Lettuce 按命令类型强制路由到主节点。</p>
     */
    @Bean
    public LettuceClientConfigurationBuilderCustomizer readFromCustomizer(DataRedisProperties properties) {
        return builder -> {
            if (properties.getLettuce().getReadFrom() == null) {
                builder.readFrom(ReadFrom.REPLICA_PREFERRED);
            }
        };
    }

    /**
     * Lettuce 连接工厂（平台线程模式）。
     *
     * <p>条件用 {@code LettuceConnectionFactory} 而非 {@code RedisConnectionFactory} ——
     * 后者会被 {@code RedissonConnectionFactory} 满足，那正是 Boot 工厂不注册的原因。</p>
     */
    @Bean("lettuceConnectionFactory")
    @ConditionalOnMissingBean(LettuceConnectionFactory.class)
    @ConditionalOnThreading(Threading.PLATFORM)
    public LettuceConnectionFactory lettuceConnectionFactory(
        DataRedisConnectionDetails connectionDetails,
        DataRedisProperties properties,
        ObjectProvider<LettuceClientConfigurationBuilderCustomizer> clientConfigurationBuilderCustomizers,
        ObjectProvider<LettuceClientOptionsBuilderCustomizer> clientOptionsBuilderCustomizers,
        ClientResources clientResources) {

        return createConnectionFactory(connectionDetails, properties, clientConfigurationBuilderCustomizers,
            clientOptionsBuilderCustomizers, clientResources);
    }

    /**
     * Lettuce 连接工厂（虚拟线程模式）。与上一个互斥，由 {@code @ConditionalOnThreading} 保证。
     */
    @Bean("lettuceConnectionFactoryVirtualThreads")
    @ConditionalOnMissingBean(LettuceConnectionFactory.class)
    @ConditionalOnThreading(Threading.VIRTUAL)
    public LettuceConnectionFactory lettuceConnectionFactoryVirtualThreads(
        DataRedisConnectionDetails connectionDetails,
        DataRedisProperties properties,
        ObjectProvider<LettuceClientConfigurationBuilderCustomizer> clientConfigurationBuilderCustomizers,
        ObjectProvider<LettuceClientOptionsBuilderCustomizer> clientOptionsBuilderCustomizers,
        ClientResources clientResources) {

        return createConnectionFactory(connectionDetails, properties, clientConfigurationBuilderCustomizers,
            clientOptionsBuilderCustomizers, clientResources);
    }

    private LettuceConnectionFactory createConnectionFactory(
        DataRedisConnectionDetails connectionDetails,
        DataRedisProperties properties,
        ObjectProvider<LettuceClientConfigurationBuilderCustomizer> clientConfigurationBuilderCustomizers,
        ObjectProvider<LettuceClientOptionsBuilderCustomizer> clientOptionsBuilderCustomizers,
        ClientResources clientResources) {

        LettuceClientConfiguration clientConfig = lettuceClientConfiguration(connectionDetails, properties,
            clientConfigurationBuilderCustomizers, clientOptionsBuilderCustomizers, clientResources);

        // 拓扑判定优先级与 Boot 一致：哨兵 → 集群 → 静态主从 → 单机
        RedisSentinelConfiguration sentinelConfig = sentinelConfiguration(connectionDetails);
        if (sentinelConfig != null) {
            return new LettuceConnectionFactory(sentinelConfig, clientConfig);
        }

        RedisClusterConfiguration clusterConfig = clusterConfiguration(connectionDetails, properties);
        if (clusterConfig != null) {
            return new LettuceConnectionFactory(clusterConfig, clientConfig);
        }

        RedisStaticMasterReplicaConfiguration masterReplicaConfig = masterReplicaConfiguration(connectionDetails);
        if (masterReplicaConfig != null) {
            return new LettuceConnectionFactory(masterReplicaConfig, clientConfig);
        }

        return new LettuceConnectionFactory(standaloneConfiguration(connectionDetails), clientConfig);
    }

    // region 客户端配置（对齐 Boot 的 getLettuceClientConfiguration）

    private LettuceClientConfiguration lettuceClientConfiguration(
        DataRedisConnectionDetails connectionDetails,
        DataRedisProperties properties,
        ObjectProvider<LettuceClientConfigurationBuilderCustomizer> clientConfigurationBuilderCustomizers,
        ObjectProvider<LettuceClientOptionsBuilderCustomizer> clientOptionsBuilderCustomizers,
        ClientResources clientResources) {

        DataRedisProperties.Lettuce lettuceProperties = properties.getLettuce();
        LettuceClientConfigurationBuilder builder = createBuilder(lettuceProperties.getPool());

        SslBundle sslBundle = connectionDetails.getSslBundle();
        applyProperties(builder, properties, sslBundle);

        String url = properties.getUrl();
        if (StringUtils.hasText(url)) {
            customizeConfigurationFromUrl(builder, url);
        }

        builder.clientOptions(createClientOptions(properties, clientOptionsBuilderCustomizers, sslBundle));
        builder.clientResources(clientResources);
        clientConfigurationBuilderCustomizers.orderedStream().forEach(customizer -> customizer.customize(builder));

        return builder.build();
    }

    /**
     * 连接池：仅在启用时使用 {@link LettucePoolingClientConfiguration}。
     */
    private LettuceClientConfigurationBuilder createBuilder(DataRedisProperties.Pool pool) {
        if (isPoolEnabled(pool)) {
            return LettucePoolingClientConfiguration.builder().poolConfig(getPoolConfig(pool));
        }
        return LettuceClientConfiguration.builder();
    }

    private boolean isPoolEnabled(DataRedisProperties.Pool pool) {
        Boolean enabled = pool.getEnabled();
        return (enabled != null) ? enabled : COMMONS_POOL2_AVAILABLE;
    }

    private GenericObjectPoolConfig<StatefulConnection<?, ?>> getPoolConfig(DataRedisProperties.Pool properties) {
        GenericObjectPoolConfig<StatefulConnection<?, ?>> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(properties.getMaxActive());
        config.setMaxIdle(properties.getMaxIdle());
        config.setMinIdle(properties.getMinIdle());
        if (properties.getTimeBetweenEvictionRuns() != null) {
            config.setTimeBetweenEvictionRuns(properties.getTimeBetweenEvictionRuns());
        }
        if (properties.getMaxWait() != null) {
            config.setMaxWait(properties.getMaxWait());
        }
        return config;
    }

    private void applyProperties(LettuceClientConfigurationBuilder builder, DataRedisProperties properties,
                                 SslBundle sslBundle) {

        if (sslBundle != null) {
            builder.useSsl();
        }
        if (properties.getTimeout() != null) {
            builder.commandTimeout(properties.getTimeout());
        }
        DataRedisProperties.Lettuce lettuce = properties.getLettuce();
        if (lettuce.getShutdownTimeout() != null && !lettuce.getShutdownTimeout().isZero()) {
            builder.shutdownTimeout(lettuce.getShutdownTimeout());
        }
        String readFrom = lettuce.getReadFrom();
        if (readFrom != null) {
            builder.readFrom(getReadFrom(readFrom));
        }
        if (StringUtils.hasText(properties.getClientName())) {
            builder.clientName(properties.getClientName());
        }
    }

    /**
     * 解析 {@code spring.data.redis.lettuce.read-from}，允许 {@code replicaPreferred}
     * 这类驼峰/连字符写法：先去掉非字母数字并转小写，再交由 {@link ReadFrom#valueOf}。
     * 冒号后的部分（如 {@code anyReplica:2}）原样保留。
     */
    private ReadFrom getReadFrom(String readFrom) {
        int index = readFrom.indexOf(':');
        if (index == -1) {
            return ReadFrom.valueOf(getCanonicalReadFromName(readFrom));
        }
        String name = getCanonicalReadFromName(readFrom.substring(0, index));
        String value = readFrom.substring(index + 1);
        return ReadFrom.valueOf(name + ":" + value);
    }

    private String getCanonicalReadFromName(String name) {
        StringBuilder canonicalName = new StringBuilder(name.length());
        name.chars()
            .filter(Character::isLetterOrDigit)
            .map(Character::toLowerCase)
            .forEach(c -> canonicalName.append((char) c));
        return canonicalName.toString();
    }

    /**
     * {@code spring.data.redis.url} 的 SSL 判定。Boot 的 {@code DataRedisUrl} 是
     * package-private 无法复用，此处等价实现：仅判断是否为 {@code rediss} 协议。
     * 其余连接信息由 {@code connectionDetails} 提供。
     */
    private void customizeConfigurationFromUrl(LettuceClientConfigurationBuilder builder, String url) {
        if (urlUsesSsl(url)) {
            builder.useSsl();
        }
    }

    private boolean urlUsesSsl(String url) {
        try {
            return "rediss".equals(new URI(url).getScheme());
        } catch (Exception ex) {
            return false;
        }
    }

    // endregion

    // region ClientOptions（连接超时、SSL 细节、集群拓扑刷新）

    private ClientOptions createClientOptions(
        DataRedisProperties properties,
        ObjectProvider<LettuceClientOptionsBuilderCustomizer> clientOptionsBuilderCustomizers,
        SslBundle sslBundle) {

        ClientOptions.Builder builder = initializeClientOptionsBuilder(properties);

        Duration connectTimeout = properties.getConnectTimeout();
        if (connectTimeout != null) {
            builder.socketOptions(SocketOptions.builder().connectTimeout(connectTimeout).build());
        }

        if (sslBundle != null) {
            io.lettuce.core.SslOptions.Builder sslOptionsBuilder = io.lettuce.core.SslOptions.builder();
            sslOptionsBuilder.keyManager(sslBundle.getManagers().getKeyManagerFactory());
            sslOptionsBuilder.trustManager(sslBundle.getManagers().getTrustManagerFactory());
            org.springframework.boot.ssl.SslOptions sslOptions = sslBundle.getOptions();
            if (sslOptions.getCiphers() != null) {
                sslOptionsBuilder.cipherSuites(sslOptions.getCiphers());
            }
            if (sslOptions.getEnabledProtocols() != null) {
                sslOptionsBuilder.protocols(sslOptions.getEnabledProtocols());
            }
            builder.sslOptions(sslOptionsBuilder.build());
        }

        builder.timeoutOptions(TimeoutOptions.enabled());
        clientOptionsBuilderCustomizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }

    private ClientOptions.Builder initializeClientOptionsBuilder(DataRedisProperties properties) {
        if (properties.getCluster() != null) {
            ClusterClientOptions.Builder builder = ClusterClientOptions.builder();
            DataRedisProperties.Lettuce.Cluster.Refresh refresh = properties.getLettuce().getCluster().getRefresh();
            ClusterTopologyRefreshOptions.Builder refreshBuilder = ClusterTopologyRefreshOptions.builder()
                .dynamicRefreshSources(refresh.isDynamicRefreshSources());
            if (refresh.getPeriod() != null) {
                refreshBuilder.enablePeriodicRefresh(refresh.getPeriod());
            }
            if (refresh.isAdaptive()) {
                refreshBuilder.enableAllAdaptiveRefreshTriggers();
            }
            return builder.topologyRefreshOptions(refreshBuilder.build());
        }
        return ClientOptions.builder();
    }

    // endregion

    // region 拓扑构造（对齐 Boot 的 DataRedisConnectionConfiguration）

    /**
     * 哨兵配置。{@code null} 表示当前不是哨兵部署。
     */
    private RedisSentinelConfiguration sentinelConfiguration(DataRedisConnectionDetails connectionDetails) {
        DataRedisConnectionDetails.Sentinel sentinel = connectionDetails.getSentinel();
        if (sentinel == null) {
            return null;
        }

        RedisSentinelConfiguration config = new RedisSentinelConfiguration();
        config.master(sentinel.getMaster());
        for (DataRedisConnectionDetails.Node node : sentinel.getNodes()) {
            config.addSentinel(new RedisNode(node.host(), node.port()));
        }

        // 数据节点认证
        config.setUsername(connectionDetails.getUsername());
        if (connectionDetails.getPassword() != null) {
            config.setPassword(RedisPassword.of(connectionDetails.getPassword()));
        }
        // 哨兵自身认证（可与数据节点不同）
        config.setSentinelUsername(sentinel.getUsername());
        if (sentinel.getPassword() != null) {
            config.setSentinelPassword(RedisPassword.of(sentinel.getPassword()));
        }
        config.setDatabase(sentinel.getDatabase());
        return config;
    }

    private RedisClusterConfiguration clusterConfiguration(DataRedisConnectionDetails connectionDetails,
                                                           DataRedisProperties properties) {
        DataRedisConnectionDetails.Cluster cluster = connectionDetails.getCluster();
        if (cluster == null) {
            return null;
        }

        RedisClusterConfiguration config = new RedisClusterConfiguration();
        for (DataRedisConnectionDetails.Node node : cluster.getNodes()) {
            config.addClusterNode(new RedisNode(node.host(), node.port()));
        }

        DataRedisProperties.Cluster clusterProperties = properties.getCluster();
        if (clusterProperties != null && clusterProperties.getMaxRedirects() != null) {
            config.setMaxRedirects(clusterProperties.getMaxRedirects());
        }

        config.setUsername(connectionDetails.getUsername());
        if (connectionDetails.getPassword() != null) {
            config.setPassword(RedisPassword.of(connectionDetails.getPassword()));
        }
        return config;
    }

    /**
     * 静态主从配置（显式列出主从节点，如 AWS ElastiCache 的主从部署）。
     */
    private RedisStaticMasterReplicaConfiguration masterReplicaConfiguration(
        DataRedisConnectionDetails connectionDetails) {

        DataRedisConnectionDetails.MasterReplica masterReplica = connectionDetails.getMasterReplica();
        if (masterReplica == null) {
            return null;
        }

        List<DataRedisConnectionDetails.Node> nodes = masterReplica.getNodes();
        if (nodes.isEmpty()) {
            return null;
        }

        DataRedisConnectionDetails.Node master = nodes.get(0);
        RedisStaticMasterReplicaConfiguration config =
            new RedisStaticMasterReplicaConfiguration(master.host(), master.port());
        nodes.stream().skip(1).forEach(node -> config.addNode(node.host(), node.port()));

        config.setUsername(connectionDetails.getUsername());
        if (connectionDetails.getPassword() != null) {
            config.setPassword(RedisPassword.of(connectionDetails.getPassword()));
        }
        return config;
    }

    /**
     * 单机配置（兜底分支，与 Boot 一致：{@code getStandalone()} 必然非 null）。
     */
    private RedisStandaloneConfiguration standaloneConfiguration(DataRedisConnectionDetails connectionDetails) {
        DataRedisConnectionDetails.Standalone standalone = connectionDetails.getStandalone();

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(standalone.getHost());
        config.setPort(standalone.getPort());
        config.setUsername(connectionDetails.getUsername());
        if (connectionDetails.getPassword() != null) {
            config.setPassword(RedisPassword.of(connectionDetails.getPassword()));
        }
        config.setDatabase(standalone.getDatabase());
        return config;
    }

    // endregion
}
