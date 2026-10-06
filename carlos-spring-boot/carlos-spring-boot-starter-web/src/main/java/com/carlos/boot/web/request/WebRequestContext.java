package com.carlos.boot.web.request;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.carlos.apm.TraceUtil;
import com.carlos.core.auth.UserContext;
import com.carlos.core.constant.CoreConstant;
import com.carlos.core.exception.BusinessException;
import com.carlos.core.exception.ComponentException;
import com.carlos.core.interfaces.ApplicationExtend;
import com.carlos.core.response.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Web 请求用户上下文工具类。
 * </p>
 * <p>
 * 通过静态 volatile 字段持有 {@link ApplicationExtend} 实例，
 * 由 {@code ApplicationAutoConfiguration#initWebContext()} 在启动时调用 {@link #init(ApplicationExtend)} 完成初始化。
 * </p>
 * <p>
 * 与旧版 {@code ExtendInfoUtil} 的区别：
 * <ul>
 *   <li>静态字段使用 {@code volatile} 修饰，保证多线程可见性</li>
 *   <li>初始化方法期望仅调用一次（从 {@code @PostConstruct} 中调用）</li>
 *   <li>Getter 方法在 extend 为 null 时抛出明确异常</li>
 * </ul>
 * </p>
 *
 * @author carlos
 * @date 2021/12/28 9:50
 */
public final class WebRequestContext {

    private static volatile ApplicationExtend extend;

    private static final ThreadLocal<RequestInfo> REQUEST_INFO_HOLDER = new TransmittableThreadLocal<>();

    /**
     * 需要脱敏的敏感请求头（日志/诊断中不输出原值）
     */
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
        "authorization",
        "cookie",
        "set-cookie",
        "x-api-key",
        "x-auth-token",
        "x-csrf-token",
        "proxy-authorization"
    );

    private WebRequestContext() {
        throw new AssertionError();
    }

    /**
     * 初始化静态字段，预期从 {@code @PostConstruct} 中调用一次。
     *
     * @param applicationExtend 应用扩展接口实现
     */
    public static void init(@Nullable ApplicationExtend applicationExtend) {
        WebRequestContext.extend = applicationExtend;
    }

    /**
     * 获取当前请求用户ID。
     *
     * @return 用户ID
     * @throws ComponentException 如果 ApplicationExtend 未配置
     */
    public static Serializable getUserId() {
        if (extend == null) {
            throw new ComponentException("ApplicationExtend not configured");
        }
        return extend.getUserId();
    }

    /**
     * 获取当前用户角色ID。
     *
     * @return 角色ID
     * @throws ComponentException 如果 ApplicationExtend 未配置
     * @throws BusinessException  如果角色ID为空
     */
    public static Serializable getRoleId() {
        if (extend == null) {
            throw new ComponentException("ApplicationExtend not configured");
        }
        UserContext context = extend.getUserContext();
        Serializable roleId = context.getRoleId();
        if (roleId == null) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        return roleId;
    }

    /**
     * 获取当前用户部门ID。
     *
     * @return 部门ID
     * @throws ComponentException 如果 ApplicationExtend 未配置
     * @throws BusinessException  如果部门ID为空
     */
    public static Serializable getDepartmentId() {
        if (extend == null) {
            throw new ComponentException("ApplicationExtend not configured");
        }
        UserContext context = extend.getUserContext();
        Serializable departmentId = context.getDepartmentId();
        if (departmentId == null) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        return departmentId;
    }

    /**
     * 获取当前用户上下文。
     *
     * @return UserContext 用户上下文
     * @throws ComponentException 如果 ApplicationExtend 未配置
     */
    public static UserContext getUserContext() {
        if (extend == null) {
            throw new ComponentException("ApplicationExtend not configured");
        }
        UserContext context = extend.getUserContext();
        if (context == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "用户登录信息为空");
        }
        return context;
    }

    // ==================== 请求信息 ====================

    /**
     * 获取当前请求元信息（缓存于 ThreadLocal）。
     *
     * @return RequestInfo
     */
    public static RequestInfo getRequestInfo() {
        RequestInfo info = REQUEST_INFO_HOLDER.get();
        if (info == null) {
            HttpServletRequest request = getCurrentRequest();
            info = new RequestInfo();
            LocalDateTime now = LocalDateTime.now();
            String requestId = TraceUtil.getTraceId();
            if (StrUtil.isBlank(requestId)) {
                requestId = DateUtil.format(now, DatePattern.PURE_DATETIME_MS_PATTERN) + RandomUtil.randomInt(3);
            }
            info.setRequestId(requestId);
            info.setContextPath(request.getContextPath());
            info.setRealPath(request.getServletPath());
            info.setUrl(request.getRequestURL().toString());
            info.setIp(IpUtils.getRequestIp(request));
            info.setMethod(request.getMethod());
            info.setContentType(request.getContentType());
            info.setTime(now);
            Map<String, String> headers = maskSensitiveHeaders(JakartaServletUtil.getHeaderMap(request));
            String rpc = headers.get(CoreConstant.HEADER_NAME_RPC);
            if (rpc != null) {
                info.setRpc(true);
            }
            info.setHeader(headers);
            REQUEST_INFO_HOLDER.set(info);
        }
        return info;
    }

    /**
     * 清除当前线程的请求信息。
     */
    public static void remove() {
        REQUEST_INFO_HOLDER.remove();
    }

    /**
     * 获取当前 HTTP 请求。
     */
    public static HttpServletRequest getCurrentRequest() {
        return ((ServletRequestAttributes)
            Objects.requireNonNull(RequestContextHolder.getRequestAttributes()))
            .getRequest();
    }

    /**
     * 刷新本地线程中的请求信息。
     */
    public static void refreshRequestInfo(RequestInfo requestInfo) {
        REQUEST_INFO_HOLDER.remove();
        REQUEST_INFO_HOLDER.set(requestInfo);
    }

    /**
     * 对敏感请求头进行脱敏处理。
     *
     * @param headers 原始请求头
     * @return 脱敏后的请求头副本
     */
    private static Map<String, String> maskSensitiveHeaders(Map<String, String> headers) {
        if (headers == null) {
            return null;
        }
        Map<String, String> masked = new HashMap<>(headers.size());
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String name = entry.getKey();
            if (name != null && SENSITIVE_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                masked.put(name, "***");
            } else {
                masked.put(name, entry.getValue());
            }
        }
        return masked;
    }
}
