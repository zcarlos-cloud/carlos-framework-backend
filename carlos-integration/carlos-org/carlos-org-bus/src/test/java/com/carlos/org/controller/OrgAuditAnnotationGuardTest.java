package com.carlos.org.controller;

import com.carlos.log.annotation.Log;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * 操作日志注解守护测试：把「写接口必须记审计、只读接口不得记审计」钉死在注解层。
 * </p>
 * <p>
 * 为什么需要它：{@code @Log} 是纯运行期注解，漏标或误标不会有任何编译期提示，
 * 也不会让任何已有测试变红——只能靠人肉 review。而审计链路（
 * {@code @Log → LogAspect → MQ(audit:log) → carlos-audit → ClickHouse}）一旦漏标，
 * 事后补不回来。因此这里断言：
 * </p>
 * <ol>
 *   <li>{@code com.carlos.org.controller} 与 {@code com.carlos.org.position.controller}
 *       下所有 {@code @RestController} 的非 GET handler（POST/PUT/DELETE/PATCH…）都带 {@code @Log}，
 *       除了 {@link #EXEMPT_WRITE_HANDLERS} 里显式列出的例外；</li>
 *   <li>所有 GET handler <b>不带</b> {@code @Log}，除了 {@link #EXEMPT_READ_HANDLERS_WITH_LOG}
 *       里显式列出的例外（导出类：GET 只是承载文件流，语义是数据外发）；</li>
 *   <li>{@code logType} 全局唯一、非空白、且统一以 {@code ORG_} 开头；</li>
 *   <li>两个豁免清单里的名字必须指向真实存在的 handler——否则豁免会变成「静默失效的墓碑」。</li>
 * </ol>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class OrgAuditAnnotationGuardTest {

    /**
     * 待扫描的控制器所在包（岗位相关的控制器单独放在 {@code position} 子域下）
     */
    private static final List<String> CONTROLLER_PACKAGES = List.of(
        "com.carlos.org.controller",
        "com.carlos.org.position.controller");

    /**
     * 本模块 {@code logType} 的强制前缀
     */
    private static final String LOG_TYPE_PREFIX = "ORG_";

    /**
     * 刻意<b>不</b>记审计的写接口：<b>新增写接口时不要往这里加，加 {@code @Log}。</b>
     * <ul>
     *   <li>{@code OrgUserController#previewImportUser} —— {@code import-preview} 只是把上传文件
     *       解析成预览行返回给前端确认，一行都不落库；真正的写入在
     *       {@code import-confirm}（{@code OrgUserController#confirmImportUser}，已标注
     *       {@code ORG_USER_IMPORT_CONFIRM}）。</li>
     *   <li>{@code OrgPositionHistoryController#{add,delete,update}} —— 岗位变更历史表是
     *       供业务侧写入的归档表（{@code ApiOrgPositionHistoryImpl} 才是它的调用方），
     *       不是管理员操作入口；且约定的 {@code targetType} 清单里没有 {@code ORG_POSITION_HISTORY}，
     *       标注它只会产生语义错误的审计记录。</li>
     * </ul>
     */
    private static final Set<String> EXEMPT_WRITE_HANDLERS = Set.of(
        "OrgUserController#previewImportUser",
        "OrgPositionHistoryController#add",
        "OrgPositionHistoryController#delete",
        "OrgPositionHistoryController#update");

    /**
     * 刻意记审计的 GET 接口（正常只读接口一律不得带 {@code @Log}）。
     * <p>
     * {@code OrgUserController#exportUser} 走 {@code GET /org/user/export} 只因为它要把
     * Excel 文件流写进 {@code HttpServletResponse}（{@code BusinessType.EXPORT}），
     * 语义上是数据外发而非查询——批量导出用户全量字段属于需要留痕的高风险动作，
     * 与 {@code import} 对称。除此之外再有 GET 带 {@code @Log} 都会让本测试失败。
     * </p>
     */
    private static final Set<String> EXEMPT_READ_HANDLERS_WITH_LOG = Set.of(
        "OrgUserController#exportUser");

    @Test
    @DisplayName("所有写接口（非 GET handler）都必须带 @Log，豁免清单除外")
    void writeEndpointsMustCarryLogAnnotation() {
        List<Class<?>> controllers = restControllers();
        assertThat(controllers)
            .as("必须扫到 %s 下的控制器，扫不到说明本测试退化成空断言", CONTROLLER_PACKAGES)
            .isNotEmpty();

        Set<String> missing = new TreeSet<>();
        for (Class<?> controller : controllers) {
            for (Method handler : writeHandlers(controller)) {
                String name = handlerName(controller, handler);
                if (hasLog(handler) || EXEMPT_WRITE_HANDLERS.contains(name)) {
                    continue;
                }
                missing.add(name);
            }
        }

        assertThat(missing)
            .as("这些写接口没有 @Log：审计链路是运行期注解驱动的，漏标不会有任何编译期提示。"
                + "确属「不该记审计」的接口，请带理由写进 EXEMPT_WRITE_HANDLERS")
            .isEmpty();
    }

    @Test
    @DisplayName("所有只读接口（GET handler）都不得带 @Log，导出类豁免除外")
    void readEndpointsMustNotCarryLogAnnotation() {
        List<Class<?>> controllers = restControllers();
        assertThat(controllers).isNotEmpty();

        Set<String> annotated = new TreeSet<>();
        for (Class<?> controller : controllers) {
            for (Method handler : readHandlers(controller)) {
                if (hasLog(handler)) {
                    annotated.add(handlerName(controller, handler));
                }
            }
        }

        assertThat(annotated)
            .as("GET handler 默认是只读接口，标 @Log 会把查询量灌进审计库（ClickHouse）。"
                + "确需留痕的导出类接口，请带理由写进 EXEMPT_READ_HANDLERS_WITH_LOG")
            .containsExactlyInAnyOrderElementsOf(EXEMPT_READ_HANDLERS_WITH_LOG);
    }

    @Test
    @DisplayName("@Log 的 logType 必须全局唯一、非空白且以 ORG_ 开头")
    void logTypesMustBeOrgPrefixedAndGloballyUnique() {
        List<Class<?>> controllers = restControllers();
        assertThat(controllers).isNotEmpty();

        Map<String, String> logTypeOwners = new LinkedHashMap<>();
        Map<String, String> duplicated = new LinkedHashMap<>();
        Set<String> malformed = new TreeSet<>();

        for (Class<?> controller : controllers) {
            for (Method method : controller.getDeclaredMethods()) {
                Log log = logOf(method);
                if (log == null) {
                    continue;
                }
                String handler = handlerName(controller, method);

                assertThat(log.title())
                    .as("%s 的 @Log 必须写 title（LogAspect 用 logType，缺 title 时回退 title_businessType）", handler)
                    .isNotBlank();

                String logType = log.logType();
                if (logType.isBlank() || !logType.startsWith(LOG_TYPE_PREFIX)) {
                    malformed.add(handler + " -> " + logType);
                    continue;
                }

                String previous = logTypeOwners.put(logType, handler);
                if (previous != null) {
                    duplicated.put(logType, previous + " / " + handler);
                }
            }
        }

        assertThat(logTypeOwners)
            .as("必须扫到 @Log 标注，扫不到说明测试退化成空断言")
            .isNotEmpty();
        assertThat(malformed)
            .as("logType 必须非空白且以 %s 开头（本模块统一命名约定）", LOG_TYPE_PREFIX)
            .isEmpty();
        assertThat(duplicated)
            .as("logType 是审计库里的操作类型标识，重复会让同一串值指向多个接口")
            .isEmpty();
    }

    @Test
    @DisplayName("豁免清单里的名字必须指向真实存在的 handler，避免豁免静默失效")
    void exemptEntriesMustReferenceRealHandlers() {
        Set<String> allHandlers = new LinkedHashSet<>();
        for (Class<?> controller : restControllers()) {
            for (Method method : controller.getDeclaredMethods()) {
                if (isMapped(method)) {
                    allHandlers.add(handlerName(controller, method));
                }
            }
        }
        assertThat(allHandlers).isNotEmpty();

        Set<String> stale = new LinkedHashSet<>();
        stale.addAll(EXEMPT_WRITE_HANDLERS);
        stale.addAll(EXEMPT_READ_HANDLERS_WITH_LOG);
        stale.removeAll(allHandlers);

        assertThat(stale)
            .as("这些豁免项已不存在对应的 handler（接口删了或改了方法名），请从豁免清单里删掉")
            .isEmpty();
    }

    /**
     * 扫描约定包下的 {@code @RestController}
     * <p>
     * 与 {@code WorkflowNamespaceBoundaryTest} 同样刻意不用
     * {@code ClassPathScanningCandidateComponentProvider}：本项目（Spring Framework 7）上
     * 它对同一组类返回空集合，会让断言静默退化成空断言；这里直接用资源解析 + 注解判定，
     * 扫不到时由调用方的 {@code isNotEmpty()} 显式失败。
     * </p>
     *
     * @return 控制器类列表
     */
    private static List<Class<?>> restControllers() {
        List<Class<?>> controllers = new ArrayList<>();
        for (String basePackage : CONTROLLER_PACKAGES) {
            controllers.addAll(restControllersIn(basePackage));
        }
        return controllers;
    }

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
            .map(OrgAuditAnnotationGuardTest::loadClass)
            .filter(type -> AnnotatedElementUtils.hasAnnotation(type, RestController.class))
            .toList();
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("扫描到的类加载不到: " + className, e);
        }
    }

    /**
     * 写 handler：有请求映射但不是 GET 的方法（POST/PUT/DELETE/PATCH 或自定义方法）
     */
    private static List<Method> writeHandlers(Class<?> controller) {
        return mappedHandlers(controller).stream().filter(handler -> !isRead(handler)).toList();
    }

    /**
     * 只读 handler：GET 映射的方法
     */
    private static List<Method> readHandlers(Class<?> controller) {
        return mappedHandlers(controller).stream().filter(OrgAuditAnnotationGuardTest::isRead).toList();
    }

    private static List<Method> mappedHandlers(Class<?> controller) {
        List<Method> handlers = new ArrayList<>();
        for (Method method : controller.getDeclaredMethods()) {
            if (isMapped(method)) {
                handlers.add(method);
            }
        }
        return handlers;
    }

    private static boolean isMapped(Method method) {
        return AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class) != null;
    }

    private static boolean isRead(Method method) {
        return AnnotatedElementUtils.findMergedAnnotation(method, GetMapping.class) != null;
    }

    private static boolean hasLog(Method method) {
        return logOf(method) != null;
    }

    private static Log logOf(Method method) {
        return AnnotatedElementUtils.findMergedAnnotation(method, Log.class);
    }

    private static String handlerName(Class<?> controller, Method method) {
        return controller.getSimpleName() + "#" + method.getName();
    }

}
