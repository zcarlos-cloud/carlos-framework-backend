package com.carlos.devkit.engine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * <p>
 * 待物化的模板源文件（由 Service 层从 {@code dev_template} 装配后交给引擎）
 * </p>
 * <p>
 * 引擎只依赖本模型，不依赖持久化实体。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Accessors(chain = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateSourceFile {

    /**
     * 组内相对路径，如 {@code src/main/java/pojo/entity/[base][for].java.ftl}
     */
    private String path;

    /**
     * 文件正文
     */
    private String content;

    /**
     * 正文编码：TEXT=纯文本 BASE64=二进制
     */
    private String contentEncoding;

    /**
     * 是否文本正文
     *
     * @return true 表示 TEXT
     */
    public boolean isText() {
        return contentEncoding == null || "TEXT".equalsIgnoreCase(contentEncoding);
    }
}
