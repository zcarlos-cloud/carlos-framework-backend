package com.carlos.workflow.api.pojo.ao;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 历史任务详情 AO，用于查看已办任务、已办分页列表与审批轨迹（某实例的全部历史任务）。
 * </p>
 * <p>
 * {@code approver} 与 {@code collaborator} 是<b>姓名</b>：{@code flow_his_task} 里存的是用户 id
 * （{@code collaborator} 为逗号分隔的 id 串），服务层批量查组织机构中心换成姓名后返回，
 * 保持与表里的分隔符一致。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
public class WorkflowHisTaskAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 历史任务id
     */
    private Long hisTaskId;

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
     * 协作方式（1审批 2转办 3委派 4会签 5票签 6加签 7减签）
     */
    private Integer cooperateType;

    /**
     * 跳转类型（PASS审批通过 REJECT退回 NONE无动作）
     */
    private String skipType;

    /**
     * 流程状态
     */
    private String flowStatus;

    /**
     * 审批人姓名（多个用逗号分隔，与表内分隔符一致）
     */
    private String approver;

    /**
     * 协作人姓名（多个用逗号分隔，与表内分隔符一致）
     */
    private String collaborator;

    /**
     * 审批意见
     */
    private String message;

    /**
     * 目标节点编码
     */
    private String targetNodeCode;

    /**
     * 目标节点名称
     */
    private String targetNodeName;

    /**
     * 审批表单是否自定义（Y是 N否）
     */
    private String formCustom;

    /**
     * 审批表单路径
     */
    private String formPath;

    /**
     * 办理时间（历史任务创建时间）
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 节点或流程绑定的表单定义，未绑定表单时为 null
     */
    private WorkflowFormAO form;

    /**
     * 表单数据，取自流程实例变量中的 formData
     */
    private Object formData;

}
