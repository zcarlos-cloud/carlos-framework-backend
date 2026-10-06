package com.carlos.agent.controller;

import com.carlos.log.annotation.Log;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * 审计注解守卫测试：把「哪些接口要留痕」钉死在代码里，而不是靠人自觉。
 * </p>
 * <p>
 * 为什么需要它：{@code @Log} 是纯声明式注解，漏标的接口不会有任何编译期或运行期提示 ——
 * 它只是安静地不产生审计记录。本模块只有一个控制器（{@code agent/chat}，SSE 流式对话），
 * 这里额外把「返回 SSE 就必须关掉响应数据落库」也钉住：把流式响应的字节序列化进审计库
 * 既没有语义，又可能把整段对话塞进 ClickHouse。
 * </p>
 * <p>
 * 不起 Spring 上下文：只读类与方法的注解元数据，因此可以随时执行、不依赖数据库与 Nacos。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class AgentAuditAnnotationGuardTest {

    /**
     * 本模块（carlos-agent-bus）自有代码的根包；递归扫描其下全部 {@code @RestController}
     */
    private static final String BASE_PACKAGE = "com.carlos.agent";

    /**
     * 包名对应的资源路径
     */
    private static final String PACKAGE_PATH = BASE_PACKAGE.replace('.', '/');

    /**
     * 写接口中「刻意不加 @Log」的例外清单：元素形如 {@code POST /agent/chat}
     * <p>
     * 当前为空 —— 本模块唯一的写接口已标注。新增例外必须连同理由一起写在这里。
     * </p>
     */
    private static final Set<String> WRITE_ENDPOINTS_EXEMPT_FROM_LOG = Set.of();

    /**
     * 读接口中「特意加 @Log」的例外清单（本模块当前没有读接口）
     */
    private static final Set<String> READ_ENDPOINTS_ALLOWED_TO_LOG = Set.of();

    /**
     * 视为「写操作」的 HTTP 方法
     */
    private static final Set<String> WRITE_METHODS = Set.of(
        RequestMethod.POST.name(), RequestMethod.PUT.name(), RequestMethod.DELETE.name());

    @Test
    void everyWriteEndpointCarriesAuditAnnotation() {
        List<Endpoint> writeEndpoints = endpoints().stream().filter(Endpoint::write).toList();

        assertThat(writeEndpoints)
            .as("必须扫到写接口，扫不到说明本测试退化成空断言")
            .isNotEmpty();

        List<String> unlogged = writeEndpoints.stream()
            .filter(endpoint -> !WRITE_ENDPOINTS_EXEMPT_FROM_LOG.contains(endpoint.key()))
            .filter(endpoint -> endpoint.audit() == null)
            .map(Endpoint::key)
            .toList();

        assertThat(unlogged)
            .as("写接口一律要留痕：漏标 @Log 不会报错，只会静默丢掉审计记录。"
                + "确需豁免请加进 WRITE_ENDPOINTS_EXEMPT_FROM_LOG 并写明理由")
            .isEmpty();
    }

    @Test
    void noReadEndpointCarriesAuditAnnotation() {
        List<String> unexpected = endpoints().stream()
            .filter(endpoint -> endpoint.read() && endpoint.audit() != null)
            .map(Endpoint::key)
            .filter(key -> !READ_ENDPOINTS_ALLOWED_TO_LOG.contains(key))
            .toList();

        assertThat(unexpected)
            .as("读接口不该留痕：标了 @Log 只会淹没审计库。确需豁免请加进 READ_ENDPOINTS_ALLOWED_TO_LOG")
            .isEmpty();
    }

    @Test
    void auditLogTypesAreUniqueWithinTheModule() {
        Map<String, List<String>> endpointsByLogType = new LinkedHashMap<>();

        for (Endpoint endpoint : endpoints()) {
            Log audit = endpoint.audit();
            if (audit == null) {
                continue;
            }
            assertThat(audit.logType())
                .as("%s 的 @Log 必须显式给出 logType（留空会退化成 title_businessType，唯一性无从谈起）",
                    endpoint.key())
                .isNotBlank();
            endpointsByLogType.computeIfAbsent(audit.logType(), key -> new ArrayList<>()).add(endpoint.key());
        }

        assertThat(endpointsByLogType)
            .as("必须扫到带 @Log 的接口，扫不到说明本测试退化成空断言")
            .isNotEmpty();

        List<String> duplicated = new ArrayList<>();
        endpointsByLogType.forEach((logType, keys) -> {
            if (keys.size() > 1) {
                duplicated.add(logType + " <- " + keys);
            }
        });

        assertThat(duplicated)
            .as("logType 是审计检索的主键，模块内必须唯一")
            .isEmpty();
    }

    @Test
    void singleControllerKeepsSseResponseOutOfTheAuditStore() {
        List<Class<?>> controllers = restControllers();

        assertThat(controllers)
            .as("carlos-agent-bus 只应有 agent/chat 一个控制器；新增控制器说明模块边界变了，"
                + "请连同审计约定一起复核")
            .hasSize(1);

        List<String> problems = new ArrayList<>();
        for (Endpoint endpoint : endpoints()) {
            if (endpoint.audit() == null) {
                problems.add(endpoint.key() + " 缺少 @Log");
            } else if (endpoint.audit().saveResponseData()) {
                problems.add(endpoint.key() + " 开了 saveResponseData");
            }
        }

        assertThat(problems)
            .as("SSE 流式响应不得序列化进审计库：接口要留痕，但响应数据必须显式关闭")
            .isEmpty();
    }

    /**
     * 收集本模块全部 {@code @RestController} 的端点
     *
     * @return 端点列表（一个方法按 HTTP 方法拆成多条）
     */
    private static List<Endpoint> endpoints() {
        List<Class<?>> controllers = restControllers();

        assertThat(controllers)
            .as("必须扫到 %s 下的控制器，扫不到说明本测试退化成空断言", BASE_PACKAGE)
            .isNotEmpty();

        List<Endpoint> endpoints = new ArrayList<>();
        for (Class<?> controller : controllers) {
            String basePath = basePathOf(controller);
            for (Method method : controller.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null) {
                    continue;
                }
                Log audit = AnnotatedElementUtils.findMergedAnnotation(method, Log.class);
                String path = join(basePath, firstPath(mapping));
                for (String httpMethod : requestMethods(mapping)) {
                    endpoints.add(new Endpoint(
                        httpMethod + " " + path,
                        WRITE_METHODS.contains(httpMethod),
                        RequestMethod.GET.name().equals(httpMethod),
                        audit));
                }
            }
        }
        return endpoints;
    }

    /**
     * 扫描 {@link #BASE_PACKAGE} 及其子包下的 {@code @RestController}
     * <p>
     * 刻意不用 {@code ClassPathScanningCandidateComponentProvider}：本项目（Spring Framework 7）上
     * 它对同一组类返回空集合，会让断言静默退化成空断言；直接用资源解析 + 注解判定，
     * 扫不到时会由调用方的 {@code isNotEmpty()} 显式失败。
     * </p>
     * <p>
     * 类加载用 {@code initialize = false}：审计守卫只需要类与方法的注解元数据，
     * 没必要（也不应该）在测试里触发这些类的静态初始化。
     * </p>
     *
     * @return 控制器类列表
     */
    private static List<Class<?>> restControllers() {
        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
            + PACKAGE_PATH + "/**/*.class";

        Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver().getResources(packageSearchPath);
        } catch (IOException e) {
            throw new AssertionError("扫描包失败: " + BASE_PACKAGE, e);
        }

        List<Class<?>> controllers = new ArrayList<>();
        for (Resource resource : resources) {
            String className = classNameOf(resource);
            if (className == null) {
                continue;
            }
            Class<?> type = loadClass(className);
            if (AnnotatedElementUtils.hasAnnotation(type, RestController.class)) {
                controllers.add(type);
            }
        }
        return controllers;
    }

    /**
     * 由资源路径还原全限定类名；内部类（{@code Foo$Bar.class}）与非 class 文件返回 null
     *
     * @param resource 资源
     * @return 全限定类名，或 null 表示跳过
     */
    private static String classNameOf(Resource resource) {
        String filename = resource.getFilename();
        if (filename == null || !filename.endsWith(".class") || filename.indexOf('$') >= 0) {
            return null;
        }

        String path;
        try {
            path = resource.getURL().toString().replace('\\', '/');
        } catch (IOException e) {
            throw new AssertionError("无法解析资源路径: " + resource, e);
        }

        // 用 lastIndexOf：工程绝对路径里也可能出现 carlos-integration/carlos-agent 这类片段
        int index = path.lastIndexOf(PACKAGE_PATH + "/");
        if (index < 0) {
            return null;
        }

        String relative = path.substring(index + PACKAGE_PATH.length() + 1);
        String className = relative.substring(0, relative.length() - ".class".length()).replace('/', '.');
        return BASE_PACKAGE + "." + className;
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className, false, AgentAuditAnnotationGuardTest.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new AssertionError("扫描到的类加载不到: " + className, e);
        }
    }

    private static String basePathOf(Class<?> controller) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
        if (mapping == null) {
            return "";
        }
        return withLeadingSlash(firstPath(mapping));
    }

    private static List<String> requestMethods(RequestMapping mapping) {
        RequestMethod[] methods = mapping.method();
        if (methods.length == 0) {
            return List.of("*");
        }
        Set<String> names = new TreeSet<>();
        for (RequestMethod method : methods) {
            names.add(method.name());
        }
        return List.copyOf(names);
    }

    private static String join(String basePath, String methodPath) {
        if (methodPath.isEmpty()) {
            return basePath;
        }
        return methodPath.startsWith("/") ? basePath + methodPath : basePath + "/" + methodPath;
    }

    private static String firstPath(RequestMapping mapping) {
        return mapping.path().length == 0 ? "" : mapping.path()[0];
    }

    private static String withLeadingSlash(String path) {
        if (path.isEmpty() || path.startsWith("/")) {
            return path;
        }
        return "/" + path;
    }

    /**
     * 单个端点：{@code key} 形如 {@code POST /agent/chat}
     *
     * @param key   「HTTP 方法 + 完整路径」
     * @param write 是否写操作
     * @param read  是否 GET
     * @param audit 方法上的 {@code @Log}，没有则为 null
     */
    private record Endpoint(String key, boolean write, boolean read, Log audit) {
    }

}
