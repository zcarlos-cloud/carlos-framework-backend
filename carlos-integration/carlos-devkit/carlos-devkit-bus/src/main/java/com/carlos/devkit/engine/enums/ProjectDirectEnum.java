package com.carlos.devkit.engine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 工程模板路径占位符指令枚举
 * </p>
 * <p>
 * 迁移自 carlos-tools 原 {@code ProjectDirectEnum}。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Getter
@AllArgsConstructor
public enum ProjectDirectEnum {

    /**
     * 构件 ID（如 demo-service）
     */
    ARTIFACT_ID("[ArtifactId]", "ArtifactId"),

    /**
     * 包路径（如 com/carlos/demo）
     */
    PACKAGE("[Package]", "packages"),

    /**
     * 项目名大驼峰（如 DemoService）
     */
    CAMEL_NAME("[CamelName]", "项目名驼峰"),

    /**
     * 项目名下划线（如 demo_service）
     */
    UNDERLINE_NAME("[UnderLineName]", "项目名下划线");

    /**
     * 指令值
     */
    private final String value;

    /**
     * 描述
     */
    private final String describe;
}
