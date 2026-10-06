package com.carlos.workflow.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * <p>
 * 流程实例 DTO。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
public class WorkflowInstanceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程实例id
     */
    private Long instanceId;

    /**
     * 流程定义id
     */
    private Long definitionId;

    /**
     * 流程名称
     */
    private String flowName;

    /**
     * 业务id
     */
    private String businessId;

    /**
     * 当前节点编码
     */
    private String nodeCode;

    /**
     * 当前节点名称
     */
    private String nodeName;

    /**
     * 当前节点类型（0开始 1中间 2结束 3互斥网关 4并行网关）
     */
    private Integer nodeType;

    /**
     * 流程状态
     */
    private String flowStatus;

    /**
     * 流程激活状态（0挂起 1激活）
     */
    private Integer activityStatus;

    /**
     * 流程变量
     */
    private Map<String, Object> variable;

    /**
     * 发起人（当前登录用户 id）
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}
