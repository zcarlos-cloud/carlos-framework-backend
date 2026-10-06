package com.carlos.system.controller;

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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * 审计标注守护测试：把 {@code carlos-system-bus} 的操作审计覆盖面钉死在「写接口必须有 {@code @Log}、
 * 只读接口必须没有」这条线上。
 * </p>
 * <p>
 * 为什么需要它：{@code @Log} 是纯注解，漏标、错标、以及把日志挂到只读接口上都不会有任何编译期提示；
 * 而审计是事后追责的唯一凭据，漏一个删除接口就等于漏一条证据链。因此这里断言：
 * </p>
 * <ol>
 *   <li>{@code com.carlos.system} 下所有 {@code @RestController} 的写 handler
 *       （{@code POST} / {@code PUT} / {@code DELETE} / {@code PATCH}）都带 {@code @Log}，
 *       例外只能写进 {@link #EXEMPT} 显式登记；</li>
 *   <li>只读 handler（{@code GET}）都<b>不带</b> {@code @Log}，例外只能写进 {@link #AUDITED_GETS} 显式登记；</li>
 *   <li>{@code logType} 全模块唯一，且遵循 {@code SYS_} 前缀命名约定；</li>
 *   <li>上述两张例外清单不得过期（列了但代码里已不存在、或实际情况已变）。</li>
 * </ol>
 * <p>
 * 刻意不起 Spring 上下文：本项目（Spring Framework 7）上
 * {@code ClassPathScanningCandidateComponentProvider} 会返回空集合，从而让断言静默退化成空断言，
 * 所以直接用资源解析 + 注解判定，扫不到时由 {@code isNotEmpty()} 显式失败。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class SysAuditAnnotationGuardTest {

    /**
     * 被扫描的根包
     */
    private static final String BASE_PACKAGE = "com.carlos.system";

    /**
     * 类文件路径片段，用于把资源 URL 反解成类名
     */
    private static final String BASE_PATH = "com/carlos/system/";

    private static final String CLASS_SUFFIX = ".class";

    /**
     * 解压到目录的类文件才可被反解成类名；jar 内条目无法这样还原
     */
    private static final String FILE_PROTOCOL = "file:";

    private static final String JAR_ENTRY_SEPARATOR = "!/";

    /**
     * {@code logType} 命名前缀：本模块统一 {@code SYS_}
     */
    private static final String LOG_TYPE_PREFIX = "SYS_";

    /**
     * 写操作 HTTP 方法：这些 handler 必须带 {@code @Log}
     */
    private static final Set<RequestMethod> WRITE_METHODS = EnumSet.of(
        RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH);

    /**
     * 刻意不标注审计的写接口（{@code 类名#方法名}）
     * <p>
     * 两类端点：
     * </p>
     * <p>
     * 1. {@code *apiimpl} 包：这些端点是<b>服务间 Feign 调用</b>契约的实现，
     * 调用方是其它微服务（业务链路的一环），不是管理员在后台点出来的动作。
     * 审计的主体、来源 IP、可信度都和管理端操作不是一回事，硬标只会把审计库灌成噪音，
     * 所以整体排除；同时下游服务自己的一次管理操作在各自模块里已有审计记录。
     * </p>
     * <p>
     * 2. {@code SysFavoriteController} 的收藏写接口：收藏/取消收藏/覆盖收藏是<b>用户私有偏好</b>的
     * 高频切换动作，数据按 user_id 强隔离、只能操作自己的行，打审计日志只会制造噪音，
     * 与「只读查询不审计」同理。
     * </p>
     * <ul>
     *   <li>{@code ApiSysMenuImpl#listMenus} —— 按 id 集合批量查菜单，POST 只是因为入参是集合；</li>
     *   <li>{@code ApiRegionImpl#addRegion} —— 服务间同步区域数据；</li>
     *   <li>{@code ApiFileImpl#upload/#delete/#deleteBatch/#update} —— 服务间文件读写，
     *       管理端的同功能接口在 {@code FileController} 里已标注。</li>
     *   <li>{@code SysFavoriteController#add/#remove/#replace} —— 用户私有偏好的高频收藏切换，不打审计。</li>
     * </ul>
     */
    private static final Set<String> EXEMPT = Set.of(
        "ApiSysMenuImpl#listMenus",
        "ApiRegionImpl#addRegion",
        "ApiFileImpl#upload",
        "ApiFileImpl#delete",
        "ApiFileImpl#deleteBatch",
        "ApiFileImpl#update",
        "SysFavoriteController#add",
        "SysFavoriteController#remove",
        "SysFavoriteController#replace");

    /**
     * {@code GET} 形态、但业务上是「导出」或「批量写库」的接口（{@code 类名#方法名}）
     * <p>
     * 它们必须被审计，却不该为了一条日志去改 HTTP 动词——动词是前端已依赖的接口契约，
     * 为了标注而改动词属于破坏性变更。因此在这里显式登记为「允许带 {@code @Log} 的 GET」：
     * 除这三个之外，任何 {@code GET} 长出 {@code @Log} 都会让
     * {@link #noReadHandlerCarriesAuditAnnotation()} 失败，逼人显式登记并说清理由。
     * </p>
     * <ul>
     *   <li>{@code SysConfigController#export} —— 导出系统配置（当前筛选结果）；</li>
     *   <li>{@code SysRegionController#exportUser} —— 导出区域数据；</li>
     *   <li>{@code SysDictController#initWithTxt} —— 从 txt 文件批量初始化字典，是写操作。</li>
     * </ul>
     */
    private static final Set<String> AUDITED_GETS = Set.of(
        "SysConfigController#export",
        "SysRegionController#exportUser",
        "SysDictController#initWithTxt");

    @Test
    void everyWriteHandlerIsAudited() {
        List<Handler> handlers = handlers();

        assertThat(handlers)
            .as("必须扫到 %s 下的控制器，扫不到说明本测试退化成空断言", BASE_PACKAGE)
            .isNotEmpty();

        List<String> missing = new ArrayList<>();
        for (Handler handler : handlers) {
            if (!handler.isWrite() || EXEMPT.contains(handler.key())) {
                continue;
            }
            if (handler.audit() == null) {
                missing.add(handler.describe());
            }
        }

        assertThat(missing)
            .as("以下写接口缺少 @Log：审计是事后追责的唯一凭据，新增写接口请补注解；"
                + "确实不该审计的，写进本测试的 EXEMPT 并说明理由")
            .isEmpty();
    }

    @Test
    void noReadHandlerCarriesAuditAnnotation() {
        List<String> unexpected = new ArrayList<>();
        for (Handler handler : handlers()) {
            if (!handler.isRead() || AUDITED_GETS.contains(handler.key())) {
                continue;
            }
            if (handler.audit() != null) {
                unexpected.add(handler.describe());
            }
        }

        assertThat(unexpected)
            .as("以下只读接口带了 @Log：只读查询会把审计库灌成噪音，请去掉注解；"
                + "若它其实是写操作（导出/批量写库），写进本测试的 AUDITED_GETS 并说明理由")
            .isEmpty();
    }

    @Test
    void everyLogTypeIsUniqueAndWellFormed() {
        Map<String, String> logTypeToHandler = new TreeMap<>();
        List<String> malformed = new ArrayList<>();
        List<String> duplicated = new ArrayList<>();

        for (Handler handler : handlers()) {
            Log audit = handler.audit();
            if (audit == null) {
                continue;
            }
            if (audit.logType().isBlank() || !audit.logType().startsWith(LOG_TYPE_PREFIX) || audit.title().isBlank()) {
                malformed.add(handler.describe() + " -> logType=" + audit.logType() + ", title=" + audit.title());
                continue;
            }
            String previous = logTypeToHandler.put(audit.logType(), handler.key());
            if (previous != null) {
                duplicated.add(audit.logType() + " 同时出现在 " + previous + " 与 " + handler.key());
            }
        }

        assertThat(logTypeToHandler)
            .as("必须至少标出一个 @Log，否则本测试退化成空断言")
            .isNotEmpty();
        assertThat(malformed)
            .as("logType 必须以 %s 开头且 title 不得为空", LOG_TYPE_PREFIX)
            .isEmpty();
        assertThat(duplicated)
            .as("logType 必须在全模块内唯一：审计检索按 logType 聚合，重复会让统计和追溯串味")
            .isEmpty();
    }

    @Test
    void exemptionsAreNotStale() {
        Map<String, Handler> byKey = new LinkedHashMap<>();
        for (Handler handler : handlers()) {
            byKey.put(handler.key(), handler);
        }

        List<String> staleExempt = new ArrayList<>();
        for (String key : EXEMPT) {
            Handler handler = byKey.get(key);
            if (handler == null || !handler.isWrite() || handler.audit() != null) {
                staleExempt.add(key);
            }
        }

        List<String> staleAuditedGets = new ArrayList<>();
        for (String key : AUDITED_GETS) {
            Handler handler = byKey.get(key);
            if (handler == null || !handler.isRead() || handler.audit() == null) {
                staleAuditedGets.add(key);
            }
        }

        assertThat(staleExempt)
            .as("EXEMPT 里的接口必须真实存在、是写方法且确实没标 @Log；"
                + "接口已改动词/已补注解后，请把这条豁免删掉")
            .isEmpty();
        assertThat(staleAuditedGets)
            .as("AUDITED_GETS 里的接口必须真实存在、是 GET 且确实标了 @Log；"
                + "动词改成 POST 之后，请把这条登记删掉")
            .isEmpty();
    }

    @Test
    void handlersDeclareExplicitHttpMethods() {
        List<String> methodAgnostic = new ArrayList<>();
        for (Handler handler : handlers()) {
            if (handler.methods().isEmpty()) {
                methodAgnostic.add(handler.describe());
            }
        }

        assertThat(methodAgnostic)
            .as("本模块的 handler 一律显式声明 HTTP 动词（@GetMapping/@PostMapping 等）；"
                + "method-agnostic 的 @RequestMapping 无法被本测试归类，会让上面几条断言静默漏判")
            .isEmpty();
    }

    /**
     * 收集被扫描包下所有控制器的全部 handler（含 {@code apiimpl}）
     *
     * @return handler 列表
     */
    private static List<Handler> handlers() {
        List<Handler> handlers = new ArrayList<>();
        for (Class<?> controller : restControllers()) {
            String basePath = basePathOf(controller);
            for (Method method : controller.getDeclaredMethods()) {
                if (method.isSynthetic() || method.isBridge() || Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null) {
                    continue;
                }
                handlers.add(new Handler(
                    controller.getSimpleName() + "#" + method.getName(),
                    join(basePath, firstPath(mapping)),
                    requestMethodsOf(mapping),
                    AnnotatedElementUtils.findMergedAnnotation(method, Log.class)));
            }
        }
        return handlers;
    }

    /**
     * 扫描 {@link #BASE_PACKAGE} 下的全部 {@code @RestController}
     *
     * @return 控制器类列表
     */
    private static List<Class<?>> restControllers() {
        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
            + BASE_PACKAGE.replace('.', '/') + "/**/*.class";

        Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver().getResources(packageSearchPath);
        } catch (IOException e) {
            throw new AssertionError("扫描包失败: " + BASE_PACKAGE, e);
        }

        Set<String> classNames = new LinkedHashSet<>();
        for (Resource resource : resources) {
            String className = classNameOf(resource);
            if (className != null) {
                classNames.add(className);
            }
        }

        List<Class<?>> controllers = new ArrayList<>();
        for (String className : classNames) {
            Class<?> type = loadClass(className);
            if (AnnotatedElementUtils.hasAnnotation(type, RestController.class)) {
                controllers.add(type);
            }
        }
        return controllers;
    }

    private static String classNameOf(Resource resource) {
        String filename = resource.getFilename();
        if (filename == null || !filename.endsWith(CLASS_SUFFIX) || filename.contains("$")) {
            return null;
        }
        String url;
        try {
            url = resource.getURL().toString();
        } catch (IOException e) {
            throw new AssertionError("解析类文件路径失败: " + resource.getDescription(), e);
        }
        // 只看解压到目录里的类：jar 内条目的 URL 形如
        // jar:file:/.../carlos-system/carlos-system-api/3.0.0-SNAPSHOT/xxx.jar!/com/carlos/system/...
        // 既会撞上本地仓库的 com/carlos/system 目录，本身也不含本模块的控制器
        if (!url.startsWith(FILE_PROTOCOL) || url.contains(JAR_ENTRY_SEPARATOR)) {
            return null;
        }
        int index = url.lastIndexOf(BASE_PATH);
        if (index < 0) {
            return null;
        }
        String relative = url.substring(index + BASE_PATH.length());
        return BASE_PACKAGE + "."
            + relative.substring(0, relative.length() - CLASS_SUFFIX.length()).replace('/', '.');
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("扫描到的类加载不到: " + className, e);
        }
    }

    private static String basePathOf(Class<?> controller) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
        return mapping == null ? "" : firstPath(mapping);
    }

    private static Set<RequestMethod> requestMethodsOf(RequestMapping mapping) {
        RequestMethod[] methods = mapping.method();
        return methods.length == 0 ? Set.of() : EnumSet.copyOf(Arrays.asList(methods));
    }

    private static String firstPath(RequestMapping mapping) {
        return mapping.path().length == 0 ? "" : mapping.path()[0];
    }

    private static String join(String basePath, String methodPath) {
        if (methodPath.isEmpty()) {
            return basePath.isEmpty() ? "/" : basePath;
        }
        String prefix = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
        return methodPath.startsWith("/") ? prefix + methodPath : prefix + "/" + methodPath;
    }

    /**
     * 一个被审计关注的方法端点
     *
     * @param key     形如 {@code SysMenuController#delete} 的稳定标识
     * @param path    完整路径，仅用于失败信息
     * @param methods 声明的 HTTP 动词；为空表示 handler 未显式声明动词
     * @param audit   方法上的 {@code @Log}，未标注时为 {@code null}
     */
    private record Handler(String key, String path, Set<RequestMethod> methods, Log audit) {

        boolean isWrite() {
            return methods.stream().anyMatch(WRITE_METHODS::contains);
        }

        boolean isRead() {
            return methods.contains(RequestMethod.GET);
        }

        String describe() {
            return key + " (" + methods + " " + path + ")";
        }
    }
}
