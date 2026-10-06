package com.carlos.monitor.audit;

import cn.hutool.core.util.StrUtil;
import com.carlos.monitor.redis.proxy.RedisDataProxy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * <p>
 * 监控操作审计切面（@MonitorAudit）
 * 解析 SpEL 取实例与参数摘要，方法执行前抓前值快照（写操作），
 * 执行后组装事件（操作人/结果/耗时/来源 IP）经 {@link MonitorAuditReporter} 异步上报；
 * 失败操作同样上报；切面的任何异常都不影响业务方法执行结果
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MonitorAuditAspect {

    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();

    private static final ParameterNameDiscoverer PARAM_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private final MonitorAuditReporter auditReporter;

    private final RedisDataProxy redisDataProxy;

    @Around("@annotation(monitorAudit)")
    public Object around(ProceedingJoinPoint joinPoint, MonitorAudit monitorAudit) throws Throwable {
        long start = System.currentTimeMillis();
        StandardEvaluationContext evalContext = buildEvalContext(joinPoint);

        // 前值快照（写操作）：要求方法签名含 instanceId 与 key 参数，失败不阻断
        String snapshot = null;
        if (monitorAudit.snapshot()) {
            snapshot = takeSnapshot(evalContext);
        }

        boolean success = true;
        String resultMessage = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            success = false;
            resultMessage = throwable.getMessage();
            throw throwable;
        } finally {
            reportSafely(joinPoint, monitorAudit, evalContext, snapshot, success, resultMessage,
                System.currentTimeMillis() - start);
        }
    }

    private void reportSafely(ProceedingJoinPoint joinPoint, MonitorAudit monitorAudit,
                              StandardEvaluationContext evalContext, String snapshot,
                              boolean success, String resultMessage, long durationMs) {
        try {
            String target = evalSpel(monitorAudit.target(), evalContext);
            if (StrUtil.isBlank(target)) {
                target = joinPoint.getSignature().toShortString();
            }
            auditReporter.report(new MonitorAuditReporter.AuditEvent(
                monitorAudit.action(), target, snapshot, success,
                StrUtil.maxLength(resultMessage, 500), durationMs, clientIp(), monitorAudit.riskLevel()));
        } catch (Exception e) {
            log.warn("审计事件组装失败（已容忍）, action:{}, error:{}", monitorAudit.action(), e.getMessage());
        }
    }

    private String takeSnapshot(StandardEvaluationContext evalContext) {
        try {
            // 参数名兼容：instanceId 或 id（controller 路径变量两种命名都存在）
            Object instanceId = evalContext.lookupVariable("instanceId");
            if (instanceId == null) {
                instanceId = evalContext.lookupVariable("id");
            }
            Object key = evalContext.lookupVariable("key");
            if (instanceId == null || key == null) {
                return null;
            }
            Object db = evalContext.lookupVariable("db");
            Integer dbIndex = db == null ? null : Integer.valueOf(db.toString());
            return redisDataProxy.snapshotForAudit(Long.valueOf(instanceId.toString()), key.toString(), dbIndex);
        } catch (Exception e) {
            log.warn("前值快照抓取失败（已容忍）, error:{}", e.getMessage());
            return null;
        }
    }

    private StandardEvaluationContext buildEvalContext(ProceedingJoinPoint joinPoint) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = PARAM_NAME_DISCOVERER.getParameterNames(method);
        Object[] args = joinPoint.getArgs();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        return context;
    }

    private String evalSpel(String spel, StandardEvaluationContext context) {
        if (StrUtil.isBlank(spel)) {
            return null;
        }
        try {
            Object value = SPEL_PARSER.parseExpression(spel).getValue(context);
            return value == null ? null : value.toString();
        } catch (Exception e) {
            log.debug("审计 SpEL 解析失败: {}, error:{}", spel, e.getMessage());
            return null;
        }
    }

    private String clientIp() {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                HttpServletRequest request = attributes.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                if (StrUtil.isNotBlank(forwarded)) {
                    return forwarded.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("获取来源 IP 失败: {}", e.getMessage());
        }
        return null;
    }
}
