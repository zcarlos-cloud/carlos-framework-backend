package com.carlos.cloud.seata;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * <p>
 * Seata 分布式事务配置
 * </p>
 *
 * <p>
 * 自动配置 Seata 分布式事务功能，提供以下特性：
 * <ul>
 *   <li>全局事务管理</li>
 *   <li>AT 模式支持</li>
 *   <li>事务传播行为控制</li>
 *   <li>与 Spring Cloud 集成</li>
 * </ul>
 * </p>
 *
 * <p>
 * 使用方法：
 * <pre>
 * // 在业务方法上添加注解开启全局事务
 * {@code @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)}
 * public void createOrder(OrderDTO order) {
 *     // 调用库存服务扣减库存
 *     stockService.deduct(order.getSkuId(), order.getQuantity());
 *     // 调用账户服务扣减余额
 *     accountService.debit(order.getUserId(), order.getAmount());
 *     // 创建订单
 *     orderMapper.insert(order);
 * }
 * </pre>
 * </p>
 *
 * @author carlos
 * @date 2024/01/15
 * @see GlobalTransactional
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(GlobalTransactional.class)
@ConditionalOnProperty(prefix = "seata", name = "enabled", havingValue = "true", matchIfMissing = false)
public class SeataConfig {

    @PostConstruct
    public void init() {
        log.info("Seata 分布式事务已启用");
    }

    /**
     * Seata Feign 拦截器 - 传递 XID
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(RootContext.class)
    @ConditionalOnProperty(prefix = "seata", name = "enabled", havingValue = "true", matchIfMissing = false)
    public SeataFeignInterceptor seataFeignInterceptor() {
        log.info("初始化 Seata Feign 拦截器");
        return new SeataFeignInterceptor();
    }

    /**
     * Seata 上下文过滤器 - 接收并绑定 XID
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(RootContext.class)
    @ConditionalOnWebApplication
    @ConditionalOnProperty(prefix = "seata", name = "enabled", havingValue = "true", matchIfMissing = false)
    public SeataContextFilter seataContextFilter() {
        log.info("初始化 Seata 上下文过滤器");
        return new SeataContextFilter();
    }

    /**
     * Seata 配置常量
     */
    public static class Constants {
        /**
         * 事务分组
         */
        public static final String DEFAULT_TX_GROUP = "default_tx_group";

        /**
         * 应用 ID
         */
        public static final String DEFAULT_APPLICATION_ID = "seata-server";

        /**
         * 事务服务组
         */
        public static final String DEFAULT_SERVICE_GROUP = "my_test_tx_group";
    }
}
