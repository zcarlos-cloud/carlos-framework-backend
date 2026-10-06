package com.carlos.workflow.controller;

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
 * 它只是安静地不产生审计记录，等到出事回溯时才发现「这一步没人记」。这里用反射把约定固化：
 * 写接口（POST / PUT / DELETE）必须带 {@code @Log}，读接口（GET）不许带，
 * {@code logType} 在模块内唯一。
 * </p>
 * <p>
 * 不起 Spring 上下文：只读类与方法的注解元数据，因此可以随时执行、不依赖数据库与 Nacos。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowAuditAnnotationGuardTest {

    /**
     * 本模块（carlos-workflow-bus）自有代码的根包；递归扫描其下全部 {@code @RestController}
     */
    private static final String BASE_PACKAGE = "com.carlos.workflow";

    /**
     * 包名对应的资源路径
     */
    private static final String PACKAGE_PATH = BASE_PACKAGE.replace('.', '/');

    /**
     * 写接口中「刻意不加 @Log」的例外清单：元素形如 {@code POST /workflow/definition/basic}
     * <p>
     * 当前为空 —— 纳入审计范围的写接口已全部标注。新增例外必须连同理由一起写在这里，
     * 否则本测试就退化成「谁想跳过就跳过」的空壳。
     * </p>
     */
    private static final Set<String> WRITE_ENDPOINTS_EXEMPT_FROM_LOG = Set.of();

    /**
     * 读接口中「特意加 @Log」的例外清单：元素形如 {@code GET /workflow/definition/{id}/export}
     * <p>
     * 导出是「数据外带」而非普通查询：谁在什么时候把流程定义整个搬走了，属于必须留痕的安全事件，
     * 因此这条 GET 破例标注（返回文件流，故 {@code saveResponseData} 保持默认 false）。
     * </p>
     */
    private static final Set<String> READ_ENDPOINTS_ALLOWED_TO_LOG = Set.of(
        "GET /workflow/definition/{id}/export");

    /**
     * 视为「写操作」的 HTTP 方法
     */
    private static final Set<String> WRITE_METHODS = Set.of(
        RequestMethod.POST.name(), RequestMethod.PUT.name(), RequestMethod.DELETE.name());

    /**
     * 本模块 {@code @RestController} 的完整清单：3 个前端控制器 + 3 个 Feign 契约实现
     * <p>
     * 为什么要钉住清单：扫描一旦退化成「只扫到一部分控制器」，下面的三条规则会全部静默通过。
     * 清单本身变化时（新增控制器）本测试会失败，这正是让人回头确认「新接口要不要留痕」的时机。
     * </p>
     */
    private static final Set<String> EXPECTED_CONTROLLERS = Set.of(
        "WorkflowDefinitionController",
        "WorkflowInstanceController",
        "WorkflowTaskController",
        "ApiWorkflowDefinitionImpl",
        "ApiWorkflowInstanceImpl",
        "ApiWorkflowTaskImpl");

    @Test
    void controllerScanCoversEveryControllerOfTheModule() {
        assertThat(restControllers())
            .extracting(Class::getSimpleName)
            .as("扫描必须覆盖本模块全部控制器；清单变化时请连同审计约定一起复核")
            .containsExactlyInAnyOrderElementsOf(EXPECTED_CONTROLLERS);
    }

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
            .as("读接口不该留痕：分页/详情这类高频查询标了 @Log 只会淹没审计库。"
                + "确需豁免请加进 READ_ENDPOINTS_ALLOWED_TO_LOG 并写明理由")
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
     * 与 {@code WorkflowNamespaceBoundaryTest} 一样刻意不用
     * {@code ClassPathScanningCandidateComponentProvider}：本项目（Spring Framework 7）上它对同一组类
     * 返回空集合，会让断言静默退化成空断言。这里用资源解析 + 注解判定，扫不到时由调用方的
     * {@code isNotEmpty()} 显式失败。
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

        // 用 lastIndexOf：工程绝对路径里也可能出现 carlos-integration/carlos-workflow 这类片段
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
            return Class.forName(className, false, WorkflowAuditAnnotationGuardTest.class.getClassLoader());
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
     * 单个端点：{@code key} 形如 {@code POST /workflow/definition/basic}
     *
     * @param key    「HTTP 方法 + 完整路径」
     * @param write  是否写操作
     * @param read   是否 GET
     * @param audit  方法上的 {@code @Log}，没有则为 null
     */
    private record Endpoint(String key, boolean write, boolean read, Log audit) {
    }

}
