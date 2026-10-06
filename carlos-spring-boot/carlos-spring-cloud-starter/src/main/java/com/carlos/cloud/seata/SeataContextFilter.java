package com.carlos.cloud.seata;

import org.apache.seata.core.context.RootContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * <p>
 * Seata 事务上下文过滤器
 * </p>
 *
 * <p>
 * 接收从上游服务传递过来的 Seata XID，并将其绑定到当前服务的 RootContext。
 * 此过滤器需要在最早阶段执行，确保在业务逻辑执行前完成 XID 绑定。
 * </p>
 *
 * <p>
 * 执行顺序：最高优先级（Ordered.HIGHEST_PRECEDENCE + 100）
 * </p>
 *
 * @author carlos
 * @date 2024/01/15
 */
@Slf4j
@Order(-100) // 确保在 Spring Security 过滤器之前执行
@ConditionalOnClass(RootContext.class)
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "seata", name = "enabled", havingValue = "true", matchIfMissing = false)
public class SeataContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String xid = httpServletRequest.getHeader(SeataFeignInterceptor.SEATA_XID_HEADER);

        boolean isBind = false;
        try {
            if (StringUtils.hasText(xid)) {
                RootContext.bind(xid);
                isBind = true;
                log.debug("绑定 Seata XID: {} 到当前上下文", xid);
            }
            chain.doFilter(request, response);
        } finally {
            if (isBind) {
                RootContext.unbind();
                log.debug("解绑 Seata XID: {}", xid);
            }
        }
    }
}
