package com.carlos.job.controller;

import com.carlos.log.annotation.Log;
import org.junit.jupiter.api.DisplayName;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * 审计注解守卫测试：把 {@code @Log} 在 carlos-job 里的覆盖面钉死，防止后续新增写接口时漏标。
 * </p>
 * <p>
 * 为什么需要它：{@code @Log} 是纯声明式注解，漏标不会有任何编译期或运行期提示 —— 接口照常工作，
 * 只是那条操作永远不会出现在 carlos-audit / ClickHouse 里，直到出事回查时才发现没有记录。
 * 因此这里用反射把规则固化成断言：
 * </p>
 * <ol>
 *   <li>所有 {@code @RestController} 的<b>写</b> handler（{@code @PostMapping} / {@code @PutMapping}
 *       / {@code @DeleteMapping}）必须带 {@code @Log}，例外只能列进 {@link #EXEMPT} 并写明理由；</li>
 *   <li>{@code @GetMapping} 的<b>只读</b> handler 一律不得带 {@code @Log}（读接口不该刷审计）；</li>
 *   <li>{@code logType} 全模块唯一 —— 复用会让审计库里无法据此区分操作。</li>
 * </ol>
 * <p>
 * 扫描范围是 {@code com.carlos.job}（递归），因此 {@code carlos-job-bus} 的
 * {@code controller} / {@code apiimpl} 两侧都会被覆盖：{@code JobController}、{@code JobLogController}、
 * {@code JobMonitorController}、{@code JobRegisterController}、{@code ApiJobImpl}，以及
 * {@code carlos-job-client} jar 里的 {@code JobExecuteController}（见 {@link #EXEMPT}）。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class JobAuditAnnotationGuardTest {

    /**
     * 扫描根包：carlos-job 全部子模块共用此包
     */
    private static final String ROOT_PACKAGE = "com.carlos.job";

    /**
     * 扫描根包对应的资源路径前缀，用于从资源 URL 反推类名
     */
    private static final String ROOT_PATH_MARKER = "com/carlos/job/";

    /**
     * 需要审计的写操作 HTTP 方法
     */
    private static final Set<RequestMethod> WRITE_METHODS = Set.of(
        RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE);

    /**
     * 刻意不打审计的写接口白名单，键为 {@code 简单类名#方法名}
     * <p>
     * <b>JobExecuteController#execute</b>（{@code /inner/job/execute}）：该端点虽然要求标注，
     * 但它并不在 carlos-job-bus，而在 <b>carlos-job-client</b> —— 一个被所有业务服务引入的客户端库。
     * 它既没有 {@code carlos-spring-boot-starter-log}（拿不到 {@code @Log} 本身），
     * 也没有 {@code carlos-audit-api} 提供的 {@code AuditMqLogStorage}（拿不到落库链路）。
     * 真要给它加上审计，就必须把 log / MQ / audit 三套依赖推进每个业务服务，
     * 那是一次跨模块的部署面变更，不属于「只加注解」的范围，故显式豁免并留待单独决策。
     * </p>
     */
    private static final Set<String> EXEMPT = Set.of(
        "JobExecuteController#execute");

    @Test
    @DisplayName("写接口的 handler 必须带 @Log")
    void writeHandlersMustCarryLogAnnotation() {
        ScanResult scan = scanRestControllers();

        assertThat(scan.controllers())
            .as("必须扫到 %s 下的 @RestController，扫不到说明本测试退化成空断言", ROOT_PACKAGE)
            .isNotEmpty();
        assertThat(unloadableControllerNames(scan))
            .as("控制器类加载失败会让上面的扫描静默漏项，必须先排除")
            .isEmpty();

        List<String> missing = new ArrayList<>();
        for (Class<?> controller : scan.controllers()) {
            for (Method method : methodsOf(controller)) {
                if (!isWriteHandler(method) || EXEMPT.contains(keyOf(controller, method))) {
                    continue;
                }
                if (!AnnotatedElementUtils.hasAnnotation(method, Log.class)) {
                    missing.add(keyOf(controller, method));
                }
            }
        }

        assertThat(missing)
            .as("以下写接口没有 @Log，操作不会进入审计库；"
                + "确实不该记的请加入 EXEMPT 并写明理由")
            .isEmpty();
    }

    @Test
    @DisplayName("只读接口的 handler 不得带 @Log")
    void readHandlersMustNotCarryLogAnnotation() {
        ScanResult scan = scanRestControllers();

        assertThat(scan.controllers())
            .as("必须扫到 %s 下的 @RestController，扫不到说明本测试退化成空断言", ROOT_PACKAGE)
            .isNotEmpty();

        List<String> readHandlers = new ArrayList<>();
        List<String> unexpected = new ArrayList<>();
        for (Class<?> controller : scan.controllers()) {
            for (Method method : methodsOf(controller)) {
                if (!isReadHandler(method)) {
                    continue;
                }
                readHandlers.add(keyOf(controller, method));
                if (AnnotatedElementUtils.hasAnnotation(method, Log.class)) {
                    unexpected.add(keyOf(controller, method));
                }
            }
        }

        assertThat(readHandlers)
            .as("一个只读 handler 都没扫到，本测试退化成空断言")
            .isNotEmpty();

        assertThat(unexpected)
            .as("以下只读接口带了 @Log：查询会被当成操作刷进审计库，与「只记状态变更」的取舍相悖")
            .isEmpty();
    }

    @Test
    @DisplayName("logType 必须全模块唯一")
    void logTypesMustBeGloballyUnique() {
        Map<String, List<String>> owners = new TreeMap<>();

        for (Class<?> controller : scanRestControllers().controllers()) {
            for (Method method : methodsOf(controller)) {
                Log annotation = AnnotatedElementUtils.findMergedAnnotation(method, Log.class);
                if (annotation == null) {
                    continue;
                }
                assertThat(annotation.logType())
                    .as("%s 的 @Log 必须显式声明 logType（留空会退化成 title_businessType 拼串）",
                        keyOf(controller, method))
                    .isNotBlank();
                owners.computeIfAbsent(annotation.logType(), key -> new ArrayList<>())
                    .add(keyOf(controller, method));
            }
        }

        assertThat(owners)
            .as("一个 @Log 都没扫到，本测试退化成空断言")
            .isNotEmpty();

        Map<String, List<String>> conflicts = new TreeMap<>();
        owners.forEach((logType, methodKeys) -> {
            if (methodKeys.size() > 1) {
                conflicts.put(logType, methodKeys);
            }
        });

        assertThat(conflicts)
            .as("同一 logType 被多个 handler 复用，审计库里无法据此区分操作")
            .isEmpty();
    }

    /**
     * 扫描 {@link #ROOT_PACKAGE}（含子包）下的全部 {@code @RestController}
     * <p>
     * 这里刻意不用 {@code ClassPathScanningCandidateComponentProvider}：本项目（Spring Framework 7）
     * 上它对同一组类返回空集合，会让断言静默退化成空断言；直接用资源解析 + 注解判定。
     * 之所以要递归（{@code **}）而不是只扫一层，是因为 {@code ApiJobImpl} 在 {@code apiimpl} 包，
     * 只扫 {@code controller} 包会漏掉整个服务间命名空间。
     * </p>
     *
     * @return 扫描结果（控制器 + 加载失败的类名）
     */
    private static ScanResult scanRestControllers() {
        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
            + ROOT_PACKAGE.replace('.', '/') + "/**/*.class";

        Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver().getResources(packageSearchPath);
        } catch (IOException e) {
            throw new AssertionError("扫描包失败: " + ROOT_PACKAGE, e);
        }

        List<Class<?>> controllers = new ArrayList<>();
        List<String> unloadable = new ArrayList<>();

        Arrays.stream(resources)
            .map(JobAuditAnnotationGuardTest::classNameOf)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .forEach(className -> {
                Class<?> type;
                try {
                    type = Class.forName(className);
                } catch (ClassNotFoundException | LinkageError e) {
                    // 缺依赖、模块未装配等都会落到这里；调用方会断言其中不含控制器
                    unloadable.add(className);
                    return;
                }
                if (AnnotatedElementUtils.hasAnnotation(type, RestController.class)) {
                    controllers.add(type);
                }
            });

        return new ScanResult(controllers, unloadable);
    }

    /**
     * 从资源 URL 反推二进制类名
     * <p>
     * 递归扫描下 {@code Resource#getFilename()} 只剩文件名，拿不到包路径，因此从 URL 里按
     * {@link #ROOT_PATH_MARKER} 截取。file: 与 jar: 两种 URL 都用 '/' 分隔，同一套解析通用。
     * </p>
     * <p>
     * 必须取<b>最后</b>一个匹配：jar URL 形如
     * {@code jar:file:/.../repository/com/carlos/job/carlos-job-client/3.0.0-SNAPSHOT/x.jar!/com/carlos/job/...}，
     * 本地仓库路径里同样含有 {@code com/carlos/job/}，取第一个会把仓库路径当成包名。
     * </p>
     *
     * @param resource 类资源
     * @return 二进制类名；非类资源或解析不出路径时返回 {@code null}
     */
    private static String classNameOf(Resource resource) {
        String filename = resource.getFilename();
        if (filename == null || !filename.endsWith(".class")) {
            return null;
        }
        String url;
        try {
            url = resource.getURL().toString();
        } catch (IOException e) {
            throw new AssertionError("解析资源 URL 失败: " + resource, e);
        }
        int index = url.lastIndexOf(ROOT_PATH_MARKER);
        if (index < 0) {
            return null;
        }
        String relativePath = url.substring(index + ROOT_PATH_MARKER.length());
        relativePath = relativePath.substring(0, relativePath.length() - ".class".length());
        return ROOT_PACKAGE + "." + relativePath.replace('/', '.');
    }

    /**
     * 取类自身声明的方法，跳过编译器生成的桥接 / 合成方法（它们可能重复携带注解）
     *
     * @param controller 控制器类
     * @return 方法列表
     */
    private static List<Method> methodsOf(Class<?> controller) {
        return Arrays.stream(controller.getDeclaredMethods())
            .filter(method -> !method.isBridge() && !method.isSynthetic())
            .toList();
    }

    /**
     * 是否为写 handler
     * <p>
     * 未显式声明 HTTP 方法的 {@code @RequestMapping} 按写处理（它同样能被 POST 命中），
     * 宁可多要求一个 {@code @Log}，也不要留一个静默漏检的口子。
     * </p>
     *
     * @param method 方法
     * @return true-写 handler
     */
    private static boolean isWriteHandler(Method method) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (mapping == null) {
            return false;
        }
        if (mapping.method().length == 0) {
            return true;
        }
        return Arrays.stream(mapping.method()).anyMatch(WRITE_METHODS::contains);
    }

    /**
     * 是否为只读 handler
     *
     * @param method 方法
     * @return true-只读 handler
     */
    private static boolean isReadHandler(Method method) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        return mapping != null && Arrays.asList(mapping.method()).contains(RequestMethod.GET);
    }

    private static List<String> unloadableControllerNames(ScanResult scan) {
        return scan.unloadable().stream()
            .filter(name -> name.endsWith("Controller"))
            .toList();
    }

    private static String keyOf(Class<?> controller, Method method) {
        return controller.getSimpleName() + "#" + method.getName();
    }

    /**
     * 扫描结果
     *
     * @param controllers 扫到的 {@code @RestController}
     * @param unloadable  加载失败（缺依赖 / 链接错误）的类名
     */
    private record ScanResult(List<Class<?>> controllers, List<String> unloadable) {
    }

}
