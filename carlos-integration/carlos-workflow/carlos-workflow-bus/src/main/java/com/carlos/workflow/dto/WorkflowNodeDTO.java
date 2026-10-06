package com.carlos.workflow.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 流程节点 DTO，用于「自由跳转 / 驳回到指定节点」的目标节点清单。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
public class WorkflowNodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点编码
     */
    private String nodeCode;

    /**
     * 节点名称
     */
    private String nodeName;

}
