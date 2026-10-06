package com.carlos.devkit.support;

import com.carlos.devkit.engine.model.GeneratedFile;
import com.carlos.devkit.enums.DevkitErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * <p>
 * 产物打包器
 * </p>
 * <p>
 * 把内存中的产物打成 zip 字节流，直接回给浏览器下载（不落业务磁盘）。
 * 路径统一用 '/' 分隔，zip 内保留完整的目录结构。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
public final class ZipPacker {

    private ZipPacker() {
    }

    /**
     * 打包为 zip 字节流
     *
     * @param files 产物文件（path 为 '/' 分隔的相对路径）
     * @return zip 字节
     */
    public static byte[] pack(final List<GeneratedFile> files) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.setLevel(java.util.zip.Deflater.BEST_SPEED);
            for (GeneratedFile file : files) {
                ZipEntry entry = new ZipEntry(file.getPath());
                entry.setTime(System.currentTimeMillis());
                zip.putNextEntry(entry);
                zip.write(file.getContent());
                zip.closeEntry();
            }
            zip.finish();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("产物打包失败", e);
            throw DevkitErrorCode.GENERATE_FAILED.exception("产物打包失败");
        }
    }

    /**
     * 生成下载文件名（RFC 5987 编码，兼容中文）
     *
     * @param baseName 基础名（不带扩展名）
     * @return Content-Disposition 头值
     */
    public static String contentDisposition(final String baseName) {
        String safe = baseName == null || baseName.isBlank() ? "generated" : baseName;
        String encoded = java.net.URLEncoder.encode(safe + ".zip", StandardCharsets.UTF_8)
            .replace("+", "%20");
        return "attachment; filename=\"" + safe + ".zip\"; filename*=UTF-8''" + encoded;
    }
}
