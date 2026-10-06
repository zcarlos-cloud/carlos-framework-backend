package com.carlos.fx.codege.entity;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 数据库表字段对象
 *
 * @author: Carlos
 * @date: 2019/4/14 0:38
 * @ModificationHistory: Who  When  What ---------     -------------   --------------------------------------
 */
@Accessors(chain = true)
@Data
public class ColumnBean {

    /**
     * 字段全名
     */
    private String columnName;
    /**
     * Java数据类型
     */
    private String columnType;
    /**
     * 数据库数据类型
     */
    private String columnDbType;
    /**
     * java 数据类型
     */
    private String javaType;
    /**
     * 行描述
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
     * 对应Java属性名
     */
    private String propertyName;
    /**
     * 对应Java属性名  首字母大写
     */
    private String propertyNameUp;
    /**
     * 主键字段
     */
    private boolean primaryKey;
    /**
     * 逻辑删除字段
     */
    private boolean logicField;
    /**
     * 乐观锁字段
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
     * 是否是通用字段
     */
    private boolean commonField;

}
