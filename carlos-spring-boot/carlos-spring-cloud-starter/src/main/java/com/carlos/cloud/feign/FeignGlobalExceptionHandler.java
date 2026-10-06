package com.carlos.cloud.feign;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.core.response.Result;
import com.carlos.core.response.SimpleErrorCode;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;


/**
 * <p>
 * Feign 全局异常处理
 * </p>
 *
 * <p>
 * 统一处理 Feign 调用相关的异常，包括：
 * <ul>
 *   <li>{@link FeignException} - Feign 客户端原始异常（如 ErrorDecoder 解码失败时保留的原始异常）</li>
 *   <li>{@link OpenFeignException} - Feign 错误解码器转换后的自定义异常</li>
 * </ul>
 * </p>
 *
 * @author carlos
 * @date 2022/4/11 11:35
 */
@Slf4j
@RestControllerAdvice
public class FeignGlobalExceptionHandler {

    /**
     * 处理 Feign 客户端原始异常
     * <p>
     * 注意：大多数 Feign 异常会经过 {@link FeignClientErrorDecoder} 转换为 {@link OpenFeignException}。
     * 此方法主要用于处理 ErrorDecoder 解码失败时保留的原始 FeignException，
     * 或某些未经 ErrorDecoder 处理的特殊场景。
     * </p>
     *
     * @param exception FeignException
     * @return 统一错误响应
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Result<Void>> handleFeignException(FeignException exception) {
        int status = exception.status();
        log.error("[Feign调用异常] status={}, methodKey={}, message={}",
            status, exception.request() != null ? exception.request().httpMethod() : "unknown",
            exception.getMessage());

        HttpStatus httpStatus;
        String message;

        if (status <= 0) {
            // 网络连接失败、超时等非 HTTP 错误（status 为 -1 或 0）
            httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
            message = "服务调用失败，请稍后重试";
        } else {
            httpStatus = resolveHttpStatus(status);
            message = extractMessage(exception);
            if (StrUtil.isBlank(message)) {
                message = resolveDefaultMessage(status);
            }
        }

        Result<Void> result = Result.error(CommonErrorCode.THIRD_PARTY_ERROR, message);
        return ResponseEntity.status(httpStatus).body(result);
    }

    /**
     * 处理 OpenFeignException（Feign 错误解码器转换后的异常）
     *
     * @param exception OpenFeignException
     * @return 统一错误响应
     */
    @ExceptionHandler(OpenFeignException.class)
    public ResponseEntity<Result<Void>> handleOpenFeignException(OpenFeignException exception) {
        log.error("[Feign服务异常] code={}, message={}", exception.getErrorCode(), exception.getMessage());

        // 优先使用异常自身携带的错误码和消息
        String errorCode = exception.getErrorCode();
        String message = exception.getMessage();
        int httpStatus = exception.getHttpStatus();

        Result<Void> result;
        if (StrUtil.isNotBlank(errorCode)) {
            result = Result.error(new SimpleErrorCode(errorCode, message, httpStatus), message);
        } else {
            result = Result.error(CommonErrorCode.THIRD_PARTY_ERROR, message);
        }

        return ResponseEntity.status(resolveHttpStatus(httpStatus)).body(result);
    }

    /**
     * 从 FeignException 响应体中提取错误消息
     */
    private String extractMessage(FeignException exception) {
        try {
            Optional<ByteBuffer> responseBody = exception.responseBody();
            if (responseBody.isEmpty()) {
                return null;
            }
            String content = StandardCharsets.UTF_8.newDecoder()
                .decode(responseBody.get().asReadOnlyBuffer()).toString();
            if (StrUtil.isBlank(content)) {
                return null;
            }
            // 尝试解析为统一响应格式 Result
            Result<?> result = JSONUtil.toBean(content, Result.class);
            if (result != null && Boolean.FALSE.equals(result.getSuccess()) && StrUtil.isNotBlank(result.getMsg())) {
                return result.getMsg();
            }
        } catch (Exception e) {
            log.debug("无法从 Feign 响应体中解析错误消息: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 根据 HTTP 状态码返回默认错误消息
     */
    private String resolveDefaultMessage(int status) {
        return switch (status) {
            case 400 -> "服务请求参数错误";
            case 401 -> "服务认证失败";
            case 403 -> "服务访问被拒绝";
            case 404 -> "服务资源不存在";
            case 408 -> "服务请求超时";
            case 429 -> "服务请求过于频繁";
            case 500 -> "服务内部错误";
            case 502 -> "服务网关错误";
            case 503 -> "服务暂不可用";
            case 504 -> "服务网关超时";
            default -> "第三方服务异常";
        };
    }

    /**
     * 将 HTTP 状态码转换为 Spring HttpStatus，无效时返回 500
     */
    private HttpStatus resolveHttpStatus(int status) {
        try {
            return HttpStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

}
