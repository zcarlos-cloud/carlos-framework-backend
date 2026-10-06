package com.carlos.workflow.api.pojo.ao;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 流程图节点 AO，包含节点办理状态着色所需的信息。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
public class WorkflowChartNodeAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点编码
     */
    private String nodeCode;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点类型（0开始 1中间 2结束 3互斥网关 4并行网关）
     */
    private Integer nodeType;

    /**
     * 办理状态（0未办理 1待办理 2已办理）
     */
    private Integer status;

    /**
     * 节点坐标
     */
    private String coordinate;

    /**
     * 任意节点跳转
     */
    private String anyNodeSkip;

    /**
     * 以本节点为起点的跳转线
     */
    private List<WorkflowChartSkipAO> skips;

}
