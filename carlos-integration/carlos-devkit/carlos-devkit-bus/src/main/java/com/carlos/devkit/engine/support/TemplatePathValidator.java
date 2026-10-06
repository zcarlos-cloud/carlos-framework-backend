package com.carlos.devkit.engine.support;

import cn.hutool.core.util.StrUtil;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * <p>
 * 模板路径安全校验
 * </p>
 * <p>
 * 模板路径可由用户在线编辑，必须防目录穿越（{@code ../../etc/passwd}）、
 * 绝对路径、Windows 盘符与 UNC 路径。任何越界路径一律拒绝，绝不静默修正。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
public final class TemplatePathValidator {

    /**
     * 单段路径的最大长度
     */
    private static final int MAX_PATH_LENGTH = 512;

    private TemplatePathValidator() {
    }

    /**
     * 校验模板相对路径是否合法（不落盘，仅校验）
     *
     * @param filePath 组内相对路径，如 {@code src/main/java/pojo/entity/[base][for].java.ftl}
     * @return 归一化后的相对路径（'{@code /}' 分隔）
     * @throws IllegalArgumentException 路径非法时抛出
     */
    public static String validate(final String filePath) {
        if (StrUtil.isBlank(filePath)) {
            throw new IllegalArgumentException("模板路径不能为空");
        }
        if (filePath.length() > MAX_PATH_LENGTH) {
            throw new IllegalArgumentException("模板路径长度不能超过 " + MAX_PATH_LENGTH);
        }
        if (filePath.contains("\0")) {
            throw new IllegalArgumentException("模板路径含非法字符");
        }
        // 绝对路径 / 盘符 / UNC
        if (filePath.startsWith("/") || filePath.startsWith("\\") || filePath.contains(":")) {
            throw new IllegalArgumentException("模板路径不允许使用绝对路径或盘符：" + filePath);
        }
        String normalized = filePath.replace('\\', '/');
        for (String segment : normalized.split("/")) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("模板路径含非法路径段：" + filePath);
            }
        }
        return normalized;
    }

    /**
     * 在基准目录下安全解析目标路径
     *
     * @param baseDir  基准目录（绝对路径）
     * @param filePath 相对路径
     * @return 解析并归一化后的绝对路径
     * @throws IllegalArgumentException 解析结果逃逸出基准目录时抛出
     */
    public static Path resolveInside(final Path baseDir, final String filePath) {
        String normalized = validate(filePath);
        Path base = baseDir.toAbsolutePath().normalize();
        Path target = base.resolve(normalized).normalize();
        if (!target.startsWith(base)) {
            throw new IllegalArgumentException("模板路径越界：" + filePath);
        }
        return target;
    }
}
