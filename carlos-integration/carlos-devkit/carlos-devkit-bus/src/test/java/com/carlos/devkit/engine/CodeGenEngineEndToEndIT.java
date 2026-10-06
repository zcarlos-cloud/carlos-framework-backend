package com.carlos.devkit.engine;

import com.carlos.devkit.config.DevkitProperties;
import com.carlos.devkit.engine.codegen.CodeGenEngine;
import com.carlos.devkit.engine.codegen.CodeGenParam;
import com.carlos.devkit.engine.model.GeneratedFile;
import com.carlos.devkit.engine.model.GeneratedResult;
import com.carlos.devkit.engine.model.TargetDatabase;
import com.carlos.devkit.engine.model.TemplateGroupMeta;
import com.carlos.devkit.engine.model.TemplateSourceFile;
import com.carlos.devkit.engine.render.FreemarkerRenderer;
import com.carlos.devkit.engine.support.JdbcMetaReader;
import com.carlos.devkit.engine.support.TemplateMaterializer;
import com.carlos.devkit.pojo.enums.DbTypeEnum;
import com.carlos.devkit.pojo.enums.FieldNameTypeEnum;
import com.carlos.devkit.support.Sandbox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 代码生成引擎端到端集成测试（需要真实 PostgreSQL）。
 *
 * <p>模板从 {@code dev_template} 表按 {@code group_code} 读取（即验证种子脚本灌入的内置模板可用），
 * 表元数据从同一个库的 {@code dev_datasource} 表读取。未配置 {@code PG_CONNECTION_STRING}
 * 时整体跳过（不会在无库环境里变红）。</p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@EnabledIfEnvironmentVariable(named = "PG_CONNECTION_STRING", matches = ".+")
class CodeGenEngineEndToEndIT {

    private static final String GROUP_CODE = "cloud-mybatis";

    private static final String SAMPLE_TABLE = "dev_datasource";

    private DevkitProperties properties;

    private ExecutorService executor;

    private Sandbox sandbox;

    private CodeGenEngine engine;

    private TargetDatabase target;

    private Path tmpRoot;

    @BeforeEach
    void setUp() throws Exception {
        String env = System.getenv("PG_CONNECTION_STRING");
        java.net.URI uri = java.net.URI.create(env);
        target = new TargetDatabase()
            .setDbType(DbTypeEnum.POSTGRESQL)
            .setHost(uri.getHost())
            .setPort(String.valueOf(uri.getPort()))
            .setDatabaseName(uri.getPath().substring(1))
            .setSchemaName("public");

        tmpRoot = java.nio.file.Files.createTempDirectory("devkit-codegen-it");
        properties = new DevkitProperties();
        properties.setTmpRoot(tmpRoot.toString());
        properties.setRenderTimeoutMs(10_000L);
        properties.setMetaTimeoutSec(10);
        executor = Executors.newFixedThreadPool(2);
        sandbox = new Sandbox(properties);
        engine = new CodeGenEngine(properties, new TemplateMaterializer(),
            new FreemarkerRenderer(properties, executor), new JdbcMetaReader(properties));
    }

    @AfterEach
    void tearDown() throws Exception {
        executor.shutdownNow();
        cn.hutool.core.io.FileUtil.del(tmpRoot.toFile());
    }

