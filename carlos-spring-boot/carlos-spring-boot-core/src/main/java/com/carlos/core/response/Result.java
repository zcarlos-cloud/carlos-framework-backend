package com.carlos.core.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 统一 API 响应对象
 * <p>
 * 标准响应结构：
 * <pre>
 * {
 *   "success": true,
 *   "code": "00000",
 *   "msg": "操作成功",
 *   "data": { ... },
 *   "timestamp": 1710638258000,
 *   "details": null
 * }
 * </pre>
 *
 * @param <T> 响应数据类型
 * @author carlos
 * @since 3.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 成功标志
     */
    private Boolean success;

    /**
     * 错误码（5位数字字符串）
     */
    private String code;

    /**
     * 响应消息
     */
    private String msg;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应时间戳（毫秒）
     */
    private Long timestamp;

    /**
     * 字段级错误详情（仅校验错误时返回）
     */
    private List<FieldErrorDetail> details;

    // ==================== 成功响应工厂方法 ====================

    /**
     * 创建成功响应（无数据）
     *
     * @param <T> 数据类型
     * @return Result
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 创建成功响应（带数据）
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return Result
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
            .success(true)
            .code(CommonErrorCode.SUCCESS.getCode())
            .msg(CommonErrorCode.SUCCESS.getMessage())
            .data(data)
            .timestamp(currentTimestamp())
            .build();
    }

    /**
     * 创建成功响应（带数据和自定义消息）
     *
     * @param data    响应数据
     * @param message 自定义消息
     * @param <T>     数据类型
     * @return Result
     */
    public static <T> Result<T> success(T data, String message) {
        return Result.<T>builder()
            .success(true)
            .code(CommonErrorCode.SUCCESS.getCode())
            .msg(message)
            .data(data)
            .timestamp(currentTimestamp())
            .build();
    }

    /**
     * 创建成功响应（返回单个键值对）
     *
     * @param key   键
     * @param value 值
     * @return Result
     */
    public static Result<Map<String, Object>> success(String key, Object value) {
        Map<String, Object> map = new HashMap<>(2);
        map.put(key, value);
        return success(map);
    }

    // ==================== 失败响应工厂方法 ====================

    /**
     * 创建失败响应（使用错误码默认消息）
     *
     * @param errorCode 错误码
     * @param <T>       数据类型
     * @return Result
     */
    public static <T> Result<T> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.getMessage(), null);
    }

    /**
     * 创建失败响应（使用错误码默认消息）
     *
     * @param message 自定义消息
     * @param <T>     数据类型
     * @return Result
     */
    public static <T> Result<T> error(String message) {
        return error(CommonErrorCode.INTERNAL_ERROR, message, null);
    }

    /**
     * 创建失败响应（使用默认错误码）
     *
     * @param <T> 数据类型
     * @return Result
     */
    public static <T> Result<T> error() {
        return error(CommonErrorCode.INTERNAL_ERROR);
    }

    /**
     * 创建失败响应（自定义消息）
     *
     * @param errorCode 错误码
     * @param message   自定义消息
     * @param <T>       数据类型
     * @return Result
     */
    public static <T> Result<T> error(ErrorCode errorCode, String message) {
        return error(errorCode, message, null);
    }

    /**
     * 创建失败响应（带字段错误详情）
     *
     * @param errorCode 错误码
     * @param message   自定义消息
     * @param details   字段错误详情
     * @param <T>       数据类型
     * @return Result
     */
    public static <T> Result<T> error(ErrorCode errorCode, String message, List<FieldErrorDetail> details) {
        return Result.<T>builder()
            .success(false)
            .code(errorCode.getCode())
            .msg(message != null ? message : errorCode.getMessage())
            .details(details)
            .timestamp(currentTimestamp())
            .build();
    }

    /**
     * 创建参数校验错误响应
     *
     * @param details 字段错误详情列表
     * @param <T>     数据类型
     * @return Result
     */
    public static <T> Result<T> validationError(List<FieldErrorDetail> details) {
        return error(CommonErrorCode.PARAM_VALIDATION_ERROR, "参数校验失败", details);
    }

    /**
     * 创建参数校验错误响应（单字段）
     *
     * @param field    字段名
     * @param message  错误消息
     * @param rejectedValue 被拒绝的值
     * @param <T>      数据类型
     * @return Result
     */
    public static <T> Result<T> validationError(String field, String message, Object rejectedValue) {
        FieldErrorDetail detail = FieldErrorDetail.builder()
            .field(field)
            .message(message)
            .rejectedValue(rejectedValue)
            .build();
        return error(CommonErrorCode.PARAM_VALIDATION_ERROR, "参数校验失败", List.of(detail));
    }

    // ==================== 便捷方法 ====================

    /**
     * 判断响应是否成功
     *
     * @return true 表示成功
     */
    public boolean isSuccess() {
        return success != null && success;
    }

    /**
     * 获取数据（如果是失败返回空Optional）
     *
     * @return Optional 包装的数据
     */
    public Optional<T> dataOpt() {
        if (!isSuccess()) {
            return Optional.empty();
        }
        return Optional.ofNullable(data);
    }

    /**
     * 获取当前时间戳
     *
     * @return 毫秒时间戳
     */
    private static long currentTimestamp() {
        return Instant.now().toEpochMilli();
    }
}
