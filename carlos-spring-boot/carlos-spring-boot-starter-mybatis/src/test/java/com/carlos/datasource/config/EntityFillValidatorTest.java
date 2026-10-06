package com.carlos.datasource.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EntityFillValidator 启动期校验 单元测试
 *
 * @author carlos
 * @since 3.0.0
 */
class EntityFillValidatorTest {

    private final MybatisProperties properties = new MybatisProperties();
    private final EntityFillValidator validator =
            new EntityFillValidator(properties, properties.toMybatisCommonField());

    @Test
    @DisplayName("fill 全部落在可填充字段上时应通过（无违规）")
    void should_pass_when_all_fill_fields_fillable() {
        List<String> violations = validator.validate(List.of("com.carlos.datasource.fixture.good"));
        assertTrue(violations.isEmpty(), "合法实体不应产生违规: " + violations);
    }

    @Test
    @DisplayName("fill 落在无策略字段上时应列出违规字段")
    void should_report_unfillable_fields() {
        List<String> violations = validator.validate(List.of("com.carlos.datasource.fixture.bad"));
        assertEquals(2, violations.size(), "BadEntity 有 enabled 与 version 两个无策略字段: " + violations);
        assertTrue(violations.stream().anyMatch(v -> v.contains("enabled")), "应报告 enabled 字段");
        assertTrue(violations.stream().anyMatch(v -> v.contains("version")), "应报告 version 字段");
    }

    @Test
    @DisplayName("混合包扫描时应只报告违规实体")
    void should_only_report_violations_in_mixed_scan() {
        List<String> violations = validator.validate(List.of("com.carlos.datasource.fixture"));
        assertEquals(2, violations.size(), "整包扫描应只报 BadEntity 的两处: " + violations);
    }

    @Test
    @DisplayName("不存在的包应返回空列表（不抛异常）")
    void should_ignore_nonexistent_package() {
        List<String> violations = validator.validate(List.of("com.carlos.datasource.notexist"));
        assertTrue(violations.isEmpty());
    }
}
