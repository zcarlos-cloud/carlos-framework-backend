package com.carlos.migration.util;

import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.CommonErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 变更日志生成工具
 * 用于生成标准的 Liquibase 变更日志文件
 *
 * @author carlos
 * @since 3.0.0
 */
public class ChangelogGenerator {

    private static final Logger log = LoggerFactory.getLogger(ChangelogGenerator.class);

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * 生成 YAML 格式的变更日志文件
     *
     * @param basePath    基础路径
     * @param version     版本号
     * @param author      作者
     * @param description 描述
     * @return 生成的文件路径
     */
    public static String generateYamlChangelog(String basePath, String version,
                                               String author, String description) {
        String timestamp = LocalDateTime.now().format(FILE_FORMATTER);
        String filename = String.format("%s_%s_%s.yaml", timestamp, version,
            description.replaceAll("\\s+", "_"));

        Path filePath = Paths.get(basePath, filename);

        String content = """
            databaseChangeLog:
              - changeSet:
                  id: %s
                  author: %s
                  created: "%s"
                  comment: "%s"
                  changes:
                    # 在此添加变更操作
                    # 示例：创建表
                    # - createTable:
                    #     tableName: example_table
                    #     columns:
                    #       - column:
                    #           name: id
                    #           type: BIGINT
                    #           autoIncrement: true
                    #           constraints:
                    #             primaryKey: true
              rollback:
                # 在此添加回滚操作
                # 示例：删除表
                # - dropTable:
                #     tableName: example_table
            """.formatted(version, author, LocalDateTime.now().format(FORMATTER), description);

        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content, StandardOpenOption.CREATE_NEW);
            log.info("[ChangelogGenerator] 生成变更日志文件: {}", filePath);
            return filePath.toString();
        } catch (IOException e) {
            log.error("[ChangelogGenerator] 生成变更日志失败", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR, "生成变更日志失败", e);
        }
    }

    /**
     * 生成 SQL 格式的变更日志文件
     */
    public static String generateSqlChangelog(String basePath, String version,
                                              String author, String description) {
        String timestamp = LocalDateTime.now().format(FILE_FORMATTER);
        String filename = String.format("%s_%s_%s.sql", timestamp, version,
            description.replaceAll("\\s+", "_"));

        Path filePath = Paths.get(basePath, filename);

        String content = """
            -- liquibase formatted sql
            -- changeset %s:%s
            -- comment: %s
            -- created: %s

            -- 在此添加 SQL 变更

            -- rollback: 在此添加回滚 SQL
            """.formatted(author, version, description, LocalDateTime.now().format(FORMATTER));

        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content, StandardOpenOption.CREATE_NEW);
            log.info("[ChangelogGenerator] 生成 SQL 变更日志文件: {}", filePath);
            return filePath.toString();
        } catch (IOException e) {
            log.error("[ChangelogGenerator] 生成 SQL 变更日志失败", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR, "生成 SQL 变更日志失败", e);
        }
    }

    /**
     * 生成主变更日志文件（用于 include 其他变更日志）
     */
    public static String generateMasterChangelog(String basePath, String... includes) {
        Path filePath = Paths.get(basePath, "db.changelog-master.yaml");

        StringBuilder content = new StringBuilder("databaseChangeLog:\n");
        for (String include : includes) {
            content.append(String.format("  - include:\n      file: %s\n", include));
        }

        try {
            Files.createDirectories(filePath.getParent());
            if (!Files.exists(filePath)) {
                Files.writeString(filePath, content.toString(), StandardOpenOption.CREATE_NEW);
                log.info("[ChangelogGenerator] 生成主变更日志文件: {}", filePath);
            }
            return filePath.toString();
        } catch (IOException e) {
            log.error("[ChangelogGenerator] 生成主变更日志失败", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR, "生成主变更日志失败", e);
        }
    }
}
