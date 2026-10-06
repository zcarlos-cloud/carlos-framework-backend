package com.carlos.devkit.support;

import cn.hutool.core.io.FileUtil;
import com.carlos.devkit.config.DevkitProperties;
import com.carlos.devkit.enums.DevkitErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * <p>
 * 渲染沙箱
 * </p>
 * <p>
 * 每次生成请求分配一个独立目录 {@code ${tmpRoot}/{uuid}/}，请求结束即整体删除。
 * 与 carlos-tools 原实现（固定 {@code java.io.tmpdir/codege_temp}）不同 —— 后者在多用户
 * 并发下会互相覆盖模板与产物。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Sandbox {

    /**
     * 启动时清理的残留目录最小年龄（小时），避免误删同机其他实例正在用的目录
     */
    private static final long STALE_HOURS = 1L;

    private final DevkitProperties devkitProperties;

    /**
     * 启动时清理残留沙箱目录
     *
     * <p>⚠️ 根目录不存在时必须直接返回：`FileUtil.ls` 对不存在的路径会抛
     * {@code IORuntimeException("Path [...] is not directory!")}，
     * 而本方法在 {@code @PostConstruct} 里，抛出会**直接导致应用启动失败**
     * （首次部署、或临时目录被清理后重启都会命中）。</p>
     */
    @PostConstruct
    public void cleanupStale() {
        File root = devkitProperties.resolveTmpRoot();
        if (!root.isDirectory()) {
            log.debug("渲染沙箱根目录不存在，跳过启动清理: {}", root.getPath());
            return;
        }
        File[] children = FileUtil.ls(root.getAbsolutePath());
        if (children == null || children.length == 0) {
            return;
        }
        Instant threshold = Instant.now().minus(STALE_HOURS, ChronoUnit.HOURS);
        int cleaned = 0;
        for (File child : children) {
            if (!child.isDirectory()) {
                continue;
            }
            try {
                Instant lastModified = Files.getLastModifiedTime(child.toPath()).toInstant();
                if (lastModified.isBefore(threshold) && FileUtil.del(child)) {
                    cleaned++;
                }
            } catch (Exception e) {
                log.warn("清理残留沙箱目录失败: {}", child.getPath(), e);
            }
        }
        if (cleaned > 0) {
            log.info("启动清理残留渲染沙箱目录 {} 个", cleaned);
        }
    }

    /**
     * 创建沙箱目录
     *
     * @return 沙箱目录路径
     */
    public Path create() {
        File root = devkitProperties.resolveTmpRoot();
        File dir = new File(root, UUID.randomUUID().toString().replace("-", ""));
        if (!FileUtil.mkdir(dir).isDirectory()) {
            throw DevkitErrorCode.GENERATE_ENGINE_ERROR.exception("渲染沙箱目录创建失败：" + dir.getPath());
        }
        return dir.toPath();
    }

    /**
     * 销毁沙箱目录（静默）
     *
     * @param dir 沙箱目录
     */
    public void destroy(final Path dir) {
        if (dir == null) {
            return;
        }
        try {
            FileUtil.del(dir.toFile());
        } catch (Exception e) {
            log.warn("删除渲染沙箱目录失败: {}", dir, e);
        }
    }
}
