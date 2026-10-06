package com.carlos.redis.config;

import io.lettuce.core.ReadFrom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link RedisConnectionConfig} 单元测试。
 *
 * <p>守护读写分离默认值的优先级：未配置 {@code spring.data.redis.lettuce.read-from} 时
 * 默认 {@code REPLICA_PREFERRED}；一旦显式配置则以配置为准，不被覆盖。</p>
 *
 * <p>此测试不依赖真实 Redis：只检查 {@code LettuceClientConfiguration} 上解析出的 {@code ReadFrom}。</p>
 *
 * @author Carlos
 */
class RedisConnectionConfigTest {

    @Test
    @DisplayName("未配置 read-from 时，customizer 应设 REPLICA_PREFERRED（读写分离默认开）")
    void should_set_replica_preferred_when_read_from_not_configured() {
        RedisConnectionConfig config = new RedisConnectionConfig();
        DataRedisProperties properties = new DataRedisProperties();

        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
        config.readFromCustomizer(properties).customize(builder);

        assertEquals(ReadFrom.REPLICA_PREFERRED, builder.build().getReadFrom().orElse(null),
            "未显式配置 read-from 时应默认读副本；若此处不是 REPLICA_PREFERRED，读写分离不成立");
    }

    @Test
    @DisplayName("已配置 read-from 时，customizer 不应覆盖（配置优先）")
    void should_not_override_when_read_from_configured() {
        RedisConnectionConfig config = new RedisConnectionConfig();
        DataRedisProperties properties = new DataRedisProperties();
        properties.getLettuce().setReadFrom("master");

        // 模拟 applyProperties 已按配置设过值，随后 customizer 才执行
        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
        builder.readFrom(ReadFrom.MASTER);
        config.readFromCustomizer(properties).customize(builder);

        assertEquals(ReadFrom.MASTER, builder.build().getReadFrom().orElse(null),
            "用户在 spring.data.redis.lettuce.read-from 的配置被 customizer 覆盖了 —— "
                + "customizer 在 applyProperties 之后执行，必须自行判断配置是否已指定");
    }
}
