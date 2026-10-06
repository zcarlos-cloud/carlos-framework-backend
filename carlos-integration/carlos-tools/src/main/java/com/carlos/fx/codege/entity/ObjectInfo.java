package com.carlos.fx.codege.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Set;

/**
 * 数据库表对象
 *
 * @author: Carlos
 * @date: 2019/4/14 0:51
 * @ModificationHistory: Who  When  What ---------     -------------   --------------------------------------
 */
@Accessors(chain = true)
@Data
public class ObjectInfo {

    /**
     * 表名
     */
    private String name;
    /**
     * 表备注
     */
    private String comment;
    /**
     * 模块名
     */
    private String module;
    /**
     * 表列名
     */
    private List<ColumnBean> columns;
    /**
     * 主键
     */
    private List<String> primaryKeys;


    /**
     * 基础类名
     */
    private String beanName;

    /**
     * 类属性名
     */
    private String beanPropertyName;

    /**
     * 简化的属性名：去除多余的前缀
     */
    private String simplePropertyName;


    /**
     * 类名前缀
     */
    private String classPrefix;
    /**
     * 类名调用前缀（第一个字母小写）
     */
    private String classPropertyPrefix;
    /**
     * 去除表名前缀的属性名
     *
     * @since 4.0.0
     */
    private String classMainPrefix;
    /**
     * 主键
     */
    private String primaryKey;
    /**
     * import动态导入
     */
    private Set<String> imports;
    /**
     * 基本Bean Name不加任何前后缀
     */
    private String apiPath;
    /**
     * 逻辑删除字段
     */
    private String logicDelete;
}
