package com.carlos.workflow.api.pojo.ao;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 流程表单定义 AO，由 Warm-Flow 的 {@code Form} 实体转换而来。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
public class WorkflowFormAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 表单id
     */
    private Long formId;

    /**
     * 表单编码
     */
    private String formCode;

    /**
     * 表单名称
     */
    private String formName;

    /**
     * 表单版本
     */
    private String version;

    /**
     * 表单类型
     */
    private Integer formType;

    /**
     * 表单内容
     */
    private String formContent;

    /**
     * 表单路径
     */
    private String formPath;

}
