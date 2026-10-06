package com.carlos.core.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * 框架层 Bean 注册约定守卫测试
 * </p>
 *
 * <p><b>约定</b>：{@code carlos-spring-boot/**} 是 starter（自动配置模块），不得用类级
 * {@code @Component} / {@code @Service} / {@code @Repository} 注册 Bean，必须由自动配置类里的
 * {@code @Bean} + {@code @ConditionalOnMissingBean} 注册。</p>
 *
 * <p><b>为什么</b>：starter 不参与宿主应用的组件扫描（扫描根是宿主自己的包）。类上加 stereotype
 * 注解的后果随宿主而异 —— 宿主扫描根不覆盖该包时 <b>Bean 根本不存在</b>；覆盖时又会绕过
 * {@code @ConditionalOn*} 条件生成一份「完全不过条件」的实例，与服务开关冲突甚至让整个服务起不来。
 * 两种情况都只能在运行期暴露，编译期毫无提示，故用本测试钉死。</p>
 *
 * <p><b>控制器的例外</b>：{@code @RestController} / {@code @Controller} 是 MVC 语义必需
 * （{@code @ResponseBody} + handler 识别），不能一概禁止；但同样必须由 {@code @Bean} 注册，
 * 不得指望被扫描到（参见 {@code OssController}：@Bean 注册 + 类级 {@code @ConditionalOnProperty}
 * 兜住扫描重复注册）。本测试的第二个用例断言这一点。</p>
 *
 * <p>直接扫源码树而非 classpath：单个模块的测试类路径看不到其它 starter 的类，而本约定是跨模块的。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
class FrameworkBeanRegistrationGuardTest {

    /**
     * 被扫描的框架层根目录（相对后端仓库根）
     */
    private static final String FRAMEWORK_DIR = "carlos-spring-boot";

    /**
     * 禁用的 Bean 注册型 stereotype 注解（简单名）
     */
    private static final Set<String> FORBIDDEN_STEREOTYPES = Set.of("Component", "Service", "Repository");

    /**
     * 控制器注解（MVC 语义必需，但必须由 @Bean 注册）
     */
    private static final Set<String> CONTROLLER_ANNOTATIONS = Set.of("RestController", "Controller");

    @Test
    @DisplayName("框架层不得用类级 @Component/@Service/@Repository 注册 Bean")
    void framework_layer_should_not_use_stereotype_annotations() throws IOException {
        Path repoRoot = backendRoot();
        List<String> violations = new ArrayList<>();

        for (Path java : javaFiles(repoRoot.resolve(FRAMEWORK_DIR))) {
            for (String annotation : classLevelAnnotations(java)) {
                String simple = simpleName(annotation);
                if (FORBIDDEN_STEREOTYPES.contains(simple)) {
                    violations.add(relative(repoRoot, java) + " -> @" + simple);
                }
            }
        }

        assertThat(violations)
            .as("框架层（%s/**）不得用类级 @Component/@Service/@Repository 注册 Bean；"
                + "请改为在自动配置里用 @Bean + @ConditionalOnMissingBean 注册"
                + "（starter 不参与宿主组件扫描，用注解会漏注册或被无条件重复注册）", FRAMEWORK_DIR)
            .isEmpty();
    }

    @Test
    @DisplayName("框架层控制器必须由 @Bean 注册，不得依赖组件扫描")
    void framework_layer_controllers_must_be_bean_registered() throws IOException {
        Path repoRoot = backendRoot();
        List<String> violations = new ArrayList<>();

        for (Path java : javaFiles(repoRoot.resolve(FRAMEWORK_DIR))) {
            boolean isController = classLevelAnnotations(java).stream()
                .map(this::simpleName)
                .anyMatch(CONTROLLER_ANNOTATIONS::contains);
            if (!isController) {
                continue;
            }
            String className = java.getFileName().toString().replace(".java", "");
            // 同一个模块的 main 源码里应能找到 new XxxController( —— 即由 @Bean 装配
            if (!moduleMainSourcesContain(java, "new " + className + "(")) {
                violations.add(relative(repoRoot, java) + " -> @" + className
                    + " 未见 @Bean 装配（new " + className + "( )");
            }
        }

        assertThat(violations)
            .as("框架层控制器必须由自动配置 @Bean 注册（@RestController 是 @Component 元注解，"
                + "不能指望被宿主扫到；有开关的还应在类上加 @ConditionalOn* 兜住扫描重复注册）")
            .isEmpty();
    }

    // ==================== 源码扫描工具 ====================

    /**
     * 从当前工作目录向上定位后端仓库根（含 carlos-spring-boot 目录的那一级）
     */
    private Path backendRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        while (dir != null) {
            if (Files.isDirectory(dir.resolve(FRAMEWORK_DIR))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("未找到仓库根（含 " + FRAMEWORK_DIR + " 的目录），当前工作目录=" + Paths.get("").toAbsolutePath());
    }

    /**
     * 收集框架层各模块 src/main/java 下的全部 .java
     */
    private List<Path> javaFiles(Path frameworkDir) throws IOException {
        if (!Files.isDirectory(frameworkDir)) {
            throw new IllegalStateException("框架层目录不存在：" + frameworkDir);
        }
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(frameworkDir)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> p.toString().replace('\\', '/').contains("/src/main/java/"))
                .forEach(files::add);
        }
        return files;
    }

    /**
     * 取一个源文件里所有「类声明上方」的注解（逐字面量，含参数）。
     *
     * <p>做法：定位 {@code class} 声明行，向上回扫连续的注解行与注释行；
     * 只有「去掉缩进后以 {@code @} 开头」的行才算注解 —— 这样 Javadoc 里的
     * {@code * @Service} 示例不会被误判。</p>
     */
    private List<String> classLevelAnnotations(Path java) throws IOException {
        List<String> lines = Files.readAllLines(java, StandardCharsets.UTF_8);
        List<String> annotations = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).matches("^\\s*(public\\s+)?(final\\s+|abstract\\s+|sealed\\s+|non-sealed\\s+)*class\\s.*")) {
                continue;
            }
            for (int j = i - 1; j >= 0; j--) {
                String trimmed = lines.get(j).trim();
                if (trimmed.isEmpty() || trimmed.startsWith("*") || trimmed.startsWith("/*")
                    || trimmed.startsWith("//") || trimmed.startsWith("*/")) {
                    continue;
                }
                if (trimmed.startsWith("@")) {
                    annotations.add(trimmed);
                    continue;
                }
                break;
            }
        }
        return annotations;
    }

    /**
     * 同一模块的 main 源码里是否出现指定片段（用于判断是否由 @Bean 装配）
     */
    private boolean moduleMainSourcesContain(Path java, String needle) throws IOException {
        Path moduleRoot = moduleRoot(java);
        if (moduleRoot == null) {
            return false;
        }
        try (Stream<Path> walk = Files.walk(moduleRoot)) {
            List<Path> sources = walk.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> p.toString().replace('\\', '/').contains("/src/main/java/"))
                .toList();
            for (Path p : sources) {
                if (Files.readString(p, StandardCharsets.UTF_8).contains(needle)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 从源文件路径反推所属模块根（含 src/main/java 的那一级）
     */
    private Path moduleRoot(Path java) {
        Path dir = java;
        while (dir != null) {
            if (Files.isDirectory(dir.resolve("src/main/java"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        return null;
    }

    /**
     * 注解字面量取简单名：{@code @Component("x")} -> {@code Component}
     */
    private String simpleName(String annotation) {
        String name = annotation.substring(1);
        int cut = name.indexOf('(');
        if (cut >= 0) {
            name = name.substring(0, cut);
        }
        name = name.trim();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : name;
    }

    /**
     * 相对路径（统一用 / 分隔，便于阅读）
     */
    private String relative(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }
}
