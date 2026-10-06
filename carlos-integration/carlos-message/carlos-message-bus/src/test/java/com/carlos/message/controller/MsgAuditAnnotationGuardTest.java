package com.carlos.message.controller;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * 审计注解守卫测试：把「哪些接口纳入审计、哪些刻意排除」钉死在代码里。
 * </p>
 * <p>
 * 为什么需要它：{@link Log} 只加注解就会生效（{@code LogAspect} 按方法注解织入），
 * 所以「漏加」不会编译报错、不会测试失败，只会让审计库永远缺这一条操作记录 —— 事后无人能察觉。
 * 反过来，只读接口被误加 {@code @Log} 则会把查询流量灌进 ClickHouse。因此这里断言：
 * </p>
 * <ol>
 *   <li>{@code com.carlos.message} 下所有 {@code @RestController}（含 {@code apiimpl} 的 Feign 实现）
 *       的 <b>写</b> handler（POST/PUT/DELETE/PATCH）都带 {@code @Log}，例外必须显式列进 {@link #EXEMPT}；</li>
 *   <li>所有 <b>只读</b> handler（GET/HEAD）都<b>不带</b> {@code @Log}；</li>
 *   <li>所有 {@code logType} 全模块唯一（空值、非 {@code MSG_} 前缀也一并拦下）。</li>
 * </ol>
 * <p>
 * 本测试刻意不起 Spring 上下文：纯反射 + 资源扫描，跑得快、无外部依赖。
 * 这里也不用 {@code ClassPathScanningCandidateComponentProvider}：本项目（Spring Framework 7）
 * 上它对同一组类返回空集合，会让下面的断言静默退化成空断言；直接用资源解析 + 注解判定，
 * 扫不到时会由显式的 {@code isNotEmpty()} 失败。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class MsgAuditAnnotationGuardTest {

    /**
     * 扫描根包：carlos-message 的控制器与 Feign 实现都必须在此之下
     */
    private static final String SCAN_PACKAGE = "com.carlos.message";

    /**
     * logType 命名前缀（全模块统一，见模块审计约定）
     */
    private static final String LOG_TYPE_PREFIX = "MSG_";

    /**
     * 只读 HTTP 方法：这些方法上出现 {@code @Log} 视为误标
     */
    private static final Set<RequestMethod> READ_METHODS = Set.of(RequestMethod.GET, RequestMethod.HEAD);

    /**
     * 写 HTTP 方法：这些方法上必须有 {@code @Log}，除非在 {@link #EXEMPT} 中
     */
    private static final Set<RequestMethod> WRITE_METHODS =
        Set.of(RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH);

    /**
     * 刻意不纳入审计的写接口，元素格式 {@code 简单类名#方法名}
     * <p>
     * 每一条都会在 {@link #exemptEntriesAreAccurateAndStillUnaudited()} 中被校验
     * 「确实存在、确实是写接口、确实没有 @Log」—— 类名或方法名写错会立刻失败，不会静默放宽约束。
     * </p>
     */
    private static final Set<String> EXEMPT = new LinkedHashSet<>(List.of(
        // 纯参数校验接口，不改变任何状态，无审计价值
        "MessageTemplateController#validate",

        // 未实现的桩：永远返回「批量发送功能待实现」，没有任何业务事实可记
        "MessageSendApiImpl#sendBatch",

        // 自动生成的记录表 CRUD 脚手架：记录/接收人/发送日志三张表本身就是消息发送过程的产物，
        // 真正的审计事实由「发送类」接口（MSG_SEND / MSG_SEND_ASYNC / MSG_SCHEDULE）承担
        "MessageReceiverController#add",
        "MessageReceiverController#delete",
        "MessageReceiverController#update",
        "MessageRecordController#add",
        "MessageRecordController#delete",
        "MessageRecordController#update",
        "MessageSendLogController#add",
        "MessageSendLogController#delete",
        "MessageSendLogController#update",

        // 只读查询：用 POST 承载分页参数只是契约选择（分页条件为嵌套对象），语义仍是查询
        "MessageQueryApiImpl#page"
    ));

    @Test
    void writeHandlersAreAuditedUnlessExplicitlyExempt() {
        Map<String, Method> writeHandlers = handlersMatching(WRITE_METHODS);

        assertThat(writeHandlers)
            .as("必须扫到 %s 下的写接口，扫不到说明本测试退化成空断言", SCAN_PACKAGE)
            .isNotEmpty();

        List<String> unaudited = writeHandlers.entrySet().stream()
            .filter(entry -> !EXEMPT.contains(entry.getKey()))
            .filter(entry -> !AnnotatedElementUtils.hasAnnotation(entry.getValue(), Log.class))
            .map(Map.Entry::getKey)
            .sorted()
            .toList();

        assertThat(unaudited)
            .as("下列写接口既没有 @Log 也不在 EXEMPT 清单里："
                + "要么补 @Log（会改变状态的操作必须留痕），要么在 EXEMPT 中写明为什么不审计")
            .isEmpty();
    }

    @Test
    void exemptEntriesAreAccurateAndStillUnaudited() {
        Map<String, Method> writeHandlers = handlersMatching(WRITE_METHODS);

        for (String exempt : EXEMPT) {
            assertThat(writeHandlers)
                .as("EXEMPT 中的 %s 不对应任何写接口：类名/方法名写错会让约束静默失效，请修正或删除该条目", exempt)
                .containsKey(exempt);
            assertThat(AnnotatedElementUtils.hasAnnotation(writeHandlers.get(exempt), Log.class))
                .as("%s 已经补上了 @Log，应从 EXEMPT 中移除，否则这条豁免会长期掩盖真实的审计缺口", exempt)
                .isFalse();
        }
    }

    @Test
    void readHandlersAreNeverAudited() {
        Map<String, Method> readHandlers = handlersMatching(READ_METHODS);

        assertThat(readHandlers)
            .as("必须扫到 %s 下的只读接口，扫不到说明本测试退化成空断言", SCAN_PACKAGE)
            .isNotEmpty();

        List<String> audited = readHandlers.entrySet().stream()
            .filter(entry -> AnnotatedElementUtils.hasAnnotation(entry.getValue(), Log.class))
            .map(Map.Entry::getKey)
            .sorted()
            .toList();

        assertThat(audited)
            .as("只读接口不产生操作审计：这些方法上的 @Log 会把查询流量灌进 ClickHouse，请删除")
            .isEmpty();
    }

    @Test
    void logTypesAreGloballyUniqueAndFollowTheNamingConvention() {
        Map<String, List<String>> owners = new TreeMap<>();
        List<String> duplicates = new ArrayList<>();

        for (Class<?> controller : restControllers()) {
            for (Method method : controller.getDeclaredMethods()) {
                Log log = AnnotatedElementUtils.findMergedAnnotation(method, Log.class);
                if (log == null) {
                    continue;
                }

                String owner = controller.getSimpleName() + "#" + method.getName();
                String logType = log.logType();

                assertThat(logType)
                    .as("%s 的 logType 不能为空：空值会让 LogAspect 回退成「title_businessType」拼装，"
                        + "破坏本模块的 %s 命名约定", owner, LOG_TYPE_PREFIX)
                    .isNotBlank();
                assertThat(logType)
                    .as("%s 的 logType 必须以 %s 开头（本模块统一约定）", owner, LOG_TYPE_PREFIX)
                    .startsWith(LOG_TYPE_PREFIX);

                List<String> current = owners.computeIfAbsent(logType, key -> new ArrayList<>());
                current.add(owner);
                if (current.size() > 1) {
                    duplicates.add(logType + " 同时被 " + String.join(" 与 ", current) + " 使用");
                }
            }
        }

        assertThat(owners)
            .as("必须扫到 @Log 注解，扫不到说明扫描退化成空断言")
            .isNotEmpty();
        assertThat(duplicates)
            .as("logType 是审计检索的关键维度，全模块必须唯一")
            .isEmpty();
    }

    @Test
    void scanCoversEveryRestControllerUnderTheScanPackage() {
        assertThat(restControllers())
            .extracting(Class::getSimpleName)
            .as("递归扫描必须覆盖 controller 与 apiimpl 两个包：只扫到其中一层会让守卫测试出现盲区")
            .contains(
                "MessageChannelController",
                "MessageTemplateController",
                "MessageTypeController",
                "MessageController",
                "MessageReadReceiptController",
                "MessageStatsController",
                "MessageReceiverController",
                "MessageRecordController",
                "MessageSendLogController",
                "MessageSendApiImpl",
                "MessageQueryApiImpl");
    }

    /**
     * 收集「HTTP 方法 + 类名#方法名 → Method」映射
     *
     * @param expectedMethods 关注的 HTTP 方法集合
     * @return 命中的 handler 方法，键为 {@code 简单类名#方法名}
     */
    private static Map<String, Method> handlersMatching(Set<RequestMethod> expectedMethods) {
        Map<String, Method> handlers = new LinkedHashMap<>();
        for (Class<?> controller : restControllers()) {
            for (Method method : controller.getDeclaredMethods()) {
                Set<RequestMethod> declared = requestMethodsOf(method);
                if (declared.isEmpty() || declared.stream().noneMatch(expectedMethods::contains)) {
                    continue;
                }
                handlers.put(controller.getSimpleName() + "#" + method.getName(), method);
            }
        }
        return handlers;
    }

    /**
     * 读取方法上（含组合注解）声明的 HTTP 方法
     *
     * @param method handler 方法
     * @return HTTP 方法集合，未声明映射时为空集合
     */
    private static Set<RequestMethod> requestMethodsOf(Method method) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (mapping == null || mapping.method().length == 0) {
            return Set.of();
        }
        return Set.of(mapping.method());
    }

    /**
     * 递归扫描 {@link #SCAN_PACKAGE} 下的所有 {@code @RestController}
     *
     * @return 控制器类列表（含 Feign 接口实现）
     */
    private static List<Class<?>> restControllers() {
        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
            + SCAN_PACKAGE.replace('.', '/') + "/**/*.class";

        Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver().getResources(packageSearchPath);
        } catch (IOException e) {
            throw new AssertionError("扫描包失败: " + SCAN_PACKAGE, e);
        }

        Set<Class<?>> controllers = new LinkedHashSet<>();
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null || !filename.endsWith(".class")) {
                continue;
            }
            Class<?> type = loadClass(toClassName(resource));
            if (AnnotatedElementUtils.hasAnnotation(type, RestController.class)) {
                controllers.add(type);
            }
        }
        return new ArrayList<>(controllers);
    }

    /**
     * 从资源 URL 反推全限定类名
     * <p>
     * 取包路径<b>最后</b>一次出现的位置：jar URL 形如
     * {@code <repo>/com/carlos/message/carlos-message-api/….jar!/com/carlos/message/api/X.class}，
     * 仓库路径里同样含 {@code com/carlos/message/}，用 {@code indexOf} 会截出错位的类名。
     * </p>
     *
     * @param resource 类文件资源
     * @return 全限定类名
     */
    private static String toClassName(Resource resource) {
        String url;
        try {
            url = resource.getURL().toString().replace('\\', '/');
        } catch (IOException e) {
            throw new AssertionError("读取资源 URL 失败: " + resource, e);
        }

        String packagePath = SCAN_PACKAGE.replace('.', '/') + "/";
        int start = url.lastIndexOf(packagePath);
        if (start < 0) {
            throw new AssertionError("无法从资源 URL 推断类名（缺少包路径 " + packagePath + "）: " + url);
        }

        String relative = url.substring(start, url.length() - ".class".length());
        return relative.replace('/', '.');
    }

    private static Class<?> loadClass(String className) {
        try {
            // initialize = false：只需要读注解，不必触发被测类的静态初始化
            return Class.forName(className, false, MsgAuditAnnotationGuardTest.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            throw new AssertionError("扫描到 " + className + " 但加载失败", e);
        }
    }
}
