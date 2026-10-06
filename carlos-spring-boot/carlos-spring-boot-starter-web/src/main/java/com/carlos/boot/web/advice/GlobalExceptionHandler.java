package com.carlos.boot.web.advice;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import com.carlos.boot.web.request.RequestInfo;
import com.carlos.boot.web.request.WebRequestContext;
import com.carlos.core.exception.BusinessException;
import com.carlos.core.exception.DaoException;
import com.carlos.core.exception.GlobalException;
import com.carlos.core.exception.RestException;
import com.carlos.core.response.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

/**
 * 全局异常处理器。
 *
 * <p>统一处理各类异常，转换为标准 Result 响应格式。支持可选 RFC 9457 Problem Detail 格式。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Environment environment;

    // ==================== 参数校验异常 ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpServletRequest request) {

        List<FieldErrorDetail> details = exception.getBindingResult().getFieldErrors().stream()
                .map(this::convertFieldError)
                .toList();

        log.warn("[参数校验失败] {} - {}", request.getRequestURI(), details);
        printRequestDetail();

        Result<Void> response = Result.validationError(details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(
            BindException exception, HttpServletRequest request) {

        List<FieldErrorDetail> details = exception.getFieldErrors().stream()
                .map(this::convertFieldError)
                .toList();

        String firstError = details.isEmpty() ? "参数绑定失败" : details.getFirst().getMessage();
        log.warn("[参数绑定失败] {} - {}", request.getRequestURI(), firstError);
        printRequestDetail();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.validationError(details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {

        List<FieldErrorDetail> details = exception.getConstraintViolations().stream()
                .map(this::convertViolation)
                .toList();

        String firstError = details.isEmpty() ? "参数约束违反" : details.getFirst().getMessage();
        log.warn("[参数约束违反] {} - {}", request.getRequestURI(), firstError);
        printRequestDetail();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.validationError(details));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Result<Void>> handleHandlerMethodValidation(
            HandlerMethodValidationException exception, HttpServletRequest request) {

        List<FieldErrorDetail> details = convertParameterValidationResults(
                exception.getParameterValidationResults());

        log.warn("[方法参数校验失败] {} - {}", request.getRequestURI(), details);
        printRequestDetail();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.validationError(details));
    }

    @ExceptionHandler(MethodValidationException.class)
    public ResponseEntity<Result<Void>> handleMethodValidation(
            MethodValidationException exception, HttpServletRequest request) {

        List<FieldErrorDetail> details = convertParameterValidationResults(
                exception.getParameterValidationResults());

        log.warn("[方法级校验失败] {} - {}", request.getRequestURI(), details);
        printRequestDetail();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.validationError(details));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(
            MissingServletRequestParameterException exception, HttpServletRequest request) {

        String message = "缺少必要参数: %s".formatted(exception.getParameterName());
        log.warn("[缺少参数] {} - {}", request.getRequestURI(), message);
        printRequestDetail();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(CommonErrorCode.PARAM_MISSING, message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {

        String message = "参数类型错误: %s 期望类型 %s".formatted(
                exception.getName(),
                exception.getRequiredType() != null ? exception.getRequiredType().getSimpleName() : "未知");
        log.warn("[参数类型错误] {} - {}", request.getRequestURI(), message);
        printRequestDetail();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(CommonErrorCode.PARAM_TYPE_ERROR, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleMessageNotReadable(
            HttpMessageNotReadableException exception, HttpServletRequest request) {

        log.warn("[请求体解析失败] {} - {}", request.getRequestURI(), exception.getMessage());
        printRequestDetail();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(CommonErrorCode.PARAM_FORMAT_ERROR, "请求参数格式错误"));
    }

    // ==================== 业务异常 ====================

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<Result<Void>> handleGlobalException(
            GlobalException exception, HttpServletRequest request) {

        printRequestDetail();
        ErrorCode errorCode = resolveErrorCode(exception);

        log.error("[全局异常] {} - type={}, code={}, httpStatus={}, message={}",
                request.getRequestURI(), exception.getClass().getSimpleName(),
                errorCode.getCode(), exception.getHttpStatus(), exception.getMessage(), exception);

        return ResponseEntity.status(exception.getHttpStatus())
                .body(Result.error(errorCode, exception.getMessage()));
    }

    // ==================== HTTP 异常 ====================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {

        log.warn("[请求方法不支持] {} {}", exception.getMethod(), request.getRequestURI());
        printRequestDetail();

        String message = "不支持的请求方法: %s".formatted(exception.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.error(CommonErrorCode.METHOD_NOT_ALLOWED, message));
    }

    @ExceptionHandler(HttpMediaTypeException.class)
    public ResponseEntity<Result<Void>> handleMediaType(
            HttpMediaTypeException exception, HttpServletRequest request) {

        log.warn("[媒体类型错误] {} - {}", request.getRequestURI(), exception.getMessage());
        printRequestDetail();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(CommonErrorCode.BAD_REQUEST, "Content-Type 不支持"));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Void>> handleNoHandlerFound(
            NoHandlerFoundException exception, HttpServletRequest request) {

        log.warn("[资源不存在] {} {}", exception.getHttpMethod(), exception.getRequestURL());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.error(CommonErrorCode.NOT_FOUND));
    }

    // ==================== 兜底 ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(
            Exception exception, HttpServletRequest request) {

        log.error("[未预期异常] {} - {}", request.getRequestURI(), exception.getMessage(), exception);
        printRequestDetail();

        String message = isProduction() ? "系统繁忙，请稍后重试" : exception.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(CommonErrorCode.INTERNAL_ERROR, message));
    }

    // ==================== 工具方法 ====================

    private ErrorCode resolveErrorCode(GlobalException exception) {
        String exceptionCode = exception.getErrorCode();
        if (StrUtil.isNotBlank(exceptionCode)) {
            return new SimpleErrorCode(exceptionCode, exception.getMessage(), exception.getHttpStatus());
        }
        return switch (exception) {
            case BusinessException b -> CommonErrorCode.BUSINESS_ERROR;
            case DaoException d -> CommonErrorCode.DATABASE_ERROR;
            case RestException r -> CommonErrorCode.BAD_REQUEST;
            default -> CommonErrorCode.INTERNAL_ERROR;
        };
    }

    private FieldErrorDetail convertFieldError(FieldError fieldError) {
        return FieldErrorDetail.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .rejectedValue(fieldError.getRejectedValue())
                .build();
    }

    private FieldErrorDetail convertViolation(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath().toString();
        if (field.contains(".")) {
            field = field.substring(field.lastIndexOf('.') + 1);
        }
        return FieldErrorDetail.builder()
                .field(field)
                .message(violation.getMessage())
                .rejectedValue(violation.getInvalidValue())
                .build();
    }

    private List<FieldErrorDetail> convertParameterValidationResults(
            List<ParameterValidationResult> results) {
        return results.stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> FieldErrorDetail.builder()
                                .field(result.getMethodParameter().getParameterName())
                                .message(error.getDefaultMessage())
                                .rejectedValue(result.getArgument())
                                .build()))
                .toList();
    }

    private void printRequestDetail() {
        try {
            RequestInfo requestInfo = WebRequestContext.getRequestInfo();
            log.error("异常来源：{}",
                    JSONUtil.toJsonStr(requestInfo, JSONConfig.create().setIgnoreNullValue(true).setIgnoreError(true)));
        } catch (Exception e) {
            log.error("打印请求详情失败", e);
        }
    }

    /**
     * 获取当前 HTTP 请求（便捷方法）。
     */
    private static HttpServletRequest getCurrentRequest() {
        return WebRequestContext.getCurrentRequest();
    }

    private boolean isProduction() {
        return environment.acceptsProfiles(Profiles.of("prod", "production"));
    }
}
