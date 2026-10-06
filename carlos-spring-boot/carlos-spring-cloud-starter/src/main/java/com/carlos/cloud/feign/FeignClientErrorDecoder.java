package com.carlos.cloud.feign;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.carlos.core.response.CommonErrorCode;
import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * <p>
 * openfeign 客户端调用自定义错误解码器
 * </p>
 *
 * @author carlos
 * @date 2022/1/17 10:30
 */
@Slf4j
public class FeignClientErrorDecoder extends ErrorDecoder.Default {

    @Override
    public Exception decode(final String methodKey, final Response response) {
        final Exception exception = super.decode(methodKey, response);
        // 如果是请求重试的异常，返回继续重试
        if (exception instanceof RetryableException) {
            return exception;
        }

        // 如果是FeignException，则对其进行处理，并抛出BusinessException
        if (!(exception instanceof FeignException feignException)) {
            return exception;
        }
        final String s;
        try {
            final ByteBuffer responseBody = feignException.responseBody().get();
            s = StandardCharsets.UTF_8.newDecoder().decode(responseBody.asReadOnlyBuffer()).toString();
            // s = Util.toString(response.body().asReader());
        } catch (final IOException e) {
            log.error("异常内容读取失败:{}", e.getMessage(), e);
            return new OpenFeignException();
        }
        // 将异常信息，转换为ExceptionInfo对象
        final ExceptionInfo exceptionInfo;
        try {
            exceptionInfo = JSONUtil.toBean(s, ExceptionInfo.class);
            log.debug("Get exception:{}", exceptionInfo);
        } catch (final Exception e) {
            log.error("Can't convert  string [{}] to 'ExceptionInfo'", StrUtil.subWithLength(s, 0, 100));
            return exception;
        }
        final String message = resolveMessage(exceptionInfo);

        return Optional.ofNullable(exceptionInfo.getException()).map(name -> {
            try {
                final Class<?> clazz = Class.forName(name);
                return (Exception) clazz.getDeclaredConstructor(String.class).newInstance(message);
            } catch (final Exception e) {
                return new OpenFeignException();
            }
        }).orElseGet(() -> {
            // 如果 exception 中 code 不为空，则使用该 code，否则使用默认的错误 code
            final String code = Optional.ofNullable(exceptionInfo.getCode())
                .filter(StrUtil::isNotBlank)
                .orElse(CommonErrorCode.INTERNAL_ERROR.getCode());
            return new OpenFeignException(code, message);
        });
    }

    /**
     * 解析远端错误文案
     *
     * <p>必须认两种错误体的文案字段：框架统一响应 {@code Result} 用的是 <b>{@code msg}</b>
     * （carlos 服务间的错误响应全是这一种，见 {@code GlobalExceptionHandler}），
     * 而 Spring Boot 默认错误体用的是 {@code message}。</p>
     *
     * <p>⚠️ 历史上只读了 {@code message}，于是 carlos 服务间的业务异常<b>错误码保留、
     * 文案一律丢失</b>，统一退化成兜底文案「openfeign调用异常」—— 错误码说「规则不存在」、
     * 文案说「调用异常」，这是排查时最误导人的组合。改这里务必保留两种字段的回退顺序。</p>
     *
     * @param exceptionInfo 已反序列化的错误体
     * @return 错误文案，两种字段都为空时返回兜底文案
     */
    private String resolveMessage(ExceptionInfo exceptionInfo) {
        if (StrUtil.isNotBlank(exceptionInfo.getMsg())) {
            return exceptionInfo.getMsg();
        }
        if (StrUtil.isNotBlank(exceptionInfo.getMessage())) {
            return exceptionInfo.getMessage();
        }
        return "openfeign调用异常";
    }


}
