package com.carlos.gateway.ratelimit.keyresolver;

import com.carlos.core.constant.HttpHeadersConstant;
import com.carlos.gateway.util.ClientIpUtil;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Carlos 网关 KeyResolver 策略枚举
 * 定义限流的维度类型
 * </p>
 *
 * @author carlos
 * @date 2026/3/24
 */
public enum CarlosKeyResolver {

    /**
     * 基于客户端 IP 限流
     */
    IP {
        @Override
        public KeyResolver getResolver() {
            return exchange -> Mono.just(ClientIpUtil.getClientIp(exchange.getRequest()));
        }
    },

    /**
     * 基于用户 ID 限流（从请求头获取）
     */
    USER {
        @Override
        public KeyResolver getResolver() {
            return exchange -> {
                String userId = exchange.getRequest().getHeaders().getFirst(HttpHeadersConstant.X_USER_ID);
                return Mono.just(userId != null ? userId : "anonymous");
            };
        }
    },

    /**
     * 基于请求路径限流
     */
    API {
        @Override
        public KeyResolver getResolver() {
            return exchange -> Mono.just(exchange.getRequest().getURI().getPath());
        }
    },

    /**
     * 基于请求方法 + 路径限流
     */
    METHOD_API {
        @Override
        public KeyResolver getResolver() {
            return exchange -> Mono.just(
                exchange.getRequest().getMethod() + ":" + exchange.getRequest().getURI().getPath()
            );
        }
    },

    /**
     * 组合限流：IP + 路径
     */
    IP_API {
        @Override
        public KeyResolver getResolver() {
            return exchange -> {
                String ip = ClientIpUtil.getClientIp(exchange.getRequest());
                String path = exchange.getRequest().getURI().getPath();
                return Mono.just(ip + ":" + path);
            };
        }
    },

    /**
     * 组合限流：IP + 用户
     */
    IP_USER {
        @Override
        public KeyResolver getResolver() {
            return exchange -> {
                String ip = ClientIpUtil.getClientIp(exchange.getRequest());
                String userId = exchange.getRequest().getHeaders().getFirst(HttpHeadersConstant.X_USER_ID);
                return Mono.just(ip + ":" + (userId != null ? userId : "anonymous"));
            };
        }
    },

    /**
     * 基于服务 ID 限流（从路由属性获取）
     */
    SERVICE {
        @Override
        public KeyResolver getResolver() {
            return exchange -> {
                String serviceId = exchange.getAttribute("org.springframework.cloud.gateway.support.ServerWebExchangeUtils.gatewayRoute");
                return Mono.just(serviceId != null ? serviceId : "default");
            };
        }
    };

    /**
     * 获取对应的 KeyResolver 实现
     *
     * @return KeyResolver 实例
     */
    public abstract KeyResolver getResolver();
}
