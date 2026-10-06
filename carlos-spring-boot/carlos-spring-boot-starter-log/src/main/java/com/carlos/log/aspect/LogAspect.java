package com.carlos.log.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.carlos.boot.web.request.RequestInfo;
import com.carlos.boot.web.request.WebRequestContext;
import com.carlos.core.auth.UserContext;
import com.carlos.disruptor.core.DisruptorTemplate;
import com.carlos.log.annotation.Log;
import com.carlos.log.entity.OperationLog;
import com.carlos.log.spel.SpelExpressionResolver;
import com.carlos.log.storage.LogStorage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 操作日志 AOP 切面
 * <p>
 * 支持 SpEL 表达式、异步记录、风险评估等功能
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@Aspect
public class LogAspect {

    private final SpelExpressionResolver spelResolver = new SpelExpressionResolver();

    // 存储相关（使用 ObjectProvider 使异步组件变为可选）
    private final DisruptorTemplate<OperationLog> disruptorTemplate;
    private final LogStorage logStorage;

    public LogAspect(ObjectProvider<DisruptorTemplate<OperationLog>> disruptorTemplateProvider,
                     LogStorage logStorage) {
        // 同步模式下 DisruptorTemplate 可能不存在，使用 getIfAvailable() 避免注入失败
        this.disruptorTemplate = disruptorTemplateProvider.getIfAvailable();
        this.logStorage = logStorage;
    }

    @Around("@annotation(logAnnotation)")
    public Object around(ProceedingJoinPoint point, Log logAnnotation) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 构建 SpEL 上下文
        StandardEvaluationContext context = spelResolver.createContext(point, null);

        // 检查记录条件
        if (StringUtils.hasText(logAnnotation.condition())) {
            boolean shouldLog = spelResolver.resolveBoolean(
                logAnnotation.condition(), context, true);
            if (!shouldLog) {
                return point.proceed();
            }
        }

        // 创建日志实体
        OperationLog operationLog = new OperationLog();

        // 填充基础信息
        fillBasicInfo(operationLog, logAnnotation, context);

        // 填充主体信息
        fillPrincipalInfo(operationLog, logAnnotation);

        // 填充请求信息
        fillRequestInfo(operationLog);

        // 解析目标对象信息
        fillTargetInfo(operationLog, logAnnotation, context);

        // 记录请求参数
        if (logAnnotation.saveRequestParams()) {
            recordRequestParams(operationLog, point, logAnnotation);
        }

        // 执行目标方法
        Object result = null;
        Throwable exception = null;

        try {
            result = point.proceed();

            // 成功状态
            operationLog.setLogStatus("SUCCESS");

            // 记录响应数据
            if (logAnnotation.saveResponseData() && result != null) {
                recordResponseData(operationLog, result, logAnnotation);
            }

            // 更新上下文中的 result
            context.setVariable("result", result);

            // 重新解析可能依赖 result 的表达式
            if (StringUtils.hasText(logAnnotation.targetName())) {
                String targetName = spelResolver.resolve(logAnnotation.targetName(), context);
                operationLog.setTargetName(targetName);
            }

        } catch (Throwable e) {
            exception = e;
            operationLog.setLogStatus("FAIL");
            operationLog.setException(e.getMessage());
            operationLog.setExceptionClass(e.getClass().getName());
        } finally {
            // 计算耗时
            if (logAnnotation.recordDuration()) {
                operationLog.setDurationMs((int) (System.currentTimeMillis() - startTime));
            }

            // 设置时间信息
            LocalDateTime now = LocalDateTime.now();
            operationLog.setServerTime(now);
            operationLog.setEventTime(now);
            operationLog.setEventDate(LocalDate.now());
            operationLog.setCreatedTime(now);
            operationLog.setRetentionDeadline(LocalDate.now().plusDays(90));

            // 发送日志
            sendLog(operationLog, logAnnotation);
        }

        // 如果发生异常，继续抛出
        if (exception != null) {
            throw exception;
        }

