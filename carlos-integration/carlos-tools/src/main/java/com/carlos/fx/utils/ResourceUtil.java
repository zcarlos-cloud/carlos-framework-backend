package com.carlos.fx.utils;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * <p>
 * 代码生成工具
 * </p>
 *
 * @author Carlos
 * @date 2025-05-06 11:34
 */
public class ResourceUtil {

    @SneakyThrows
    public static void copyResourceFile2Temp(String resourceName, String localTempDir) {
        // 清空临时目录
        FileUtil.del(localTempDir);
        // 获取JAR包资源路径（示例：jar:file:/path/to.jar!/templates ）
        URL resourceUrl = ResourceUtil.class.getClassLoader().getResource(resourceName);
        if (resourceUrl == null) {
            throw new IllegalStateException("目录不存在: " + resourceName);
        }

        // 解析本地目录路径或者jar目录路径
        if (resourceUrl.toString().startsWith("file:/")) {
            // 本地目录
            File localDir = new File(URLDecoder.decode(resourceUrl.getPath(), StandardCharsets.UTF_8));
            if (!localDir.exists() || !localDir.isDirectory()) {
                throw new IllegalStateException("目录不存在: " + resourceName);
            }
            // 复制文件到临时目录
            File tempDir = new File(localTempDir);
            Files.walk(localDir.toPath()).forEach(source -> {
                try {
                    Files.copy(source, tempDir.toPath().resolve(localDir.toPath().relativize(source)));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
            return;
        }

        // 解析JAR文件路径
        if (resourceUrl.toString().startsWith("jar:file:/")) {
            String jarPath = resourceUrl.getPath().split("!")[0].replace("jar:file:", "").replace("file:/", "");
            try (JarFile jarFile = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().startsWith(resourceName) && !entry.isDirectory()) {
                        // 创建本地目录结构
                        File localFile = new File(localTempDir, entry.getName().substring(resourceName.length()));
                        Files.createDirectories(localFile.getParentFile().toPath());
                        // 复制文件内容
                        IoUtil.copy(jarFile.getInputStream(entry), Files.newOutputStream(localFile.toPath()));
                    }
                }
            }
            return;
        }
    }

}
