package com.carlos.redis.ratelimit;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.interfaces.ApplicationExtend;
import com.carlos.core.util.SpelUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 限流注解 AOP 切面
 * <p>
 * 处理 @RateLimit 注解，实现分布式限流控制。
 * 支持多种限流类型、策略和自定义键表达式。
 *
 * @author carlos
 * @date 2026-04-08
 */
@Slf4j
@Aspect
@Order(-50) // 在事务注解之前，分布式锁之后执行
@AllArgsConstructor
public class RateLimitAspect {

    private final RedissonClient redissonClient;
    private final RateLimitProperties properties;

    /**
     * 应用扩展（可选）：用于取当前登录用户 ID。
     *
     * <p>用 {@link ObjectProvider} 而非直接注入 —— 它<b>永远可注入</b>（无实现时返回 null），
     * 因此没有 {@code ApplicationExtend} 实现的服务（如 carlos-job）不会因为本依赖而起不来。
     * 该接口由各服务实现，最终从 SecurityContext 取用户，故此处拿到的身份来自网关签名断言，
     * 不依赖任何可伪造的请求头。</p>
     */
    private final ObjectProvider<ApplicationExtend> applicationExtendProvider;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String rateLimitKey = buildRateLimitKey(joinPoint, rateLimit);
        RRateLimiter rateLimiter = getOrCreateRateLimiter(rateLimitKey, rateLimit);

        RateLimitStrategy strategy = rateLimit.strategy();

