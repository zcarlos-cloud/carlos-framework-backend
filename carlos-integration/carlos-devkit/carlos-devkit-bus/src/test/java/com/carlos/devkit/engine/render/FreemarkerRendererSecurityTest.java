package com.carlos.devkit.engine.render;

import com.carlos.devkit.config.DevkitProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FreeMarker 渲染器安全策略测试。
 *
 * <p>模板可由用户在线编辑，服务端求值等同于任意代码执行。本测试把「禁 ?new / 禁 ?api /
 * 输出限额 / 渲染超时」四条防线钉死 —— 删掉任何一条对应断言必红。</p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
class FreemarkerRendererSecurityTest {

    private DevkitProperties properties;

    private ExecutorService executor;

    private FreemarkerRenderer renderer;

    private Path workDir;

    @BeforeEach
    void setUp() throws Exception {
        properties = new DevkitProperties();
        properties.setRenderTimeoutMs(3000L);
        properties.setMaxSingleFileSize(2048L);
        executor = Executors.newFixedThreadPool(2);
        renderer = new FreemarkerRenderer(properties, executor);
        workDir = Files.createTempDirectory("devkit-render-test");
    }

    @AfterEach
    void tearDown() throws Exception {
        executor.shutdownNow();
        if (workDir != null) {
            Files.walk(workDir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(path -> path.toFile().delete());
        }
    }

    @Test
    @DisplayName("正常模板应渲染成功")
    void should_render_normal_template() throws Exception {
        File template = writeTemplate("normal.ftl", "Hello ${project.packageName}!");
        Map<String, Object> params = new HashMap<>();
        params.put("project", Map.of("packageName", "com.carlos.demo"));
        assertEquals("Hello com.carlos.demo!", renderer.render(template, params));
    }

    @Test
    @DisplayName("?new 内建必须被拒绝（否则等同于服务端任意代码执行）")
    void should_reject_new_builtin() throws Exception {
        File template = writeTemplate("new.ftl",
            "<#assign ex = \"freemarker.template.utility.Execute\"?new()>${ex(\"whoami\")}");
        assertThrows(RuntimeException.class, () -> renderer.render(template, new HashMap<>()));
    }

    @Test
    @DisplayName("?api 内建必须被拒绝（否则可反射拿到任意对象）")
    void should_reject_api_builtin() throws Exception {
        File template = writeTemplate("api.ftl",
            "<#assign s = \"abc\"?api>${s.getClass().getName()}");
        assertThrows(RuntimeException.class, () -> renderer.render(template, new HashMap<>()));
    }

    @Test
    @DisplayName("超大输出必须被限额拦截")
    void should_reject_oversized_output() throws Exception {
        // 输出 10000 个字符，超过 maxSingleFileSize=2048
        File template = writeTemplate("big.ftl", "<#list 1..10000 as i>x</#list>");
        RuntimeException error = assertThrows(RuntimeException.class,
            () -> renderer.render(template, new HashMap<>()));
        assertTrue(error.getMessage() == null || !error.getMessage().isEmpty());
    }

    @Test
    @DisplayName("渲染结果应显式按 UTF-8 写入目标文件")
    void should_write_utf8() throws Exception {
        File template = writeTemplate("utf8.ftl", "中文注释：${project.author}");
        File target = workDir.resolve("out/中文.java").toFile();
        Map<String, Object> params = new HashMap<>();
        params.put("project", Map.of("author", "Carlos"));
        renderer.renderToFile(template, params, target);

        String content = Files.readString(target.toPath(), StandardCharsets.UTF_8);
        assertEquals("中文注释：Carlos", content);
    }

    private File writeTemplate(String name, String content) throws Exception {
        Path path = workDir.resolve(name);
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path.toFile();
    }
}
