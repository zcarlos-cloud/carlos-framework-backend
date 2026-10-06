package com.carlos.devkit.engine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * <p>
 * 生成产物中的单个文件（内存态，不落盘）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Accessors(chain = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedFile {

    /**
     * 相对产物根目录的路径（统一用 '/' 分隔）
     */
    private String path;

    /**
     * 文件内容
     */
    private byte[] content;

    /**
     * 是否文本（true 时 content 按 UTF-8 文本下发预览；false 时前端只展示大小）
     */
    private boolean text;

    /**
     * 文件字节数
     */
    private int size;

    /**
     * 构造文本产物
     *
     * @param path    相对路径
     * @param content 文本内容
     * @return 生成文件
     */
    public static GeneratedFile text(final String path, final String content) {
        final byte[] bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return new GeneratedFile(path, bytes, true, bytes.length);
    }

    /**
     * 构造二进制产物
     *
     * @param path    相对路径
     * @param content 字节内容
     * @return 生成文件
     */
    public static GeneratedFile binary(final String path, final byte[] content) {
        return new GeneratedFile(path, content, false, content.length);
    }
}