    @Test
    @DisplayName("应能从数据库模板 + 真实表元数据生成代码，且模板变量全部被替换")
    void should_generate_code_from_db_templates() throws Exception {
        String[] userInfo = java.net.URI.create(System.getenv("PG_CONNECTION_STRING")).getUserInfo().split(":", 2);
        String url = "jdbc:postgresql://" + target.getHost() + ":" + target.getPort() + "/"
            + target.getDatabaseName();
        java.util.Properties props = new java.util.Properties();
        props.setProperty("user", java.net.URLDecoder.decode(userInfo[0], StandardCharsets.UTF_8));
        props.setProperty("password", java.net.URLDecoder.decode(userInfo[1], StandardCharsets.UTF_8));

        try (Connection connection = DriverManager.getConnection(url, props)) {
            TemplateGroupMeta groupMeta = loadGroupMeta(connection);
            List<TemplateSourceFile> templates = loadTemplates(connection, groupMeta.getGroupId());
            assertFalse(templates.isEmpty(), "dev_template 中应存在 " + GROUP_CODE + " 的模板，请先执行种子脚本");

            CodeGenParam param = new CodeGenParam()
                .setProjectName("devkit-demo")
                .setPackageName("com.carlos.demo")
                .setPackageNameItems(List.of("com", "carlos", "demo"))
                .setAuthor("Carlos")
                .setUseTablePrefix(false)
                .setNameType(FieldNameTypeEnum.NOT_PREFIX_AND_CAMEL)
                .setTables(List.of(new com.carlos.devkit.engine.model.TableBaseInfo(SAMPLE_TABLE, "示例表")))
                .setTemplateGroup(groupMeta);

            Path work = sandbox.create();
            try {
                GeneratedResult result = engine.generate(
                    groupMeta, groupMeta.getTemplateRoot(), templates, param, target, connection, work);
                List<String> paths = result.getFiles().stream().map(GeneratedFile::getPath).toList();

                assertFalse(paths.isEmpty(), "产物不应为空");
                assertTrue(paths.stream().noneMatch(path -> path.contains("[base]")),
                    "路径中的 [base] 未替换: " + paths);
                assertTrue(paths.stream().noneMatch(path -> path.contains("[for]")),
                    "路径中的 [for] 未替换: " + paths);
                String entityPath = "devkit-demo/src/main/java/com/carlos/demo/pojo/entity/Datasource.java";
                assertTrue(paths.contains(entityPath),
                    "应生成实体类 " + entityPath + "（useTablePrefix=false 时去掉 dev_ 前缀）: " + paths);

                String entity = text(result, entityPath);
                assertTrue(entity.contains("package com.carlos.demo"), entity);
                assertTrue(entity.contains("class Datasource"), entity);
                assertTrue(entity.contains("private Boolean deleted"), "逻辑删除字段应映射为 Boolean: " + entity);
            } finally {
                sandbox.destroy(work);
            }
        }
    }

    private String text(final GeneratedResult result, final String path) {
        return result.getFiles().stream()
            .filter(file -> path.equals(file.getPath()))
            .findFirst()
            .map(file -> new String(file.getContent(), StandardCharsets.UTF_8))
            .orElseThrow(() -> new AssertionError("产物中不存在: " + path));
    }

    private TemplateGroupMeta loadGroupMeta(final Connection connection) throws Exception {
        String sql = "SELECT id, group_code, group_name, description, template_root, "
            + "logic_delete_fields, version_fields, common_fields "
            + "FROM dev_template_group WHERE group_code = ? AND is_deleted = 0";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, GROUP_CODE);
            try (ResultSet rs = statement.executeQuery()) {
                assertTrue(rs.next(), "未找到模板组 " + GROUP_CODE + "，请先执行 docs/db/devkit_seed_templates.sql");
                return new TemplateGroupMeta()
                    .setGroupId(rs.getLong("id"))
                    .setCode(rs.getString("group_code"))
                    .setName(rs.getString("group_name"))
                    .setDescribe(rs.getString("description"))
                    .setTemplateRoot(rs.getString("template_root"))
                    .setLogicDeleteFields(split(rs.getString("logic_delete_fields")))
                    .setVersionFields(split(rs.getString("version_fields")))
                    .setCommonFields(split(rs.getString("common_fields")));
            }
        }
    }

    private List<TemplateSourceFile> loadTemplates(final Connection connection, final Long groupId)
        throws Exception {
        String sql = "SELECT file_path, content, content_encoding FROM dev_template "
            + "WHERE group_id = ? AND status = 1 AND is_deleted = 0 ORDER BY file_path";
        List<TemplateSourceFile> list = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, groupId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    list.add(new TemplateSourceFile(rs.getString("file_path"),
                        rs.getString("content"), rs.getString("content_encoding")));
                }
            }
        }
        return list;
    }

    private java.util.Set<String> split(final String value) {
        if (value == null || value.isBlank()) {
            return java.util.Set.of();
        }
        return java.util.Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
