package com.carlos.cloud.seata;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.StringUtils;

/**
 * <p>
 * Seata 事务上下文 Feign 拦截器
 * </p>
 *
 * <p>
 * 在 Feign 调用时自动传递 Seata 全局事务 ID (XID)，实现跨服务的分布式事务传播。
 * </p>
 *
 * <p>
 * 工作原理：
 * <ol>
 *   <li>当服务 A 开启全局事务时，Seata 会生成一个 XID</li>
 *   <li>服务 A 通过 Feign 调用服务 B 时，此拦截器会将 XID 放入请求头</li>
 *   <li>服务 B 收到请求后，从请求头中取出 XID 并绑定到当前上下文</li>
 *   <li>服务 B 的操作会被纳入同一个全局事务管理</li>
 * </ol>
 * </p>
 *
 * @author carlos
 * @date 2024/01/15
 * @see RootContext
 */
@Slf4j
@ConditionalOnClass(RootContext.class)
@ConditionalOnProperty(prefix = "seata", name = "enabled", havingValue = "true", matchIfMissing = false)
public class SeataFeignInterceptor implements RequestInterceptor {

    /**
     * Seata XID 请求头名称
     */
    public static final String SEATA_XID_HEADER = "TX_XID";

    @Override
    public void apply(RequestTemplate requestTemplate) {
        String xid = RootContext.getXID();
        if (StringUtils.hasText(xid)) {
            requestTemplate.header(SEATA_XID_HEADER, xid);
            log.debug("传递 Seata XID: {} 到目标服务", xid);
        }
    }
}
