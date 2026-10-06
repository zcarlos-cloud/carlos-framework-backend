package com.carlos.workflow.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 流程定义 DTO。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
public class WorkflowDefinitionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程定义id
     */
    private Long definitionId;

    /**
     * 流程编码
     */
    private String flowCode;

    /**
     * 流程名称
     */
    private String flowName;

    /**
     * 流程版本
     */
    private String version;

    /**
     * 流程类别
     */
    private String category;

    /**
     * 设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）
     */
    private String modelValue;

    /**
     * 是否发布（0未发布 1已发布 9已失效）
     */
    private Integer isPublish;

    /**
     * 流程定义激活状态（0挂起 1激活）
     */
    private Integer activityStatus;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}
