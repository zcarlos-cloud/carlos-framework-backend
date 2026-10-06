package com.carlos.workflow.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 待办任务详情 DTO，用于审批页加载：任务信息 + 审批表单 + 表单数据。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
public class WorkflowTaskDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 待办任务id
     */
    private Long taskId;

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
     * 流程激活状态（0挂起 1激活），取自流程实例
     */
    private Integer activityStatus;

    /**
     * 审批表单是否自定义（Y是 N否）
     */
    private String formCustom;

    /**
     * 审批表单路径
     */
    private String formPath;

    /**
     * 当前任务的办理人权限标识
     */
    private List<String> permissionList;

    /**
     * 计划审批人姓名串（{@code flow_user} 中 type=1，多个用分号拼接）
     */
    private String approver;

    /**
     * 转办人姓名串（{@code flow_user} 中 type=2，多个用分号拼接）
     */
    private String transferredBy;

    /**
     * 委托人姓名串（{@code flow_user} 中 type=3，多个用分号拼接）
     */
    private String delegate;

    /**
     * 任务创建时间
     */
    private Date createTime;

    /**
     * 任务更新时间
     */
    private Date updateTime;

    /**
     * 节点或流程绑定的表单定义，未绑定表单时为 null
     */
    private WorkflowFormDTO form;

    /**
     * 表单数据，取自流程实例变量中的 formData
     */
    private Object formData;

}
