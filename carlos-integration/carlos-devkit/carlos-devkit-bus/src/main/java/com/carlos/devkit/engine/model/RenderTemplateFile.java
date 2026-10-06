package com.carlos.devkit.engine.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.File;
import java.io.Serializable;

/**
 * <p>
 * 待渲染的模板文件运行时信息（物化到临时目录后的文件）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Accessors(chain = true)
@Data
public class RenderTemplateFile implements Serializable {

    /**
     * 模板文件（临时目录中的真实文件）
     */
    private File file;

    /**
     * 模板文件名（含 .ftl 后缀）
     */
    private String name;

    /**
     * 模板所在目录
     */
    private String path;

    /**
     * 去掉 .ftl 与 [for] 指令后的目标文件名前缀
     */
    private String preName;

    /**
     * 最终目标文件名
     */
    private String targetName;

    /**
     * 是否按表循环生成（文件名含 [for]）
     */
    private boolean loop;
}
