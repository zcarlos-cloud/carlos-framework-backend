package com.carlos.devkit.engine.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 数据库表字段元数据（FreeMarker 渲染上下文中的 column 项）
 * </p>
 * <p>
 * 字段名与 carlos-tools 原 {@code ColumnBean} 完全一致，模板迁移零成本。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Accessors(chain = true)
@Data
public class ColumnMeta {

    /**
     * 字段全名
     */
    private String columnName;

    /**
     * 字段全限定 Java 类型（如 java.lang.String）
     */
    private String columnType;

    /**
     * 数据库数据类型（如 VARCHAR）
     */
    private String columnDbType;

    /**
     * Java 数据类型简名
     */
    private String javaType;

    /**
     * 字段注释
     */
    private String columnComment;

    /**
     * 默认值
     */
    private String columnDefault;

    /**
     * 小数位数
     */
    private int decimalDigits;

    /**
     * 容量
     */
    private int columnsSize;

    /**
     * 对应 Java 属性名
     */
    private String propertyName;

    /**
     * 对应 Java 属性名（首字母大写）
     */
    private String propertyNameUp;

    /**
     * 是否主键
     */
    private boolean primaryKey;

    /**
     * 是否逻辑删除字段
     */
    private boolean logicField;

    /**
     * 是否乐观锁字段
     */
    private boolean versionField;

    /**
     * 是否自增
     */
    private boolean autoIncrement;

    /**
     * 是否允许空值
     */
    private boolean nullable;

    /**
     * 是否通用字段
     */
    private boolean commonField;
}
