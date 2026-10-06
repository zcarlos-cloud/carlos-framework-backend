package com.carlos.workflow.ddl;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * PostgreSQL DDL 校验测试：在专用 schema 中执行 docs/sql/workflow_postgresql.sql 并校验建表结果。
 * </p>
 * <p>
 * 未配置 POC_PG_URL 时通过 Assumptions 自动跳过，因此默认构建不需要 PostgreSQL。
 * 参见模块 README 的「测试」章节了解如何启用。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowPostgreSqlDdlTest {

    private static final String URL_KEY = "POC_PG_URL";

    private static final String USER_KEY = "POC_PG_USER";

    private static final String PASSWORD_KEY = "POC_PG_PASSWORD";

    private static final String SCHEMA_KEY = "POC_PG_SCHEMA";

    private static final String DEFAULT_SCHEMA = "workflow_poc";

    /**
     * 测试的工作目录是模块根（carlos-workflow-bus），而按模块惯例，
     * 建表脚本放在聚合模块的 docs/sql 下，因此这里需要回退一级。
     */
    private static final Path DDL_PATH = Path.of("..", "docs", "sql", "workflow_postgresql.sql");

    private static final List<String> EXPECTED_TABLES = List.of(
        "flow_definition", "flow_node", "flow_skip", "flow_instance",
        "flow_task", "flow_his_task", "flow_user");

    @Test
    void officialDdlCreatesEveryFlowTable() throws Exception {
        String url = config(URL_KEY);
        Assumptions.assumeTrue(StringUtils.hasText(url), URL_KEY + " 未配置，跳过 PostgreSQL DDL 校验");

        String schema = configOrDefault(SCHEMA_KEY, DEFAULT_SCHEMA);
        assertThat(schema)
            .as("schema 名会被直接拼进 SQL，必须是安全的标识符")
            .matches("[A-Za-z_][A-Za-z0-9_]*");
        assertThat(DDL_PATH).exists();

        List<String> statements = splitStatements(Files.readString(DDL_PATH, StandardCharsets.UTF_8));
        assertThat(statements).hasSizeGreaterThan(EXPECTED_TABLES.size());

        try (Connection connection = DriverManager.getConnection(url, config(USER_KEY), config(PASSWORD_KEY));
             Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            statement.execute("CREATE SCHEMA " + schema);
            statement.execute("SET search_path TO " + schema);

            executeAll(statement, statements);

            assertThat(existingTables(connection, schema)).containsAll(EXPECTED_TABLES);
        }
    }

    private void executeAll(Statement statement, List<String> statements) {
        for (int index = 0; index < statements.size(); index++) {
            String sql = statements.get(index);
            try {
                statement.execute(sql);
            } catch (SQLException e) {
                throw new AssertionError("第 %d 条 SQL 执行失败（SQLState=%s, errorCode=%d）：%s%n--- SQL ---%n%s"
                    .formatted(index + 1, e.getSQLState(), e.getErrorCode(), e.getMessage(), sql), e);
            }
        }
    }

    private List<String> existingTables(Connection connection, String schema) throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tables.add(resultSet.getString(1).toLowerCase());
                }
            }
        }
        return tables;
    }

    /**
     * 按行累积切分语句：官方 DDL 中每条语句都以独占一行的分号结束，注释与字符串内不含分号
     */
    private List<String> splitStatements(String content) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            current.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                String sql = current.toString().trim();
                statements.add(sql.substring(0, sql.length() - 1).trim());
                current.setLength(0);
            }
        }
        assertThat(current.toString().isBlank()).as("DDL 末尾存在未以分号结束的语句").isTrue();
        return statements;
    }

    private String configOrDefault(String key, String defaultValue) {
        String value = config(key);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String config(String key) {
        String value = System.getProperty(key);
        return StringUtils.hasText(value) ? value : System.getenv(key);
    }
}
