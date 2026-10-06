package com.carlos.gateway.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Map;

/**
 * <p>
 * 网关错误响应对象
 * 统一封装网关层面的错误响应信息
 * </p>
 *
 * @author carlos
 * @date 2026/3/24
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * 是否成功（始终为 false）
     */
    private boolean success;

    /**
     * HTTP 状态码
     */
    private int status;

    /**
     * 业务错误码
     */
    private String code;

    /**
     * 错误消息
     */
    private String msg;

    /**
     * 错误详情（开发环境可展示）
     */
    private String detail;

    /**
     * 异常堆栈（仅开发环境展示）
     */
    private String stack;

    /**
     * 请求路径
     */
    private String path;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 请求 ID（链路追踪）
     */
    private String traceId;

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 扩展字段（特定异常类型的额外信息）
     */
    private Map<String, Object> extra;

    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder()
            .success(false)
            .timestamp(System.currentTimeMillis());
    }

    public static class ErrorResponseBuilder {
        private boolean success;
        private int status;
        private String code;
        private String msg;
        private String detail;
        private String stack;
        private String path;
        private String method;
        private String traceId;
        private long timestamp;
        private Map<String, Object> extra;

        public ErrorResponseBuilder success(boolean success) {
            this.success = success;
            return this;
        }

        public ErrorResponseBuilder status(int status) {
            this.status = status;
            return this;
        }

        public ErrorResponseBuilder code(String code) {
            this.code = code;
            return this;
        }

        public ErrorResponseBuilder msg(String msg) {
            this.msg = msg;
            return this;
        }

        public ErrorResponseBuilder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public ErrorResponseBuilder stack(String stack) {
            this.stack = stack;
            return this;
        }

        public ErrorResponseBuilder path(String path) {
            this.path = path;
            return this;
        }

        public ErrorResponseBuilder method(String method) {
            this.method = method;
            return this;
        }

        public ErrorResponseBuilder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public ErrorResponseBuilder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ErrorResponseBuilder extra(Map<String, Object> extra) {
            this.extra = extra;
            return this;
        }

        public ErrorResponse build() {
            ErrorResponse response = new ErrorResponse();
            response.setSuccess(success);
            response.setStatus(status);
            response.setCode(code);
            response.setMsg(msg);
            response.setDetail(detail);
            response.setStack(stack);
            response.setPath(path);
            response.setMethod(method);
            response.setTraceId(traceId);
            response.setTimestamp(timestamp);
            response.setExtra(extra);
            return response;
        }
    }
}
