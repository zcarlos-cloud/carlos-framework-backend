package com.carlos.devkit.engine;

import cn.hutool.core.io.FileUtil;
import com.carlos.devkit.config.DevkitProperties;
import com.carlos.devkit.engine.model.GeneratedFile;
import com.carlos.devkit.engine.model.GeneratedResult;
import com.carlos.devkit.engine.model.TemplateGroupMeta;
import com.carlos.devkit.engine.model.TemplateSourceFile;
import com.carlos.devkit.engine.projectgen.ProjectGenEngine;
import com.carlos.devkit.engine.projectgen.ProjectGenParam;
import com.carlos.devkit.engine.render.FreemarkerRenderer;
import com.carlos.devkit.engine.support.TemplateMaterializer;
import com.carlos.devkit.support.Sandbox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工程生成引擎端到端测试。
 *
 * <p>直接用 carlos-tools 的真实模板资源跑完整流水线（物化 → 渲染 → 收集），
 * 验证「模板入库 + 引擎 Web 化」没有破坏原有产物语义。不依赖数据库。</p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
class ProjectGenEngineEndToEndTest {

    private DevkitProperties properties;

    private ExecutorService executor;

    private Sandbox sandbox;

    private ProjectGenEngine engine;

    private Path tmpRoot;

    @BeforeEach
    void setUp() throws Exception {
        tmpRoot = java.nio.file.Files.createTempDirectory("devkit-projectgen-test");
        properties = new DevkitProperties();
        properties.setRenderTimeoutMs(10_000L);
        properties.setTmpRoot(tmpRoot.toString());
        executor = Executors.newFixedThreadPool(2);
        sandbox = new Sandbox(properties);
        engine = new ProjectGenEngine(properties, new TemplateMaterializer(),
            new FreemarkerRenderer(properties, executor));
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        FileUtil.del(tmpRoot.toFile());
    }

    @Test
    @DisplayName("应从真实模板生成可用的 Spring Boot 工程骨架")
    void should_generate_springboot_scaffold() {
        File templateRoot = locateResource("projectge/project-templates/carlos-springboot/template");
        assertTrue(templateRoot.isDirectory(), "模板资源目录不存在: " + templateRoot);

        List<TemplateSourceFile> templates = readTemplates(templateRoot);

        ProjectGenParam param = new ProjectGenParam()
            .setArtifactId("demo-service")
            .setGroupId("com.carlos.demo")
            .setPackageNameItems(List.of("com", "carlos", "demo"))
            .setVersion("1.0.0-SNAPSHOT")
            .setDescribe("单元测试产物")
            .setAuthor("Carlos");
        // 故意不设 packageName，验证引擎能由 groupId 兜底还原

        Path work = sandbox.create();
        try {
            GeneratedResult result = engine.generate(groupMeta(), "template", templates, param, work);
            List<String> paths = result.getFiles().stream().map(GeneratedFile::getPath).toList();

            assertFalse(paths.isEmpty(), "产物不应为空");
            // 目录占位符必须全部被替换掉
            assertTrue(paths.stream().noneMatch(path -> path.contains("[ArtifactId]")),
                "路径中的 [ArtifactId] 未替换: " + paths);
            assertTrue(paths.stream().noneMatch(path -> path.contains("[Package]")),
                "路径中的 [Package] 未替换: " + paths);
            assertTrue(paths.stream().noneMatch(path -> path.contains("[CamelName]")),
                "路径中的 [CamelName] 未替换: " + paths);
            assertTrue(paths.stream().noneMatch(path -> path.contains("[UnderLineName]")),
                "路径中的 [UnderLineName] 未替换: " + paths);
            // .ftl 后缀必须已被剥掉
            assertTrue(paths.stream().noneMatch(path -> path.endsWith(".ftl")), "产物中残留 .ftl: " + paths);
            // 根目录为 artifactId
            assertTrue(paths.stream().allMatch(path -> path.startsWith("demo-service/")),
                "产物根目录应为 demo-service: " + paths);

            String pom = text(result, "demo-service/pom.xml");
            assertTrue(pom.contains("<artifactId>demo-service</artifactId>"), pom);
            assertTrue(pom.contains("com.carlos.demo"), pom);
            assertTrue(pom.contains("1.0.0-SNAPSHOT"), pom);
            // Maven 占位符应原样保留（模板里是 ${r'...'} 转义）
            assertTrue(pom.contains("${project.parent.artifactId}"), pom);
        } finally {
            sandbox.destroy(work);
        }
    }

    private String text(final GeneratedResult result, final String path) {
        return result.getFiles().stream()
            .filter(file -> path.equals(file.getPath()))
            .findFirst()
            .map(file -> new String(file.getContent(), StandardCharsets.UTF_8))
            .orElseThrow(() -> new AssertionError("产物中不存在: " + path));
    }

    private TemplateGroupMeta groupMeta() {
        return new TemplateGroupMeta()
            .setGroupId(1L)
            .setCode("carlos-springboot")
            .setName("Spring Boot 单体工程")
            .setDescribe("test")
            .setTemplateRoot("template");
    }

    /**
     * 递归读取目录下的模板文件为引擎可消费的源文件
     *
     * @param root 模板根目录
     * @return 模板源文件清单
     */
    private List<TemplateSourceFile> readTemplates(final File root) {
        List<TemplateSourceFile> list = new ArrayList<>();
        Path base = root.toPath();
        FileUtil.walkFiles(root, file -> list.add(new TemplateSourceFile(
            base.relativize(file.toPath()).toString().replace('\\', '/'),
            FileUtil.readString(file, StandardCharsets.UTF_8),
            "TEXT")));
        return list;
    }

    /**
     * 从模块目录向上定位后端根，再拼出 carlos-tools 资源路径
     *
     * @param relative 资源相对路径
     * @return 资源目录
     */
    private File locateResource(final String relative) {
        String suffix = "carlos-integration/carlos-tools/src/main/resources/" + relative;
        File current = new File(".").getAbsoluteFile();
        for (int i = 0; i < 8 && current != null; i++) {
            File candidate = new File(current, suffix);
            if (candidate.isDirectory()) {
                return candidate;
            }
            current = current.getParentFile();
        }
        throw new AssertionError("找不到模板资源: " + relative);
    }
}