        return result;
    }

    /**
     * 填充基础信息
     */
    private void fillBasicInfo(OperationLog operationLog, Log logAnnotation,
                               StandardEvaluationContext context) {
        // Schema 版本
        operationLog.setLogSchemaVersion(1);

        // 风险等级
        operationLog.setRiskLevel(logAnnotation.riskLevel());

        // 日志类型
        String logType = logAnnotation.logType();
        if (!StringUtils.hasText(logType) && StringUtils.hasText(logAnnotation.title())) {
            logType = logAnnotation.title() + "_" + logAnnotation.businessType().name();
        }
        operationLog.setLogType(logType);

        // 类别（从 title 推断或默认 BUSINESS）
        operationLog.setCategory("BUSINESS");

        // 操作描述（支持 SpEL）
        if (StringUtils.hasText(logAnnotation.operation())) {
            String operation = spelResolver.resolve(logAnnotation.operation(), context);
            operationLog.setOperation(operation);
        } else {
            operationLog.setOperation(logAnnotation.businessType().getInfo());
        }

        // 业务渠道（支持 SpEL）
        if (StringUtils.hasText(logAnnotation.bizChannel())) {
            String bizChannel = spelResolver.resolve(logAnnotation.bizChannel(), context);
            operationLog.setBizChannel(bizChannel);
        }

        // 业务场景（支持 SpEL）
        if (StringUtils.hasText(logAnnotation.bizScene())) {
            String bizScene = spelResolver.resolve(logAnnotation.bizScene(), context);
            operationLog.setBizScene(bizScene);
        }
    }

    /**
     * 填充主体信息（操作人）
     */
    private void fillPrincipalInfo(OperationLog operationLog, Log logAnnotation) {
        try {
            UserContext userContext = WebRequestContext.getUserContext();

            if (userContext != null) {
                // 主体ID
                if (userContext.getUserId() != null) {
                    String userId = String.valueOf(userContext.getUserId());
                    operationLog.setPrincipalId(userId);
                }

                // 主体名称
                String account = userContext.getAccount();
                operationLog.setPrincipalName(account);

                // 租户ID
                if (userContext.getTenantId() != null) {
                    operationLog.setTenantId(String.valueOf(userContext.getTenantId()));
                }

                // 部门ID
                if (userContext.getDepartmentId() != null) {
                    operationLog.setDeptId(String.valueOf(userContext.getDepartmentId()));
                }

                // 主体类型
                operationLog.setPrincipalType("USER");
            } else {
                // 无登录身份（如定时任务、未认证调用）：标记为匿名，不再尝试从请求头推断 ——
                // 明文身份头已随「签名断言」方案废弃，网关不再注入，读取它只会得到 null
                operationLog.setPrincipalType("ANONYMOUS");
            }
        } catch (Exception e) {
            log.debug("获取主体信息失败", e);
            operationLog.setPrincipalType("ANONYMOUS");
        }
    }

    /**
     * 填充请求信息
     */
    private void fillRequestInfo(OperationLog operationLog) {
        try {
            RequestInfo requestInfo = WebRequestContext.getRequestInfo();
            if (requestInfo != null) {
                operationLog.setUrl(requestInfo.getUrl());
                operationLog.setClientIp(requestInfo.getIp());
                // 追踪号：审计行据此与一次具体请求对上，排查时是第一手线索
                operationLog.setTraceId(requestInfo.getRequestId());
            }

            ServletRequestAttributes attributes = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                operationLog.setHttpMethod(request.getMethod());
                operationLog.setUserAgent(request.getHeader("User-Agent"));
                operationLog.setServerIp(request.getLocalAddr());
                operationLog.setClientPort(request.getRemotePort());

                // 应用信息
                String contextPath = request.getContextPath();
                operationLog.setAppName(contextPath != null ? contextPath.replace("/", "") : "");
            }

        } catch (Exception e) {
            log.debug("获取请求信息失败", e);
        }
    }

    /**
     * 填充目标对象信息
     */
    private void fillTargetInfo(OperationLog operationLog, Log logAnnotation, StandardEvaluationContext context) {
        operationLog.setTargetType(logAnnotation.targetType());

        if (StringUtils.hasText(logAnnotation.targetId())) {
            String targetId = spelResolver.resolve(logAnnotation.targetId(), context);
            operationLog.setTargetId(targetId);
        }

        if (StringUtils.hasText(logAnnotation.targetName())) {
            String targetName = spelResolver.resolve(logAnnotation.targetName(), context);
            operationLog.setTargetName(targetName);
        }
    }

    /**
     * 记录请求参数
     * <p>
     * 落库前按 {@link Log#excludeParams()} 剔除敏感内容，分两道：
     * </p>
     * <ol>
     *   <li><b>按参数名</b>剔除整个入参 —— 覆盖「敏感值作为裸标量传入」的情形，
     *       如 {@code (@RequestBody X param, @RequestHeader String token)} 里的 token。
     *       只按字段名过滤挡不住这种写法：它不是对象字段，没有字段名可匹配。</li>
     *   <li><b>按字段名</b>递归剔除对象/数组内的敏感字段（含嵌套）。</li>
     * </ol>
     * <p>
     * 该属性此前只声明未生效，导致明文口令 / 密钥被原样写入审计库（ClickHouse）——
     * 剔除必须在序列化<b>之前</b>做，事后脱敏做不到「不落库」。
     * </p>
     */
    private void recordRequestParams(OperationLog operationLog, ProceedingJoinPoint point, Log logAnnotation) {
        try {
            Object[] args = point.getArgs();
            if (args == null || args.length == 0) {
                return;
            }

            List<String> excluded = parseExcludedFields(logAnnotation.excludeParams());
            String[] paramNames = resolveParameterNames(point);

            List<Object> keptArgs = new ArrayList<>(args.length);
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                // 无法/无意义序列化的参数：Servlet 对象与文件（含 MultipartFile[] 这种批量上传形态）
                if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse
                    || containsMultipartFile(arg)) {
                    continue;
                }
                // 参数名本身敏感的裸标量：整体剔除
                if (i < paramNames.length && isExcluded(paramNames[i], excluded)) {
                    continue;
                }
                keptArgs.add(arg);
            }

            if (keptArgs.isEmpty()) {
                return;
            }

            // 序列化参数
            JSONArray params = JSONUtil.parseArray(keptArgs);

            // 剔除对象/数组内的敏感字段
            if (!excluded.isEmpty()) {
                removeExcludedFields(params, excluded);
            }

            String paramsJson = params.toString();

            // 长度限制
            if (paramsJson.length() > logAnnotation.maxParamLength()) {
                paramsJson = paramsJson.substring(0, logAnnotation.maxParamLength()) + "...(truncated)";
            }

            operationLog.setRequestPayloadRef(paramsJson);

        } catch (Exception e) {
            log.warn("序列化请求参数失败", e);
        }
    }

    /**
     * 参数是否含文件（含 {@code MultipartFile[]} 批量上传形态）
     * <p>
     * 只判 {@code instanceof MultipartFile} 会漏掉数组：{@code MultipartFile[]} 不是
     * {@code MultipartFile} 的实例，会一路走到 JSON 序列化 —— 而 Hutool 的 Bean 反射
     * 可能去调 {@code getBytes()}/{@code getInputStream()}，既无意义又可能抛错。
     * </p>
     */
    private boolean containsMultipartFile(Object arg) {
        if (arg instanceof MultipartFile) {
            return true;
        }
        if (arg instanceof Object[] array) {
            for (Object element : array) {
                if (element instanceof MultipartFile) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 取方法形参名
     * <p>
     * 依赖项目全局开启的 {@code -parameters}（见 carlos-parent 的 compilerArgs）；
     * 未开启时拿到的是 {@code arg0} 这类合成名，此时按参数名过滤静默失效 ——
     * 但按字段名过滤仍然有效，故不做额外处置，只在取值失败时返回空数组跳过该道。
     * </p>
     */
    private String[] resolveParameterNames(ProceedingJoinPoint point) {
        if (!(point.getSignature() instanceof MethodSignature signature)) {
            return new String[0];
        }
        Parameter[] parameters = signature.getMethod().getParameters();
        String[] names = new String[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            names[i] = parameters[i].getName();
        }
        return names;
    }

    /**
     * 解析待排除字段名，统一小写以便比对
     */
    private List<String> parseExcludedFields(String excludeParams) {
        if (StrUtil.isBlank(excludeParams)) {
            return List.of();
        }
        return StrUtil.split(excludeParams, ',').stream()
            .map(String::trim)
            .filter(StrUtil::isNotBlank)
            .map(field -> field.toLowerCase(Locale.ROOT))
            .toList();
    }

    /**
     * 递归剔除 JSON 中的敏感字段
     * <p>
     * 采用「包含即排除」而非全等匹配，理由与
     * {@code com.carlos.audit.util.SensitiveDataUtil#isSensitiveField} 一致：
     * 真实字段名多为 {@code newPassword} / {@code secretKey} 这类派生写法，
     * 全等匹配会漏。<b>宁可多排除，不可漏排除。</b>
     * </p>
     *
     * @param node     待处理的 JSON 节点（JSONObject / JSONArray / 标量）
     * @param excluded 已小写的排除词
     */
    private void removeExcludedFields(Object node, List<String> excluded) {
        if (node instanceof JSONObject jsonObject) {
            jsonObject.keySet().removeIf(key -> isExcluded(key, excluded));
            // 处理完本层再下钻，避免边遍历边删除
            for (Object value : jsonObject.values()) {
                removeExcludedFields(value, excluded);
            }
        } else if (node instanceof JSONArray jsonArray) {
            for (Object element : jsonArray) {
                removeExcludedFields(element, excluded);
            }
        }
    }

    /**
     * 字段名是否命中排除词
     */
    private boolean isExcluded(String fieldName, List<String> excluded) {
        if (fieldName == null) {
            return false;
        }
        String lowerName = fieldName.toLowerCase(Locale.ROOT);
        return excluded.stream().anyMatch(lowerName::contains);
    }

    /**
     * 记录响应数据
     */
    private void recordResponseData(OperationLog operationLog, Object result, Log logAnnotation) {
        try {
            String resultJson = JSONUtil.toJsonStr(result);

            if (resultJson.length() > logAnnotation.maxResponseLength()) {
                resultJson = resultJson.substring(0, logAnnotation.maxResponseLength()) + "...(truncated)";
            }

            operationLog.setResponsePayloadRef(resultJson);
        } catch (Exception e) {
            log.warn("序列化响应数据失败", e);
        }
    }

    /**
     * 发送日志
     */
    private void sendLog(OperationLog operationLog, Log logAnnotation) {
        try {
            if (logAnnotation.async() && disruptorTemplate != null) {
                // 异步发送
                disruptorTemplate.publishEvent(operationLog);
            } else {
                // 同步发送
                if (logStorage != null) {
                    logStorage.store(operationLog);
                }
            }
        } catch (Exception e) {
            log.error("发送操作日志失败", e);
        }
    }
}
