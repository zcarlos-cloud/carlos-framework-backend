package com.carlos.devkit.engine.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 模板路径安全校验测试。
 *
 * <p>模板路径可由用户在线编辑，是目录穿越的唯一入口，必须逐个拒绝。</p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
class TemplatePathValidatorTest {

    @Test
    @DisplayName("合法相对路径应原样通过并归一化分隔符")
    void should_accept_valid_relative_path() {
        assertEquals("src/main/java/pojo/entity/[base][for].java.ftl",
            TemplatePathValidator.validate("src/main/java/pojo/entity/[base][for].java.ftl"));
        assertEquals("a/b.txt", TemplatePathValidator.validate("a\\b.txt"));
    }

    @Test
    @DisplayName("含 .. 的路径必须被拒绝")
    void should_reject_parent_traversal() {
        assertThrows(IllegalArgumentException.class, () -> TemplatePathValidator.validate("../etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> TemplatePathValidator.validate("a/../../b.txt"));
        assertThrows(IllegalArgumentException.class, () -> TemplatePathValidator.validate("a/./b.txt"));
    }

    @Test
    @DisplayName("绝对路径、盘符与 UNC 路径必须被拒绝")
    void should_reject_absolute_paths() {
        assertThrows(IllegalArgumentException.class, () -> TemplatePathValidator.validate("/etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> TemplatePathValidator.validate("\\windows\\system32"));
        assertThrows(IllegalArgumentException.class, () -> TemplatePathValidator.validate("C:/windows/x.txt"));
        assertThrows(IllegalArgumentException.class, () -> TemplatePathValidator.validate("c:x.txt"));
    }

    @Test
    @DisplayName("空路径、含 NUL、超长路径必须被拒绝")
    void should_reject_invalid_input() {
        assertThrows(IllegalArgumentException.class, () -> TemplatePathValidator.validate(""));
        assertThrows(IllegalArgumentException.class, () -> TemplatePathValidator.validate("  "));
        assertThrows(IllegalArgumentException.class, () -> TemplatePathValidator.validate("a\0b"));
        assertThrows(IllegalArgumentException.class,
            () -> TemplatePathValidator.validate("a".repeat(600)));
    }

    @Test
    @DisplayName("resolveInside 结果必须落在基准目录内")
    void should_resolve_inside_base_dir() {
        Path base = Path.of(System.getProperty("java.io.tmpdir"), "devkit-validator-test");
        assertEquals(base.resolve("a/b.txt").normalize(),
            TemplatePathValidator.resolveInside(base, "a/b.txt"));
        assertThrows(IllegalArgumentException.class,
            () -> TemplatePathValidator.resolveInside(base, "../escape.txt"));
    }
}