        return switch (strategy) {
            case FAIL_FAST -> handleFailFast(joinPoint, rateLimiter, rateLimitKey);
            case BLOCK -> handleBlock(joinPoint, rateLimiter, rateLimit, rateLimitKey);
            case FALLBACK -> handleFallback(joinPoint, rateLimiter, rateLimit, rateLimitKey);
        };
    }

    /**
     * 快速失败策略处理
     */
    private Object handleFailFast(ProceedingJoinPoint joinPoint, RRateLimiter rateLimiter,
                                  String rateLimitKey) throws Throwable {
        boolean acquired = rateLimiter.tryAcquire();

        if (!acquired) {
            log.warn("[RateLimit] Rate limit triggered (FAIL_FAST): {}", rateLimitKey);
            throw new RateLimitException(rateLimitKey);
        }

        log.debug("[RateLimit] Acquired permit (FAIL_FAST): {}", rateLimitKey);

        return joinPoint.proceed();
    }

    /**
     * 阻塞等待策略处理
     */
    private Object handleBlock(ProceedingJoinPoint joinPoint, RRateLimiter rateLimiter,
                               RateLimit rateLimit, String rateLimitKey) throws Throwable {
        long maxWaitTime = rateLimit.maxWaitTime();

        boolean acquired = rateLimiter.tryAcquire(1, maxWaitTime, TimeUnit.MILLISECONDS);

        if (!acquired) {
            log.warn("[RateLimit] Rate limit triggered (BLOCK timeout): {}, waitTime: {}ms", rateLimitKey, maxWaitTime);
            throw new RateLimitException(rateLimitKey, maxWaitTime);
        }

        log.debug("[RateLimit] Acquired permit (BLOCK): {}, waitTime: {}ms", rateLimitKey, maxWaitTime);

        return joinPoint.proceed();
    }

    /**
     * 降级策略处理
     */
    private Object handleFallback(ProceedingJoinPoint joinPoint, RRateLimiter rateLimiter,
                                  RateLimit rateLimit, String rateLimitKey) throws Throwable {
        boolean acquired = rateLimiter.tryAcquire();

        if (acquired) {
            log.debug("[RateLimit] Acquired permit (FALLBACK): {}", rateLimitKey);
            return joinPoint.proceed();
        }

        log.warn("[RateLimit] Rate limit triggered (FALLBACK): {}", rateLimitKey);

        // 执行降级逻辑
        String fallbackMethod = rateLimit.fallbackMethod();
        if (StrUtil.isBlank(fallbackMethod)) {
            log.error("[RateLimit] Fallback method not specified: {}", rateLimitKey);
            throw new RateLimitException(rateLimitKey, "服务繁忙，请稍后重试");
        }

        return invokeFallbackMethod(joinPoint, fallbackMethod);
    }

    /**
     * 调用降级方法
     */
    private Object invokeFallbackMethod(ProceedingJoinPoint joinPoint, String fallbackMethod) throws Exception {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = signature.getMethod();
        Object target = joinPoint.getTarget();
        Object[] args = joinPoint.getArgs();

        try {
            // 查找降级方法（与原方法同名参数）
            Method fallback = target.getClass().getMethod(fallbackMethod, targetMethod.getParameterTypes());
            fallback.setAccessible(true);
            return fallback.invoke(target, args);
        } catch (NoSuchMethodException e) {
            log.error("[RateLimit] Fallback method not found: {}.{}",
                target.getClass().getName(), fallbackMethod);
            throw new RateLimitException("fallback", "降级方法不存在: " + fallbackMethod);
        } catch (Exception e) {
            log.error("[RateLimit] Fallback method execution failed: {}.{}",
                target.getClass().getName(), fallbackMethod, e);
            throw new RateLimitException("fallback", "降级方法执行失败");
        }
    }

    /**
     * 获取或创建限流器
     */
    private RRateLimiter getOrCreateRateLimiter(String rateLimitKey, RateLimit rateLimit) {
        String fullKey = properties.getPrefix() + rateLimitKey;
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(fullKey);

        if (!rateLimiter.isExists()) {
            long rate = rateLimit.rate();
            long capacity = rateLimit.capacity() > 0 ? rateLimit.capacity() : rate;
            long rateInterval = rateLimit.rateInterval();
            RateIntervalUnit intervalUnit = convertToRateIntervalUnit(rateLimit.rateIntervalUnit());

            // 尝试设置限流规则
            boolean success = rateLimiter.trySetRate(RateType.OVERALL, rate, rateInterval, intervalUnit);

            if (success) {
                log.info("[RateLimit] Created rate limiter: {}, rate: {}/{}, capacity: {}",
                    rateLimitKey, rate, rateLimit.rateIntervalUnit(), capacity);
            }
        }

        return rateLimiter;
    }

    /**
     * 构建限流键
     */
    private String buildRateLimitKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        RateLimitType type = rateLimit.type();
        String key = rateLimit.key();

        // 构建基础键
        StringBuilder keyBuilder = new StringBuilder();

        // 添加方法签名作为默认键
        if (StrUtil.isBlank(key)) {
            Signature signature = joinPoint.getSignature();
            keyBuilder.append(signature.getDeclaringTypeName()).append(".").append(signature.getName());
        } else {
            keyBuilder.append(key);
        }

        // 根据限流类型添加维度标识
        String dimensionKey = switch (type) {
            case GLOBAL -> "";
            case USER -> getUserKey(joinPoint, rateLimit);
            case IP -> getIpKey();
            case CUSTOM -> getCustomKey(joinPoint, rateLimit);
        };

        if (StrUtil.isNotBlank(dimensionKey)) {
            keyBuilder.append(":").append(dimensionKey);
        }

        return keyBuilder.toString();
    }

    /**
     * 获取用户维度键
     */
    private String getUserKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        // 尝试从自定义表达式获取用户标识
        String customExpr = rateLimit.customKeyExpression();
        if (StrUtil.isNotBlank(customExpr)) {
            return parseSpelExpression(joinPoint, customExpr);
        }

        // 优先取已验签的登录身份（经 ApplicationExtend → SecurityContext）
        ApplicationExtend applicationExtend = applicationExtendProvider.getIfAvailable();
        if (applicationExtend != null) {
            Serializable userId = applicationExtend.getUserId();
            if (userId != null) {
                return userId.toString();
            }
        }

        // 兜底的请求属性（历史机制，当前无写入方）
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            Object userId = attributes.getRequest().getAttribute("userId");
            if (userId != null) {
                return userId.toString();
            }
        }

        log.warn("[RateLimit] Cannot get user key, fallback to 'unknown'. Key: {}", rateLimit.key());
        return "unknown";
    }

    /**
     * 获取IP维度键
     */
    private String getIpKey() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }

        HttpServletRequest request = attributes.getRequest();
        return getClientIp(request);
    }

    /**
     * 获取自定义维度键
     */
    private String getCustomKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String customExpr = rateLimit.customKeyExpression();
        if (StrUtil.isBlank(customExpr)) {
            log.warn("[RateLimit] CUSTOM type requires customKeyExpression");
            return "default";
        }
        return parseSpelExpression(joinPoint, customExpr);
    }

    /**
     * 解析 SpEL 表达式
     */
    private String parseSpelExpression(ProceedingJoinPoint joinPoint, String expression) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = methodSignature.getMethod();
        Object target = joinPoint.getTarget();
        Object[] arguments = joinPoint.getArgs();

        return SpelUtil.parse(target, expression, targetMethod, arguments);
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // 处理多个IP的情况，取第一个
                int index = ip.indexOf(",");
                if (index != -1) {
                    ip = ip.substring(0, index).trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * 转换时间单位
     */
    private RateIntervalUnit convertToRateIntervalUnit(TimeUnit timeUnit) {
        return switch (timeUnit) {
            case NANOSECONDS, MICROSECONDS, MILLISECONDS -> RateIntervalUnit.MILLISECONDS;
            case SECONDS -> RateIntervalUnit.SECONDS;
            case MINUTES -> RateIntervalUnit.MINUTES;
            case HOURS -> RateIntervalUnit.HOURS;
            case DAYS -> RateIntervalUnit.HOURS; // Redisson 不支持 DAYS
        };
    }
}
