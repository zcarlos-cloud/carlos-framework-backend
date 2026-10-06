package com.carlos.devkit.support;

import com.carlos.devkit.config.DevkitProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 渲染沙箱测试。
 *
 * <p>{@link Sandbox#cleanupStale()} 在 {@code @PostConstruct} 里执行，一旦抛异常应用就起不来 ——
 * 「根目录还不存在」是最容易踩的分支（首次部署 / 临时目录被清后重启），必须钉死。</p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
class SandboxTest {

    private Path tmpRoot;

    private DevkitProperties properties;

    private Sandbox sandbox;

    @BeforeEach
    void setUp() throws Exception {
        // 指向一个**尚不存在**的目录，复现首次部署场景
        tmpRoot = java.nio.file.Files.createTempDirectory("devkit-sandbox-test").resolve("not-created-yet");
        properties = new DevkitProperties();
        properties.setTmpRoot(tmpRoot.toString());
        sandbox = new Sandbox(properties);
    }

    @AfterEach
    void tearDown() {
        cn.hutool.core.io.FileUtil.del(tmpRoot.getParent().toFile());
    }

    @Test
    @DisplayName("沙箱根目录不存在时启动清理不应抛异常（否则应用起不来）")
    void should_not_fail_when_root_absent() {
        assertFalse(tmpRoot.toFile().exists(), "前置条件：根目录尚未创建");
        assertDoesNotThrow(() -> sandbox.cleanupStale());
    }

    @Test
    @DisplayName("create 应创建沙箱目录，destroy 应删除它")
    void should_create_and_destroy_sandbox() {
        Path dir = sandbox.create();
        assertTrue(dir.toFile().isDirectory(), "沙箱目录应已创建");

        sandbox.destroy(dir);
        assertFalse(dir.toFile().exists(), "沙箱目录应已删除");
    }

    @Test
    @DisplayName("destroy 对 null / 不存在路径都应静默")
    void should_destroy_silently() {
        assertDoesNotThrow(() -> sandbox.destroy(null));
        assertDoesNotThrow(() -> sandbox.destroy(tmpRoot.resolve("never-existed")));
    }

    @Test
    @DisplayName("启动清理只删除超过 1 小时的残留目录，新目录必须保留")
    void should_keep_fresh_directories() throws Exception {
        Path fresh = sandbox.create();
        Files.writeString(fresh.resolve("a.txt"), "x", StandardCharsets.UTF_8);

        sandbox.cleanupStale();

        assertTrue(fresh.toFile().isDirectory(), "刚创建的沙箱目录不应被启动清理删掉");
    }

    @Test
    @DisplayName("启动清理应删除过期的残留目录")
    void should_remove_stale_directories() throws Exception {
        Path stale = sandbox.create();
        Files.writeString(stale.resolve("a.txt"), "x", StandardCharsets.UTF_8);
        // 把目录改到 2 小时前；个别文件系统不允许改目录时间，此时跳过本用例
        File staleFile = stale.toFile();
        org.junit.jupiter.api.Assumptions.assumeTrue(
            staleFile.setLastModified(System.currentTimeMillis() - 2L * 60 * 60 * 1000),
            "当前文件系统不支持修改目录时间，跳过");

        sandbox.cleanupStale();

        assertFalse(staleFile.exists(), "超过 1 小时的残留目录应被清理");
    }
}
