package com.carlos.cloud.feign;


import lombok.Data;

/**
 * <p>
 * openfeign 异常信息
 * </p>
 *
 * <p>本类要同时承接<b>两种</b>错误响应体：</p>
 * <ol>
 *   <li><b>框架统一响应 {@code Result}</b>（carlos 服务间实际会遇到的全部情形）：
 *       {@code {"success":false,"code":"20981","msg":"编码规则不存在","timestamp":...}} ——
 *       错误码在 {@code code}、<b>文案在 {@code msg}</b>；</li>
 *   <li><b>Spring Boot 默认错误体</b>（网关/非 carlos 服务透传时可能遇到）：
 *       {@code {"timestamp":...,"status":404,"error":"Not Found","exception":"...","message":"...","path":"..."}}
 *       —— 文案在 {@code message}，且带 {@code exception} 类名可供反序列化还原。</li>
 * </ol>
 *
 * <p>⚠️ 因此 {@code msg} 与 {@code message} 都必须映射。历史上只声明了 {@code message}，
 * 而 carlos 服务的错误体从来只有 {@code msg}，导致<b>错误码保留、文案全部丢失</b>
 * （退化成「openfeign调用异常」）。取值顺序见
 * {@code FeignClientErrorDecoder#resolveMessage}。</p>
 *
 * @author carlos
 * @date 2022/4/11 11:10
 */
@Data
public class ExceptionInfo {

    private Long timestamp;

    private Integer status;

    private String exception;

    /**
     * Spring Boot 默认错误体的文案字段
     */
    private String message;

    /**
     * 框架统一响应 {@code Result} 的文案字段（carlos 服务间的实际形态）
     */
    private String msg;

    private String path;

    private String code;

    private String error;
}
