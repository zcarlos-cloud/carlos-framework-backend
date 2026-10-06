package com.carlos.boot.web.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * <p>
 * HTTP 请求元数据（用于日志、诊断、链路追踪）。
 * </p>
 * <p>
 * 用户身份信息已迁移至 {@code com.carlos.core.auth.UserContext}，
 * 通过 {@code com.carlos.boot.web.request.WebRequestContext} 获取。
 * </p>
 *
 * @author carlos
 * @date 2020/4/24 13:26
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RequestInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * context-path
     */
    private String contextPath;

    /**
     * 请求实际路径 去除context-path /foobar/add
     */
    private String realPath;

    /**
     * 请求IP地址
     */
    private String ip;

    /**
     * 请求地址
     */
    private String url;

    /**
     * 是否是feign/rpc请求
     */
    private boolean rpc;

    /**
     * 请求方式，GET/POST
     */
    private String method;

    /**
     * 请求内容类型
     */
    private String contentType;

    /**
     * 请求参数
     */
    private Object param;

    /**
     * 请求时间
     */
    @JsonIgnore
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime time;

    /**
     * 请求头信息
     */
    @JsonIgnore
    @ToString.Exclude
    private Map<String, String> header;

}
