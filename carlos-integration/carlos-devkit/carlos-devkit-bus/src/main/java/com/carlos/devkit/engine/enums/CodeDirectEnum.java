package com.carlos.devkit.engine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 模板文件名指令枚举
 * </p>
 * <p>
 * 迁移自 carlos-tools 原 {@code CodeDirectEnum}。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Getter
@AllArgsConstructor
public enum CodeDirectEnum {

    /**
     * 循环生成（文件名含 [for] 时，按表逐张生成）
     */
    FOR("[for]", "循环指令"),

    /**
     * 基础文件名占位（渲染时替换为 table.classPrefix）
     */
    BASE("[base]", "文件基础名称");

    /**
     * 指令值
     */
    private final String value;

    /**
     * 描述
     */
    private final String describe;
}
