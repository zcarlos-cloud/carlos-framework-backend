package com.carlos.cloud.feign;

import feign.Request;
import feign.Request.HttpMethod;
import feign.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FeignClientErrorDecoder 单元测试
 *
 * <p>核心不变量：<b>远端错误文案必须被取到</b>。框架统一响应 {@code Result} 的文案字段是
 * {@code msg}，而 Spring Boot 默认错误体的是 {@code message}；只认其中一个，就会让另一类
 * 响应体「错误码保留、文案丢失」，退化成兜底文案 —— 这类缺陷不会报错、只是排查时误导人，
 * 所以在此钉死。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@DisplayName("FeignClientErrorDecoder")
class FeignClientErrorDecoderTest {

    /**
     * 运行中的 carlos-system 在规则不存在时的<b>真实响应体</b>（实测抓取）
     */
    private static final String RESULT_ERROR_BODY =
        "{\"success\":false,\"code\":\"20981\",\"msg\":\"编码规则不存在\",\"timestamp\":\"1790499713795\"}";

    /**
     * Spring Boot 默认错误体（网关或非 carlos 服务透传时可能遇到）
     */
    private static final String SPRING_ERROR_BODY =
        "{\"timestamp\":\"2026-09-27T10:00:00.000+00:00\",\"status\":404,\"error\":\"Not Found\","
            + "\"message\":\"No endpoint GET /api/x\",\"path\":\"/api/x\"}";

    private static Exception decode(int status, String body) {
        Response response = Response.builder()
            .status(status)
            .reason("error")
            .headers(Map.of("Content-Type", List.of("application/json")))
            .request(Request.create(HttpMethod.GET, "http://localhost:9513/api/sys/sequence/next",
                Map.of(), new byte[0], StandardCharsets.UTF_8, null))
            .body(body, StandardCharsets.UTF_8)
            .build();

        return new FeignClientErrorDecoder().decode("ApiSequence#next(String)", response);
    }

    @Test
    @DisplayName("Result 错误体的 msg 必须被取到，而不是退化成兜底文案")
    void should_take_msg_from_result_body() {
        Exception exception = decode(404, RESULT_ERROR_BODY);

        assertThat(exception).isInstanceOfSatisfying(OpenFeignException.class, e -> {
            assertThat(e.getErrorCode())
                .as("错误码原本就能取到，这条防的是回归")
                .isEqualTo("20981");
            assertThat(e.getMessage())
                .as("文案是本次修复的目标：曾经一律退化成「openfeign调用异常」")
                .isEqualTo("编码规则不存在");
        });
    }

    @Test
    @DisplayName("Spring 默认错误体的 message 仍要能取到（修复 msg 不能破坏旧格式）")
    void should_still_take_message_from_spring_default_body() {
        Exception exception = decode(404, SPRING_ERROR_BODY);

        assertThat(exception).isInstanceOfSatisfying(OpenFeignException.class,
            e -> assertThat(e.getMessage()).isEqualTo("No endpoint GET /api/x"));
    }

    @Test
    @DisplayName("两种文案字段都缺失时退回兜底文案与默认错误码")
    void should_fall_back_when_no_message_field_present() {
        Exception exception = decode(500, "{\"code\":\"\"}");

        assertThat(exception).isInstanceOfSatisfying(OpenFeignException.class, e -> {
            assertThat(e.getMessage()).isEqualTo("openfeign调用异常");
            assertThat(e.getErrorCode()).isNotBlank();
        });
    }

    @Test
    @DisplayName("响应体不是 JSON 时退回兜底文案的 OpenFeignException（原始异常被替换，非保留）")
    void should_fall_back_for_non_json_body() {
        Exception exception = decode(502, "<html><body>Bad Gateway</body></html>");

        // ⚠️ 这里刻意记录**现状**，不是理想行为：Hutool 的 JSONUtil.toBean 对非 JSON 文本
        // 不抛异常、只返回字段全 null 的对象，于是 decode 里那条
        // `catch (Exception e) { return exception; }`（日志文案 "Can't convert ..."）**不可达**，
        // 原始 FeignException 被替换成兜底异常。
        // 连带后果：FeignGlobalExceptionHandler 本可按状态码给出更准确的文案
        // （502 → 「服务网关错误」），一旦被包成 OpenFeignException 就用不上了。
        // 详见 FeignClientErrorDecoder#resolveMessage 的说明；要改需连同该 handler 一起动。
        assertThat(exception).isInstanceOfSatisfying(OpenFeignException.class,
            e -> assertThat(e.getMessage()).isEqualTo("openfeign调用异常"));
    }
}
