package com.carlos.workflow.api.pojo.ao;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 流程节点 AO，用于「自由跳转 / 驳回到指定节点」时选择一个目标节点。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
public class WorkflowNodeAO implements Serializable {

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
