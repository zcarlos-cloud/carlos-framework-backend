package com.carlos.devkit.engine.support;

import cn.hutool.core.util.StrUtil;
import com.carlos.devkit.engine.DevkitConstants;
import com.carlos.devkit.engine.model.GeneratedFile;
import com.carlos.devkit.enums.DevkitErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * <p>
 * 产物收集器
 * </p>
 * <p>
 * 把磁盘上的产物目录读进内存（用于在线预览与 zip 打包）。带文件数 / 总大小上限，
 * 超限立即失败，避免恶意或异常模板撑爆内存。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
public final class OutputCollector {

    /**
     * 文件大小上限（单文件）
     */
    private static final long MAX_SINGLE_FILE_SIZE = 5L * 1024 * 1024;

    private OutputCollector() {
    }

    /**
     * 收集目录下的全部产物文件
     *
     * @param root         产物根目录
     * @param maxFileCount 文件数上限
     * @param maxTotalSize 总大小上限（字节）
     * @return 产物文件清单（路径为相对 root 的 '/' 分隔路径）
     */
    public static List<GeneratedFile> collect(final File root, final int maxFileCount, final long maxTotalSize) {
        Path rootPath = root.toPath().toAbsolutePath().normalize();
        List<GeneratedFile> files = new ArrayList<>();
        long totalSize = 0L;
        try (Stream<Path> stream = Files.walk(rootPath)) {
            List<Path> paths = stream
                .filter(Files::isRegularFile)
                .sorted(Comparator.comparing(Path::toString))
                .toList();
            for (Path path : paths) {
                String relative = toRelative(rootPath, path);
                // 渲染后未清理干净的模板文件不进入产物
                if (StrUtil.endWithIgnoreCase(relative, DevkitConstants.TEMPLATE_FILE_SUFFIX)) {
                    continue;
                }
                if (files.size() >= maxFileCount) {
                    throw DevkitErrorCode.GENERATE_OUTPUT_TOO_LARGE.exception(
                        "产物文件数超过 " + maxFileCount + " 个上限");
                }
                byte[] content = Files.readAllBytes(path);
                if (content.length > MAX_SINGLE_FILE_SIZE) {
                    throw DevkitErrorCode.GENERATE_OUTPUT_TOO_LARGE.exception(
                        "产物文件过大：" + relative);
                }
                totalSize += content.length;
                if (totalSize > maxTotalSize) {
                    throw DevkitErrorCode.GENERATE_OUTPUT_TOO_LARGE.exception(
                        "产物总大小超过 " + maxTotalSize + " 字节上限");
                }
                files.add(new GeneratedFile(relative, content, isText(content), content.length));
            }
        } catch (IOException e) {
            log.error("产物收集失败: {}", rootPath, e);
            throw DevkitErrorCode.GENERATE_FAILED.exception("产物收集失败");
        }
        if (files.isEmpty()) {
            throw DevkitErrorCode.GENERATE_FAILED.exception("本次生成没有产生任何文件，请检查模板是否含有 .ftl 文件");
        }
        return files;
    }

    /**
     * 计算相对路径（统一 '/' 分隔）
     *
     * @param root 根目录
     * @param path 文件
     * @return 相对路径
     */
    private static String toRelative(final Path root, final Path path) {
        return root.relativize(path).toString().replace(File.separatorChar, '/');
    }

    /**
     * 判断是否为文本文件（无 NUL 字节即视为文本）
     *
     * @param content 文件内容
     * @return true 表示文本
     */
    private static boolean isText(final byte[] content) {
        int limit = Math.min(content.length, 4096);
        for (int i = 0; i < limit; i++) {
            if (content[i] == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 以 UTF-8 读取文本产物内容
     *
     * @param file 产物文件
     * @return 文本内容
     */
    public static String readText(final GeneratedFile file) {
        return new String(file.getContent(), StandardCharsets.UTF_8);
    }
}
