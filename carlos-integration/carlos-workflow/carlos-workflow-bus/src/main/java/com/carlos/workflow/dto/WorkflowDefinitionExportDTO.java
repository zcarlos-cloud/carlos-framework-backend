package com.carlos.workflow.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 流程定义导出结果：既要给前端下载的 JSON 正文，也要拼下载文件名所需的基础信息。
 * </p>
 * <p>
 * 与 {@link WorkflowDefinitionDTO} 的区别：后者是列表 / 详情口径，不含正文；
 * 导出的正文可能很大，不适合塞进通用 DTO 里让列表端点跟着背负它。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
public class WorkflowDefinitionExportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程定义id
     */
    private Long definitionId;

    /**
     * 流程编码（下载文件名的一部分）
     */
    private String flowCode;

    /**
     * 流程版本（下载文件名的一部分）
     */
    private String version;

    /**
     * 导出内容（引擎生成的流程定义 JSON）
     */
    private String content;

}
