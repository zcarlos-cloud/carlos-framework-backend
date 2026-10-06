package com.carlos.fx.projectge.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 指令枚举
 * </p>
 *
 * @author Carlos
 * @date 2020/8/18 12:09
 * @since 3.0
 */
@AllArgsConstructor
@Getter
public enum ProjectDirectEnum {
    /**
     *
     */
    ARTIFACT_ID("[ArtifactId]", "ArtifactId"),
    PACKAGE("[Package]", "packages"),
    CAMEL_NAME("[CamelName]", "项目名驼峰"),
    UNDERLINE_NAME("[UnderLineName]", "项目名下划线"),

    ;

    /**
     * 指令值
     */
    private final String value;

    /**
     * 描述
     */
    private final String describe;


}
