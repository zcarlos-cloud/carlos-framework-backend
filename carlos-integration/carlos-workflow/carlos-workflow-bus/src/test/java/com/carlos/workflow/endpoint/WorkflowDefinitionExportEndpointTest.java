package com.carlos.workflow.endpoint;

import com.carlos.boot.web.advice.ResponseWrapAdvice;
import com.carlos.boot.web.config.ResponseWrapProperties;
import com.carlos.core.response.Result;
import com.carlos.workflow.controller.WorkflowDefinitionController;
import com.carlos.workflow.dto.WorkflowDefinitionExportDTO;
import com.carlos.workflow.service.WorkflowDefinitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <p>
 * 流程定义导出端点的<b>响应包装排除</b>验证：下载响应必须是引擎 JSON 原文，
 * <b>不能</b>被 {@link ResponseWrapAdvice} 包成 Carlos 的 {@code Result} 信封。
 * </p>
 *
 * <h2>被测的真实故障</h2>
 * <p>
 * {@code ResponseWrapAdvice#beforeBodyWrite} 对<b>任何</b>非 {@code Result} 的返回值都会
 * {@code return Result.success(body)}。导出接口返回的是 {@code ResponseEntity<byte[]>}，
 * 漏掉 {@code @NoWrap} 时序列化结果会变成
 * {@code {"success":true,"code":"00000",...,"data":"{...流程定义JSON...}"}}——
 * <b>请求成功、状态 200、文件名也对</b>，只有下载下来的文件打不开。因此这里的断言把
 * 「顶层没有 {@code code} / {@code success}」放在最前面：漏注解时它的失败信息最直白
 * （{@code Expected no value at JSON path "$.code"}）。
 * </p>
 *
 * <h2>为什么用 standaloneSetup + 真实的 ResponseWrapAdvice</h2>
 * <p>
 * 本测试只关心「advice 对这一组映射的判定」，不需要数据库，也不该为了它去动 warm-flow 那个
 * 一个 JVM 只能有一份的 Spring 上下文（见 {@code WorkflowContextStartupTest} 的说明）。
 * {@code standaloneSetup} 会把 {@code setControllerAdvice(...)} 传进来的 advice 注册进
 * StubWebApplicationContext，走的是真实的 {@code RequestMappingHandlerAdapter} 与
 * {@code ControllerAdviceBean} 收集流程，因此包装行为与生产一致。
 * </p>
 * <p>
 * {@link #probeEndpointsAreStillWrapped()} 是这套断言的反事实对照：同一次 MockMvc 里注册的
 * 探针端点返回的是普通 {@code Map}，它<b>必须</b>被包成信封。没有这一条，万一 advice 根本没生效，
 * 「导出没被包装」会因为「压根没有包装器」而假通过。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowDefinitionExportEndpointTest {

    /**
     * {@code ResponseWrapAdvice} 不读转换器类型，任意实现即可满足签名
     */
    private static final Class<? extends HttpMessageConverter<?>> ANY_CONVERTER = StringHttpMessageConverter.class;

    /**
     * 引擎导出的 JSON：顶层只有流程定义自己的字段
     */
    private static final String ENGINE_JSON = """
        {"flowCode":"leave","flowName":"请假流程","version":"3"}""";

    private final WorkflowDefinitionService workflowDefinitionService = mock(WorkflowDefinitionService.class);

    private final ResponseWrapAdvice advice = new ResponseWrapAdvice(new ResponseWrapProperties());

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new WorkflowDefinitionController(workflowDefinitionService), new ProbeController())
            .setControllerAdvice(advice)
            .build();
    }

    // ==================== 断言 1：导出不被包装 ====================

    @Test
    void exportEndpointDownloadsTheRawEngineJsonWithTheAgreedFileName() throws Exception {
        when(workflowDefinitionService.exportDefinition(7L)).thenReturn(export());

        mockMvc.perform(get("/workflow/definition/{id}/export", 7L))
            .andExpect(status().isOk())
            // 先钉「没有被再包一层」：Carlos 信封的字段一个都不该出现。
            // 放最前面是因为被包装时它的失败信息最直白。
            .andExpect(jsonPath("$.code").doesNotExist())
            .andExpect(jsonPath("$.success").doesNotExist())
            .andExpect(jsonPath("$.timestamp").doesNotExist())
            // 顶层直接就是引擎的流程定义 JSON
            .andExpect(jsonPath("$.flowCode").value("leave"))
            .andExpect(jsonPath("$.flowName").value("请假流程"))
            .andExpect(jsonPath("$.version").value("3"))
            // 下载文件名：流程编码_版本.json
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"leave_3.json\""));
    }

    @Test
    void exportEndpointWritesTheExactEngineJsonBytes() throws Exception {
        when(workflowDefinitionService.exportDefinition(7L)).thenReturn(export());

        String body = mockMvc.perform(get("/workflow/definition/{id}/export", 7L))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(body)
            .as("导出的必须是引擎 JSON 原文，一个字节都不能多（被包装时会多出整个信封）")
            .isEqualTo(ENGINE_JSON);
    }

    // ==================== 断言 2：advice 真的生效，且只对 @NoWrap 的方法退让 ====================

    @Test
    void probeEndpointsAreStillWrapped() throws Exception {
        mockMvc.perform(get("/workflow/probe/payload"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.probe").value(true));
    }

    @Test
    void adviceSkipsOnlyTheNoWrapAnnotatedExportMethod() throws Exception {
        assertThat(advice.supports(exportMethodParameter(), ANY_CONVERTER))
            .as("导出方法带 @NoWrap，advice 必须在 supports() 阶段就跳过它")
            .isFalse();
        assertThat(advice.supports(pageMethodParameter(), ANY_CONVERTER))
            .as("同一个控制器上的普通方法仍必须被包装——排除是方法级的，不能把整个控制器关掉")
            .isTrue();

        WorkflowDefinitionExportDTO payload = export();
        Object wrapped = advice.beforeBodyWrite(payload, pageMethodParameter(), MediaType.APPLICATION_JSON,
            ANY_CONVERTER, request("/workflow/definition/page"),
            new ServletServerHttpResponse(new MockHttpServletResponse()));

        assertThat(wrapped)
            .as("反事实对照：没有 @NoWrap 的方法，同样的非 Result 返回值确实会被包成 Carlos 信封")
            .isInstanceOf(Result.class);
        assertThat(((Result<?>) wrapped).getCode()).isEqualTo("00000");
        assertThat(((Result<?>) wrapped).getData()).isSameAs(payload);
    }

    // ==================== 夹具 ====================

    private static WorkflowDefinitionExportDTO export() {
        WorkflowDefinitionExportDTO export = new WorkflowDefinitionExportDTO();
        export.setDefinitionId(7L);
        export.setFlowCode("leave");
        export.setVersion("3");
        export.setContent(ENGINE_JSON);
        return export;
    }

    private static MethodParameter exportMethodParameter() throws NoSuchMethodException {
        Method method = WorkflowDefinitionController.class.getDeclaredMethod("exportDefinition", Long.class);
        return new MethodParameter(method, -1);
    }

    private static MethodParameter pageMethodParameter() throws NoSuchMethodException {
        Method method = WorkflowDefinitionController.class.getDeclaredMethod("getById", Long.class);
        return new MethodParameter(method, -1);
    }

    private static ServerHttpRequest request(String path) {
        return new ServletServerHttpRequest(new MockHttpServletRequest("GET", path));
    }

    /**
     * 探针控制器：只提供「返回普通对象」的端点，用来证明本次 MockMvc 里 advice 确实在包装。
     * <p>
     * 刻意放在 {@code com.carlos.workflow.endpoint} 而不是 {@code com.carlos.workflow.controller}：
     * 后者被 {@code WorkflowNamespaceBoundaryTest} 整包扫描，塞一个只为测试存在的控制器进去会污染那份边界清单。
     * </p>
     */
    @RestController
    @RequestMapping("/workflow/probe")
    static class ProbeController {

        @GetMapping("/payload")
        Map<String, Object> payload() {
            return Map.of("probe", true);
        }

    }

}
