package com.carlos.workflow.advice;

import com.carlos.boot.web.advice.ResponseWrapAdvice;
import com.carlos.boot.web.config.ResponseWrapProperties;
import com.carlos.boot.web.config.WebProperties;
import com.carlos.core.response.Result;
import com.carlos.workflow.api.pojo.ao.WorkflowDefinitionAO;
import com.carlos.workflow.api.pojo.param.ApiWorkflowDefinitionPageParam;
import com.carlos.workflow.controller.WorkflowDefinitionController;
import com.carlos.workflow.poc.WorkflowContextStartupTest;
import org.dromara.warm.flow.core.dto.ApiResult;
import org.dromara.warm.flow.ui.controller.WarmFlowController;
import org.dromara.warm.flow.ui.controller.WarmFlowUiController;
import org.dromara.warm.flow.ui.vo.WarmFlowVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
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
import org.springframework.web.context.WebApplicationContext;
import org.yaml.snakeyaml.Yaml;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <p>
 * 统一响应包装的「外科手术式排除」验证：{@code carlos.web.response-wrap.exclude-packages} 与
 * {@code exclude-paths} 必须让 Warm-Flow 设计器的接口保持引擎自己的 {@code ApiResult}
 * （顶层 {@code code = 200}），同时<strong>不</strong>影响 Carlos 自己的业务接口
 * （顶层 {@code code = "00000"} 的信封必须原样保留）。
 * </p>
 *
 * <h2>被测的真实故障</h2>
 * <p>
 * 设计器（jar 自带的第三方 SPA）的 axios 拦截器只看响应<strong>顶层</strong>的 {@code code}，
 * 期望引擎的 {@code 200}。没有排除配置时 {@link ResponseWrapAdvice} 会把引擎的 {@code ApiResult}
 * 再包一层 {@code Result}，顶层 {@code code} 变成 {@code "00000"}，拦截器随即
 * {@code Promise.reject(new Error(res.msg))}，控制台只有一句
 * {@code Uncaught (in promise) Error: 操作成功}，画布永远出不来。
 * </p>
 *
 * <h2>为什么用真实上下文 + MockMvc</h2>
 * <p>
 * {@code standaloneSetup} 不装配 {@code @ControllerAdvice}，用它测不出包装行为；
 * 而本模块的 warm-flow 引擎在一个 JVM 里只能有一个 Spring 上下文
 * （见 {@link WorkflowContextStartupTest} 与 {@code WorkflowDefinitionManagerTest} 的说明），
 * 所以这里复用它们的 {@code TestApplication} 与<strong>逐字一致</strong>的 properties
 * 以命中上下文缓存，再用 {@link MockMvcBuilders#webAppContextSetup} 从该上下文里手工装一个
 * MockMvc——它走上下文里真实的 {@code RequestMappingHandlerAdapter}，{@code @ControllerAdvice}
 * 因此照常生效，且不会新建第二个上下文。
 * </p>
 * <p>
 * 两处排除配置由 {@code src/test/resources/application.yml} 提供（配置文件不进上下文缓存键，
 * 是唯一能「只给共享上下文补配置、又不触发第二个上下文」的入口）；该文件里的这一段与
 * {@code carlos-workflow-boot} / {@code carlos-workflow-cloud} 生产配置逐字一致，
 * 并由 {@link #productionApplicationYmlsCarryTheSameExclusionStanza()} 守着。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@SpringBootTest(
    classes = WorkflowContextStartupTest.TestApplication.class,
    properties = {
        // ⚠️ 必须与 WorkflowContextStartupTest / WorkflowDefinitionManagerTest / WorkflowTaskManagerTest /
        // WorkflowTransactionalTest 逐字一致（含顺序）：上下文缓存键包含 properties 数组，
        // 一旦不同就会新建第二个带 FlowAutoConfig 的上下文，先坏的会是 WorkflowContextStartupTest。
        "spring.datasource.dynamic.primary=master",
        "spring.datasource.dynamic.datasource.master.url=jdbc:h2:mem:carlos_workflow_poc;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.dynamic.datasource.master.driver-class-name=org.h2.Driver",
        "spring.datasource.dynamic.datasource.master.username=sa",
        "spring.datasource.dynamic.datasource.master.password=",
        "spring.application.name=carlos-workflow",
        "carlos.mybatis.db-type=dm",
        "carlos.workflow.enabled=true",
        "warm-flow.banner=false",
        "warm-flow.permission-handler-path="
    }
)
class ResponseWrapExclusionTest {

    /** {@code ResponseWrapAdvice} 不读转换器类型，任意实现即可满足签名 */
    private static final Class<? extends HttpMessageConverter<?>> ANY_CONVERTER = StringHttpMessageConverter.class;

    /** 生产配置里排除的包：引擎全部 2 个 REST 控制器都在 {@code org.dromara.warm.flow.ui.controller} 下 */
    private static final List<String> EXPECTED_EXCLUDE_PACKAGES = List.of("org.dromara.warm.flow.ui");

    /** 生产配置里排除的路径：设计器的数据接口与配置接口 */
    private static final List<String> EXPECTED_EXCLUDE_PATHS = List.of("/warm-flow/**", "/warm-flow-ui/**");

    /** surefire 的工作目录是模块目录，三个配置文件因此都能用相对路径定位 */
    private static final Path TEST_CONFIG = Path.of("src/test/resources/application.yml");

    private static final Path BOOT_CONFIG = Path.of("../carlos-workflow-boot/src/main/resources/application.yml");

    private static final Path CLOUD_CONFIG = Path.of("../carlos-workflow-cloud/src/main/resources/application.yml");

    /**
     * 与 docs/sql/workflow_postgresql.sql 的 flow_definition 段等价，仅按 H2 语法改写。
     * 与 {@code WorkflowDefinitionManagerTest} 里的同名夹具一致：业务端点
     * {@code /workflow/definition/page} 要真的查到东西才算走通，表不在就只能拿到异常信封，
     * 「业务接口仍被包装」这条断言就失去了意义。
     */
    private static final String FLOW_DEFINITION_DDL = """
        CREATE TABLE flow_definition (
            id              BIGINT       NOT NULL,
            flow_code       VARCHAR(40)  NOT NULL,
            flow_name       VARCHAR(100) NOT NULL,
            model_value     VARCHAR(40)  NOT NULL DEFAULT 'CLASSICS',
            category        VARCHAR(100) NULL,
            version         VARCHAR(20)  NOT NULL,
            is_publish      SMALLINT     NOT NULL DEFAULT 0,
            form_custom     CHAR(1)      NULL DEFAULT 'N',
            form_path       VARCHAR(100) NULL,
            activity_status SMALLINT     NOT NULL DEFAULT 1,
            listener_type   VARCHAR(100) NULL,
            listener_path   VARCHAR(400) NULL,
            ext             VARCHAR(500) NULL,
            create_time     TIMESTAMP    NULL,
            create_by       VARCHAR(64)  NULL,
            update_time     TIMESTAMP    NULL,
            update_by       VARCHAR(64)  NULL,
            del_flag        CHAR(1)      NULL DEFAULT '0',
            tenant_id       VARCHAR(40)  NULL,
            CONSTRAINT flow_definition_pkey PRIMARY KEY (id)
        )
        """;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws SQLException {
        // webAppContextSetup 用的是上下文里真实的 RequestMappingHandlerAdapter，
        // @ControllerAdvice（ResponseWrapAdvice）与全局异常处理都在生效范围内。
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS flow_definition");
            statement.execute(FLOW_DEFINITION_DDL);
        }
    }

    // ==================== 断言 1：设计器配置接口不被包装 ====================

    @Test
    void designerConfigEndpointKeepsEngineApiResultOnTopLevel() throws Exception {
        mockMvc.perform(get("/warm-flow-ui/config"))
            .andExpect(status().isOk())
            // 先钉「没有被再包一层」：Carlos 信封的字段一个都不该出现。
            // 放最前面是因为被包装时这两条的失败信息最直白（"Expected no value at JSON path $.success
            // but found: true"），而 code 那条会因 JSON 类型强转把实际值 "00000" 显示成 0。
            .andExpect(jsonPath("$.success").doesNotExist())
            .andExpect(jsonPath("$.timestamp").doesNotExist())
            // 引擎的 ApiResult.ok()：code 是数字 200。被 Carlos 包装后这里是字符串 "00000"。
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.tokenNameList[0]").value("Authorization"))
            .andExpect(jsonPath("$.data.tokenNameList.length()").value(1))
            .andExpect(jsonPath("$.data.framework").value("SPRING_BOOT"));
    }

    // ==================== 断言 2：设计器数据接口不被包装 ====================

    @Test
    void designerDataEndpointKeepsEngineApiResultOnTopLevel() throws Exception {
        mockMvc.perform(get("/warm-flow/query-def"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").doesNotExist())
            .andExpect(jsonPath("$.timestamp").doesNotExist())
            .andExpect(jsonPath("$.code").value(200))
            // queryDef(null) 返回一个全新的 DefJson，modelValue 固定为 CLASSICS
            .andExpect(jsonPath("$.data.modelValue").value("CLASSICS"));
    }

    // ==================== 断言 3：业务接口照旧被包装 ====================

    @Test
    void businessEndpointStillReturnsTheCarlosEnvelope() throws Exception {
        mockMvc.perform(get("/workflow/definition/page").param("current", "1").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.msg").value("操作成功"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 上一条只证明「业务接口的响应长得像 Carlos 信封」，但业务控制器<strong>自己就返回
     * {@code Result}</strong>，所以单看 JSON 无法区分「被 advice 包了」与「本来就返回 Result」。
     * 这里把排除规则按判定条件拆开验：
     * <ul>
     *   <li>{@code supports} 对引擎控制器返回 false（包名规则生效），对业务控制器返回 true（业务仍受管）；</li>
     *   <li>喂一个<strong>非</strong> {@code Result} 的业务返回值，advice 必须把它包成 {@code Result}；</li>
     *   <li>喂引擎路径时原样返回（路径规则生效）。</li>
     * </ul>
     */
    @Test
    void adviceSkipsEngineControllersButStillWrapsBusinessPayloads() throws Exception {
        ResponseWrapAdvice advice = applicationContext.getBean(ResponseWrapAdvice.class);

        MethodParameter designerConfig = returnTypeOf(WarmFlowUiController.class, "config");
        MethodParameter designerQueryDef = returnTypeOf(WarmFlowController.class, "queryDef", Long.class);
        MethodParameter businessPage =
            returnTypeOf(WorkflowDefinitionController.class, "page", ApiWorkflowDefinitionPageParam.class);

        assertThat(advice.supports(designerConfig, ANY_CONVERTER))
            .as("exclude-packages 命中 org.dromara.warm.flow.ui，设计器配置接口必须完全绕开包装")
            .isFalse();
        assertThat(advice.supports(designerQueryDef, ANY_CONVERTER))
            .as("同一个包下的数据接口同理")
            .isFalse();
        assertThat(advice.supports(businessPage, ANY_CONVERTER))
            .as("业务控制器仍必须被包装——排除是外科手术式的，不能把包装器整体关掉")
            .isTrue();

        WorkflowDefinitionAO payload = new WorkflowDefinitionAO();
        payload.setDefinitionId(7L);
        Object wrapped = advice.beforeBodyWrite(payload, businessPage, MediaType.APPLICATION_JSON,
            ANY_CONVERTER, request("/workflow/definition/page"),
            new ServletServerHttpResponse(new MockHttpServletResponse()));

        assertThat(wrapped)
            .as("非 Result 的业务返回值确实被包成了 Carlos 信封")
            .isInstanceOf(Result.class);
        assertThat(((Result<?>) wrapped).getCode()).isEqualTo("00000");
        assertThat(((Result<?>) wrapped).getData()).isSameAs(payload);

        ApiResult<WarmFlowVo> engineBody = ApiResult.ok(new WarmFlowVo());
        Object untouched = advice.beforeBodyWrite(engineBody, designerConfig, MediaType.APPLICATION_JSON,
            ANY_CONVERTER, request("/warm-flow-ui/config"),
            new ServletServerHttpResponse(new MockHttpServletResponse()));

        assertThat(untouched)
            .as("第二道保险：即便走到 beforeBodyWrite，exclude-paths 也必须让引擎响应原样返回")
            .isSameAs(engineBody);
    }

    /**
     * 反事实对照：用一份空配置构造的 advice 会把同一段引擎返回值包成 {@code Result}。
     * 它证明上面那些「原样返回」是排除配置起的作用，而不是别的东西顺手挡掉了；
     * 也把真实故障的成因固定在测试里（顶层 code 变成 "00000" → 设计器 Promise.reject）。
     */
    @Test
    void withoutExclusionConfigurationTheAdviceWouldWrapEnginePayloads() throws Exception {
        ResponseWrapAdvice bareAdvice = new ResponseWrapAdvice(new ResponseWrapProperties());
        MethodParameter designerConfig = returnTypeOf(WarmFlowUiController.class, "config");
        ApiResult<WarmFlowVo> engineBody = ApiResult.ok(new WarmFlowVo());

        assertThat(bareAdvice.supports(designerConfig, ANY_CONVERTER))
            .as("默认配置下引擎控制器不在任何排除名单里，会被包装——这正是待修复的故障")
            .isTrue();

        Object wrapped = bareAdvice.beforeBodyWrite(engineBody, designerConfig, MediaType.APPLICATION_JSON,
            ANY_CONVERTER, request("/warm-flow-ui/config"),
            new ServletServerHttpResponse(new MockHttpServletResponse()));

        assertThat(wrapped).isInstanceOf(Result.class);
        assertThat(((Result<?>) wrapped).getCode())
            .as("设计器看到的顶层 code 正是 \"00000\"，于是 Promise.reject(new Error(\"操作成功\"))")
            .isEqualTo("00000");
    }

    // ==================== 断言 4：包装器真的存在，且配置真的被绑定 ====================

    @Test
    void responseWrapAdviceBeanIsRegisteredInThisContext() {
        assertThat(applicationContext.getBeanNamesForType(ResponseWrapAdvice.class))
            .as("没有这个 Bean，前面「引擎不被包装」的断言会因为「根本没有包装器」而假通过")
            .hasSize(1);
        assertThat(applicationContext.getBean(ResponseWrapAdvice.class)).isNotNull();
    }

    @Test
    void exclusionIsBoundFromYamlIntoResponseWrapProperties() {
        WebProperties webProperties = applicationContext.getBean(WebProperties.class);

        assertThat(webProperties.getResponseWrap().isEnabled())
            .as("包装器保持开启——排除不是靠关掉包装实现的")
            .isTrue();
        assertThat(webProperties.getResponseWrap().getExcludePackages()).isEqualTo(EXPECTED_EXCLUDE_PACKAGES);
        assertThat(webProperties.getResponseWrap().getExcludePaths()).isEqualTo(EXPECTED_EXCLUDE_PATHS);
    }

    /**
     * 运行期那份配置是 {@code src/test/resources/application.yml} 里的副本；这里解析真正的生产文件，
     * 确认三个文件里的 {@code carlos.web.response-wrap} 逐字一致——否则本测试验证的就不是生产行为。
     */
    @Test
    void productionApplicationYmlsCarryTheSameExclusionStanza() throws IOException {
        Map<String, Object> fromTestConfig = responseWrapOfFile(TEST_CONFIG);

        assertThat(fromTestConfig).isEqualTo(responseWrapOfFile(BOOT_CONFIG));
        assertThat(fromTestConfig).isEqualTo(responseWrapOfFile(CLOUD_CONFIG));
        assertThat(fromTestConfig)
            .containsEntry("exclude-packages", EXPECTED_EXCLUDE_PACKAGES)
            .containsEntry("exclude-paths", EXPECTED_EXCLUDE_PATHS);
    }

    // ==================== 夹具 ====================

    private static MethodParameter returnTypeOf(Class<?> controller, String methodName, Class<?>... parameterTypes)
        throws NoSuchMethodException {
        Method method = controller.getDeclaredMethod(methodName, parameterTypes);
        return new MethodParameter(method, -1);
    }

    private static ServerHttpRequest request(String path) throws IOException {
        return new ServletServerHttpRequest(new MockHttpServletRequest("GET", path));
    }

    private static Map<String, Object> responseWrapOfFile(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            for (Object document : new Yaml().loadAll(input)) {
                Map<String, Object> block = nestedMap(document, "carlos", "web", "response-wrap");
                if (block != null) {
                    return block;
                }
            }
        }
        throw new AssertionError("未在 " + path.toAbsolutePath() + " 找到 carlos.web.response-wrap 配置");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> nestedMap(Object document, String... keys) {
        Object cursor = document;
        for (String key : keys) {
            if (!(cursor instanceof Map<?, ?> map)) {
                return null;
            }
            cursor = map.get(key);
        }
        return cursor instanceof Map ? (Map<String, Object>) cursor : null;
    }

}
