package com.carlos.workflow.controller;

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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * 命名空间边界守护测试：把「前端（经网关）」与「服务间 Feign」两套端点钉死在各自的包与前缀里。
 * </p>
 * <p>
 * 为什么需要它：网关只路由 {@code /<服务>/**}（业务路径原样透传），{@code /api/**} <b>不在路由表内</b>；
 * 反过来，{@code /api/**} 又只给服务间 Feign 用。一旦有人把管理端接口加回
 * {@code ApiWorkflowXxx} + {@code apiimpl}（历史上就是这样：一套 URL 同时充当 Feign 契约与前端接口），
 * 内部命名空间就对外可见了，而且不会有任何编译期提示。因此这里断言：
 * </p>
 * <ol>
 *   <li>{@code com.carlos.workflow.controller} 下的类<b>不 implements</b>
 *       {@code com.carlos.workflow.api} 包里的任何接口（前端控制器不得兼任 Feign 契约实现）；</li>
 *   <li>这些类的 {@code @RequestMapping} 全部以 {@code /workflow/} 开头，且不带 {@code /api} 前缀；</li>
 *   <li>{@code com.carlos.workflow.apiimpl} 下三个实现类的 {@code @RequestMapping}
 *       全部以 {@code /api/workflow} 开头；</li>
 *   <li>两侧端点的「HTTP 方法 + 路径」集合交集为空——包括把 Feign 的 {@code /api} 前缀剥掉后
 *       再比一次，这样即便有人换个写法把管理接口塞回 Feign 接口也能拦住；</li>
 *   <li>Feign 侧只剩约定保留的 5 个端点（多一个都要显式改这份清单，逼人想清楚）。</li>
 * </ol>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowNamespaceBoundaryTest {

    /**
     * 前端（经网关）命名空间：管理端点所在包
     */
    private static final String FRONTEND_PACKAGE = "com.carlos.workflow.controller";

    /**
     * 服务间命名空间：Feign 契约实现所在包
     */
    private static final String FEIGN_IMPL_PACKAGE = "com.carlos.workflow.apiimpl";

    /**
     * 服务间契约接口所在包（前端控制器不得实现之）
     */
    private static final String FEIGN_API_PACKAGE = "com.carlos.workflow.api";

    /**
     * 前端命名空间根路径
     */
    private static final String FRONTEND_ROOT = "/workflow";

    /**
     * 前端命名空间前缀（类级 @RequestMapping 必须以此开头）
     */
    private static final String FRONTEND_PREFIX = FRONTEND_ROOT + "/";

    /**
     * 服务间命名空间根路径（网关不路由 /api/**）
     */
    private static final String FEIGN_PREFIX = "/api/workflow";

    /**
     * 没有显式声明 HTTP 方法时的占位符
     */
    private static final String ANY_METHOD = "*";

    /**
     * 服务间契约唯一保留的 5 个端点：业务服务发起/查询流程真正需要的那几个
     */
    private static final Set<String> FEIGN_ENDPOINTS = Set.of(
        "GET /api/workflow/definition/published",
        "POST /api/workflow/instance/start",
        "GET /api/workflow/instance/{instanceId}/chart",
        "GET /api/workflow/task/{taskId}",
        "GET /api/workflow/his-task/{hisTaskId}");

    @Test
    void frontendControllersDoNotImplementAnyFeignContractInterface() {
        List<Class<?>> controllers = frontendControllers();

        assertThat(controllers)
            .as("必须扫到 %s 下的控制器，扫不到说明本测试退化成空断言", FRONTEND_PACKAGE)
            .isNotEmpty();

        for (Class<?> controller : controllers) {
            assertThat(controller.getInterfaces())
                .as("%s 不得 implements %s 包里的接口：前端控制器兼任 Feign 契约实现，"
                        + "就等于把同一组 URL 同时暴露在两个命名空间里（/api/** 不经网关）",
                    controller.getSimpleName(), FEIGN_API_PACKAGE)
                .noneMatch(WorkflowNamespaceBoundaryTest::inFeignApiPackage);
        }
    }

    @Test
    void frontendControllersAreMappedUnderTheWorkflowPrefixOnly() {
        for (Class<?> controller : frontendControllers()) {
            String basePath = basePathOf(controller);

            assertThat(basePath)
                .as("%s 的类级 @RequestMapping 必须落在前端命名空间 %s/** 下",
                    controller.getSimpleName(), FRONTEND_ROOT)
                .startsWith(FRONTEND_PREFIX);
            assertThat(basePath)
                .as("%s 的类级 @RequestMapping 不得带 /api 前缀：/api/** 不在网关路由表内，前端调不到",
                    controller.getSimpleName())
                .doesNotStartWith("/api");

            assertThat(endpointsOf(controller))
                .as("%s 的每个端点都应落在 %s/** 下且不带 /api 前缀",
                    controller.getSimpleName(), FRONTEND_ROOT)
                .isNotEmpty()
                .allSatisfy(endpoint -> {
                    assertThat(pathOf(endpoint)).startsWith(FRONTEND_PREFIX);
                    assertThat(pathOf(endpoint)).doesNotStartWith("/api");
                });
        }
    }

    @Test
    void feignImplementationsStayOnTheApiWorkflowPrefix() {
        List<Class<?>> implementations = feignImplementations();

        assertThat(implementations)
            .as("必须扫到 %s 下的实现类，扫不到说明本测试退化成空断言", FEIGN_IMPL_PACKAGE)
            .isNotEmpty();

        for (Class<?> implementation : implementations) {
            assertThat(basePathOf(implementation))
                .as("%s 是 Feign 契约实现，类级 @RequestMapping 必须以 %s 开头",
                    implementation.getSimpleName(), FEIGN_PREFIX)
                .startsWith(FEIGN_PREFIX);

            assertThat(endpointsOf(implementation))
                .as("%s 的每个端点都应落在 %s/** 下", implementation.getSimpleName(), FEIGN_PREFIX)
                .isNotEmpty()
                .allSatisfy(endpoint -> assertThat(pathOf(endpoint)).startsWith(FEIGN_PREFIX));
        }
    }

    @Test
    void frontendAndFeignEndpointSetsDoNotOverlap() {
        Set<String> frontendEndpoints = endpointsOf(frontendControllers());
        Set<String> feignEndpoints = endpointsOf(feignImplementations());

        assertThat(frontendEndpoints).isNotEmpty();
        assertThat(feignEndpoints).isNotEmpty();

        assertThat(frontendEndpoints)
            .as("同一个「HTTP 方法 + 路径」不能既在前端命名空间又在服务间命名空间")
            .doesNotContainAnyElementsOf(feignEndpoints);

        assertThat(normalized(frontendEndpoints))
            .as("把 Feign 的 /api 前缀剥掉后再比一次：把管理接口加回 Feign 接口会在这里被拦住")
            .doesNotContainAnyElementsOf(normalized(feignEndpoints));
    }

    @Test
    void feignNamespaceCarriesOnlyTheFiveAgreedEndpoints() {
        assertThat(endpointsOf(feignImplementations()))
            .as("服务间契约只剩「业务服务发起/查询流程」需要的 5 个端点；"
                + "新增前请先确认它确实属于服务间调用，而不是管理端接口")
            .containsExactlyInAnyOrderElementsOf(FEIGN_ENDPOINTS);

        assertThat(endpointsOf(frontendControllers()))
            .as("管理端接口必须与 Feign 契约分开：这些端点在 %s/** 下，不在 /api/** 下", FRONTEND_ROOT)
            .doesNotContainAnyElementsOf(FEIGN_ENDPOINTS);
    }

    /**
     * 扫描指定包下的 {@code @RestController}
     * <p>
     * 这里刻意不用 {@code ClassPathScanningCandidateComponentProvider}：本项目（Spring Framework 7）
     * 上它对同一组类返回空集合，会让下面的断言静默退化成空断言；直接用资源解析 + 注解判定，
     * 扫不到时会由调用方的 {@code isNotEmpty()} 显式失败。
     * </p>
     *
     * @param basePackage 包名
     * @return 控制器类列表
     */
    private static List<Class<?>> restControllersIn(String basePackage) {
        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
            + basePackage.replace('.', '/') + "/*.class";

        Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver().getResources(packageSearchPath);
        } catch (IOException e) {
            throw new AssertionError("扫描包失败: " + basePackage, e);
        }

        return Arrays.stream(resources)
            .map(Resource::getFilename)
            .filter(Objects::nonNull)
            .filter(filename -> filename.endsWith(".class"))
            .map(filename -> basePackage + "." + filename.substring(0, filename.length() - ".class".length()))
            .map(WorkflowNamespaceBoundaryTest::loadClass)
            .filter(type -> AnnotatedElementUtils.hasAnnotation(type, RestController.class))
            .toList();
    }

    private static List<Class<?>> frontendControllers() {
        return restControllersIn(FRONTEND_PACKAGE);
    }

    private static List<Class<?>> feignImplementations() {
        return restControllersIn(FEIGN_IMPL_PACKAGE);
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("扫描到的类加载不到: " + className, e);
        }
    }

    private static boolean inFeignApiPackage(Class<?> type) {
        String packageName = type.getPackageName();
        return packageName.equals(FEIGN_API_PACKAGE) || packageName.startsWith(FEIGN_API_PACKAGE + ".");
    }

    private static String basePathOf(Class<?> controller) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
        assertThat(mapping)
            .as("%s 缺少类级 @RequestMapping", controller.getSimpleName())
            .isNotNull();
        return firstPath(mapping);
    }

    private static Set<String> endpointsOf(Class<?> controller) {
        return endpointsOf(List.of(controller));
    }

    /**
     * 收集一组控制器暴露的全部端点，元素形如 {@code GET /workflow/task/todo/page}
     *
     * @param controllers 控制器类列表
     * @return 「HTTP 方法 + 完整路径」集合
     */
    private static Set<String> endpointsOf(List<Class<?>> controllers) {
        Set<String> endpoints = new TreeSet<>();
        for (Class<?> controller : controllers) {
            String basePath = basePathOf(controller);
            for (Method method : controller.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null) {
                    continue;
                }
                String path = join(basePath, firstPath(mapping));
                for (String requestMethod : requestMethods(mapping)) {
                    endpoints.add(requestMethod + " " + path);
                }
            }
        }
        return endpoints;
    }

    private static List<String> requestMethods(RequestMapping mapping) {
        RequestMethod[] methods = mapping.method();
        if (methods.length == 0) {
            return List.of(ANY_METHOD);
        }
        return List.of(methods).stream().map(RequestMethod::name).toList();
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

    /**
     * 剥离服务间命名空间的 {@code /api} 前缀，用于「同一路径换个前缀硬塞回来」这种改法的自查
     *
     * @param endpoints 端点集合
     * @return 归一化后的端点集合
     */
    private static Set<String> normalized(Set<String> endpoints) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String endpoint : endpoints) {
            normalized.add(endpoint.replace(" " + FEIGN_PREFIX, " " + FRONTEND_ROOT));
        }
        return normalized;
    }

    private static String pathOf(String endpoint) {
        return endpoint.substring(endpoint.indexOf(' ') + 1);
    }

}
