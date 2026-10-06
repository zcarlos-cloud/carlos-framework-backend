package com.carlos.devkit.engine.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Set;

/**
 * <p>
 * 模板组元数据（渲染期上下文）
 * </p>
 * <p>
 * 对应 carlos-tools 原 {@code TemplateBaseInfo}（来自 template.xml），
 * Web 版改为从 {@code dev_template_group} 行装配。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Accessors(chain = true)
@Data
public class TemplateGroupMeta implements Serializable {

    /**
     * 模板组 ID
     */
    private Long groupId;

    /**
     * 模板组编码
     */
    private String code;

    /**
     * 模板组名称
     */
    private String name;

    /**
     * 模板组描述
     */
    private String describe;

    /**
     * 物化时的模板根目录名（默认 template）
     */
    private String templateRoot;

    /**
     * 逻辑删除字段名清单
     */
    private Set<String> logicDeleteFields;

    /**
     * 乐观锁版本字段名清单
     */
    private Set<String> versionFields;

    /**
     * 通用字段名清单
     */
    private Set<String> commonFields;
}
