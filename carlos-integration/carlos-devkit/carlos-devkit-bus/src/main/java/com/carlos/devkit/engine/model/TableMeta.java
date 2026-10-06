package com.carlos.devkit.engine.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 数据库表元数据（FreeMarker 渲染上下文中的 table 项）
 * </p>
 * <p>
 * 字段名与 carlos-tools 原 {@code ObjectInfo} 完全一致，模板迁移零成本。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Accessors(chain = true)
@Data
public class TableMeta {

    /**
     * 表名（小写）
     */
    private String name;

    /**
     * 表备注
     */
    private String comment;

    /**
     * 模块名（表名第一段）
     */
    private String module;

    /**
     * 列元数据
     */
    private List<ColumnMeta> columns;

    /**
     * 主键列名（单主键场景）
     */
    private String primaryKey;

    /**
     * 类名前缀（如 SysUser）
     */
    private String classPrefix;

    /**
     * 类属性前缀（首字母小写，如 sysUser）
     */
    private String classPropertyPrefix;

    /**
     * 去除模块前缀的属性名（如 user）
     */
    private String classMainPrefix;

    /**
     * 接口路径片段（如 sys-user）
     */
    private String apiPath;

    /**
     * 逻辑删除字段列名
     */
    private String logicDelete;

    /**
     * 需要 import 的全限定类型
     */
    private Set<String> imports;
}
