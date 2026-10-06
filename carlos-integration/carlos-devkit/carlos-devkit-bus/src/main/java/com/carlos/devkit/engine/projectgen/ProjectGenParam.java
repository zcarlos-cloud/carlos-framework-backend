package com.carlos.devkit.engine.projectgen;

import com.carlos.devkit.engine.model.TemplateGroupMeta;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 工程生成参数（FreeMarker 渲染上下文中的 project 对象）
 * </p>
 * <p>
 * 字段名与 carlos-tools 原 {@code ProjectInfo} 保持一致，
 * 现有工程模板里的 {@code ${project.artifactId}} / {@code ${project.camelName}} 无需改动。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Accessors(chain = true)
@Data
public class ProjectGenParam {

    /**
     * 项目名（产物根目录名）
     */
    private String projectName;

    /**
     * 项目描述
     */
    private String describe;

    /**
     * Maven groupId
     */
    private String groupId;

    /**
     * groupId 拆分
     */
    private List<String> packageNameItems;

    /**
     * Maven artifactId
     */
    private String artifactId;

    /**
     * Maven version
     */
    private String version;

    /**
     * 包名（= groupId）
     */
    private String packageName;

    /**
     * 项目名大驼峰
     */
    private String camelName;

    /**
     * 项目名下划线
     */
    private String underlineName;

    /**
     * 项目负责人
     */
    private String author;

    /**
     * 输出路径（沙箱内，仅内部使用）
     */
    private String outputPath;

    /**
     * 模板组元数据
     */
    private TemplateGroupMeta templateGroup;
}
