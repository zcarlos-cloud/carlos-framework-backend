package com.carlos.security.config;

import com.carlos.core.response.CommonErrorCode;
import com.carlos.core.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <p>
 * 方法级鉴权拒绝通知
 * </p>
 *
 * <p>{@code @PreAuthorize} 抛出的 {@link AccessDeniedException} 产生于过滤器链之后
 * （DispatcherServlet 内），过滤器层的 {@link ForbiddenAccessDeniedHandler} 收不到，
 * 会被全局兜底当成「未预期异常」返回 500。这里将其归位为 403。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@RestControllerAdvice
public class AccessDeniedExceptionAdvice {

    /**
     * Boot 4 / Spring Security 7 的方法级拒绝并行新类型，显式声明保证最具体匹配，
     * 不被全局兜底的 50001 抢占（方法级拒绝应返回 20203）
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Result<Void>> handleAuthorizationDenied(AuthorizationDeniedException exception,
                                                                  HttpServletRequest request) {
        log.warn("[访问被拒绝] {} - {}", request.getRequestURI(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Result.error(CommonErrorCode.AUTH_ACCESS_DENIED));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException exception,
                                                           HttpServletRequest request) {
        log.warn("[访问被拒绝] {} - {}", request.getRequestURI(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.error(CommonErrorCode.AUTH_ACCESS_DENIED));
    }
}
