package com.carlos.workflow.convert;

import com.carlos.workflow.dto.WorkflowChartNodeDTO;
import com.carlos.workflow.dto.WorkflowChartSkipDTO;
import com.carlos.workflow.dto.WorkflowDefinitionDTO;
import com.carlos.workflow.dto.WorkflowFormDTO;
import com.carlos.workflow.dto.WorkflowHisTaskDTO;
import com.carlos.workflow.dto.WorkflowInstanceDTO;
import com.carlos.workflow.dto.WorkflowNodeDTO;
import com.carlos.workflow.dto.WorkflowTaskDTO;
import org.dromara.warm.flow.core.dto.FlowDto;
import org.dromara.warm.flow.core.dto.NodeJson;
import org.dromara.warm.flow.core.dto.SkipJson;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Form;
import org.dromara.warm.flow.core.entity.HisTask;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.core.entity.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Warm-Flow 引擎对象到本组件 DTO 的转换，集中在此处以避免引擎类型扩散到 Controller 与前端。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
public final class WorkflowDtoConvert {

    private WorkflowDtoConvert() {
    }

    /**
     * 待办任务 + 表单数据转 DTO
     *
     * @param task    待办任务
     * @param flowDto 引擎的表单与表单数据承载对象
     * @return WorkflowTaskDTO
     */
    public static WorkflowTaskDTO toTaskDTO(Task task, FlowDto flowDto) {
        WorkflowTaskDTO dto = toTaskDTO(task);
        if (flowDto != null) {
            dto.setForm(toFormDTO(flowDto.getForm()));
            dto.setFormData(flowDto.getData());
        }
        return dto;
    }

    /**
     * 待办任务转 DTO（列表口径：只映射任务实体自身携带的字段）
     * <p>
     * 流程名称、业务id、激活状态、计划审批人姓名等来自其它表或组织机构中心的字段，
     * 由 Manager 层批量补齐（见 {@code WorkflowTaskManagerImpl}），本转换器不查库。
     * </p>
     *
     * @param task 待办任务
     * @return WorkflowTaskDTO
     */
    public static WorkflowTaskDTO toTaskDTO(Task task) {
        WorkflowTaskDTO dto = new WorkflowTaskDTO();
        dto.setTaskId(task.getId());
        dto.setInstanceId(task.getInstanceId());
        dto.setDefinitionId(task.getDefinitionId());
        dto.setFlowName(task.getFlowName());
        dto.setBusinessId(task.getBusinessId());
        dto.setNodeCode(task.getNodeCode());
        dto.setNodeName(task.getNodeName());
        dto.setNodeType(task.getNodeType());
        dto.setFlowStatus(task.getFlowStatus());
        dto.setFormCustom(task.getFormCustom());
        dto.setFormPath(task.getFormPath());
        dto.setPermissionList(task.getPermissionList());
        dto.setCreateTime(task.getCreateTime());
        dto.setUpdateTime(task.getUpdateTime());
        return dto;
    }

    /**
     * 历史任务 + 表单数据转 DTO
     *
     * @param hisTask 历史任务
     * @param flowDto 引擎的表单与表单数据承载对象
     * @return WorkflowHisTaskDTO
     */
    public static WorkflowHisTaskDTO toHisTaskDTO(HisTask hisTask, FlowDto flowDto) {
        WorkflowHisTaskDTO dto = toHisTaskDTO(hisTask);
        if (flowDto != null) {
            dto.setForm(toFormDTO(flowDto.getForm()));
            dto.setFormData(flowDto.getData());
        }
        return dto;
    }

    /**
     * 历史任务转 DTO（列表口径：只映射历史任务实体自身携带的字段）
     *
     * @param hisTask 历史任务
     * @return WorkflowHisTaskDTO
     */
    public static WorkflowHisTaskDTO toHisTaskDTO(HisTask hisTask) {
        WorkflowHisTaskDTO dto = new WorkflowHisTaskDTO();
        dto.setHisTaskId(hisTask.getId());
        dto.setTaskId(hisTask.getTaskId());
        dto.setInstanceId(hisTask.getInstanceId());
        dto.setDefinitionId(hisTask.getDefinitionId());
        dto.setFlowName(hisTask.getFlowName());
        dto.setBusinessId(hisTask.getBusinessId());
        dto.setNodeCode(hisTask.getNodeCode());
        dto.setNodeName(hisTask.getNodeName());
        dto.setNodeType(hisTask.getNodeType());
        dto.setCooperateType(hisTask.getCooperateType());
        dto.setSkipType(hisTask.getSkipType());
        dto.setFlowStatus(hisTask.getFlowStatus());
        dto.setApprover(hisTask.getApprover());
        dto.setCollaborator(hisTask.getCollaborator());
        dto.setMessage(hisTask.getMessage());
        dto.setTargetNodeCode(hisTask.getTargetNodeCode());
        dto.setTargetNodeName(hisTask.getTargetNodeName());
        dto.setFormCustom(hisTask.getFormCustom());
        dto.setFormPath(hisTask.getFormPath());
        dto.setCreateTime(hisTask.getCreateTime());
        dto.setUpdateTime(hisTask.getUpdateTime());
        return dto;
    }

    /**
     * 待办任务列表转 DTO 列表
     *
     * @param tasks 待办任务列表
     * @return List&lt;WorkflowTaskDTO&gt;
     */
    public static List<WorkflowTaskDTO> toTaskDTOList(List<? extends Task> tasks) {
        List<WorkflowTaskDTO> dtoList = new ArrayList<>();
        if (tasks == null) {
            return dtoList;
        }
        for (Task task : tasks) {
            dtoList.add(toTaskDTO(task));
        }
        return dtoList;
    }

    /**
     * 历史任务列表转 DTO 列表
     *
     * @param hisTasks 历史任务列表
     * @return List&lt;WorkflowHisTaskDTO&gt;
     */
    public static List<WorkflowHisTaskDTO> toHisTaskDTOList(List<? extends HisTask> hisTasks) {
        List<WorkflowHisTaskDTO> dtoList = new ArrayList<>();
        if (hisTasks == null) {
            return dtoList;
        }
        for (HisTask hisTask : hisTasks) {
            dtoList.add(toHisTaskDTO(hisTask));
        }
        return dtoList;
    }

    /**
     * 流程节点转 DTO
     *
     * @param node 节点
     * @return WorkflowNodeDTO
     */
    public static WorkflowNodeDTO toNodeDTO(Node node) {
        WorkflowNodeDTO dto = new WorkflowNodeDTO();
        dto.setNodeCode(node.getNodeCode());
        dto.setNodeName(node.getNodeName());
        return dto;
    }

    /**
     * 流程节点列表转 DTO 列表
     *
     * @param nodes 节点列表
     * @return List&lt;WorkflowNodeDTO&gt;
     */
    public static List<WorkflowNodeDTO> toNodeDTOList(List<? extends Node> nodes) {
        List<WorkflowNodeDTO> dtoList = new ArrayList<>();
        if (nodes == null) {
            return dtoList;
        }
        for (Node node : nodes) {
            dtoList.add(toNodeDTO(node));
        }
        return dtoList;
    }

    /**
     * 流程实例转 DTO
     *
     * @param instance 流程实例
     * @return WorkflowInstanceDTO
     */
    public static WorkflowInstanceDTO toInstanceDTO(Instance instance) {
        WorkflowInstanceDTO dto = new WorkflowInstanceDTO();
        dto.setInstanceId(instance.getId());
        dto.setDefinitionId(instance.getDefinitionId());
        dto.setFlowName(instance.getFlowName());
        dto.setBusinessId(instance.getBusinessId());
        dto.setNodeCode(instance.getNodeCode());
        dto.setNodeName(instance.getNodeName());
        dto.setNodeType(instance.getNodeType());
        dto.setFlowStatus(instance.getFlowStatus());
        dto.setActivityStatus(instance.getActivityStatus());
        dto.setVariable(instance.getVariableMap());
        dto.setCreateBy(instance.getCreateBy());
        dto.setCreateTime(instance.getCreateTime());
        dto.setUpdateTime(instance.getUpdateTime());
        return dto;
    }

    /**
     * 流程实例列表转 DTO 列表（列表口径：只映射实例实体自身携带的字段）
     * <p>
     * {@code flow_instance} 没有 {@code flow_name} 列，流程名称由 Manager 层按定义 id 批量补齐
     * （见 {@code WorkflowInstanceManagerImpl}），本转换器不查库。
     * </p>
     *
     * @param instances 流程实例列表
     * @return List&lt;WorkflowInstanceDTO&gt;
     */
    public static List<WorkflowInstanceDTO> toInstanceDTOList(List<? extends Instance> instances) {
        List<WorkflowInstanceDTO> dtoList = new ArrayList<>();
        if (instances == null) {
            return dtoList;
        }
        for (Instance instance : instances) {
            dtoList.add(toInstanceDTO(instance));
        }
        return dtoList;
    }

    /**
     * 流程定义转 DTO
     *
     * @param definition 流程定义
     * @return WorkflowDefinitionDTO
     */
    public static WorkflowDefinitionDTO toDefinitionDTO(Definition definition) {
        WorkflowDefinitionDTO dto = new WorkflowDefinitionDTO();
        dto.setDefinitionId(definition.getId());
        dto.setFlowCode(definition.getFlowCode());
        dto.setFlowName(definition.getFlowName());
        dto.setVersion(definition.getVersion());
        dto.setCategory(definition.getCategory());
        dto.setModelValue(definition.getModelValue());
        dto.setIsPublish(definition.getIsPublish());
        dto.setActivityStatus(definition.getActivityStatus());
        dto.setCreateTime(definition.getCreateTime());
        dto.setUpdateTime(definition.getUpdateTime());
        return dto;
    }

    /**
     * 流程定义列表转 DTO 列表
     *
     * @param definitions 流程定义列表
     * @return List&lt;WorkflowDefinitionDTO&gt;
     */
    public static List<WorkflowDefinitionDTO> toDefinitionDTOList(List<? extends Definition> definitions) {
        List<WorkflowDefinitionDTO> dtoList = new ArrayList<>();
        if (definitions == null) {
            return dtoList;
        }
        for (Definition definition : definitions) {
            dtoList.add(toDefinitionDTO(definition));
        }
        return dtoList;
    }

    /**
     * 表单实体转 DTO
     *
     * @param form 表单实体
     * @return WorkflowFormDTO，入参为空时返回 null
     */
    public static WorkflowFormDTO toFormDTO(Form form) {
        if (form == null) {
            return null;
        }
        WorkflowFormDTO dto = new WorkflowFormDTO();
        dto.setFormId(form.getId());
        dto.setFormCode(form.getFormCode());
        dto.setFormName(form.getFormName());
        dto.setVersion(form.getVersion());
        dto.setFormType(form.getFormType());
        dto.setFormContent(form.getFormContent());
        dto.setFormPath(form.getFormPath());
        return dto;
    }

    /**
     * 流程节点转 DTO
     *
     * @param nodeJson 引擎流程图节点
     * @return WorkflowChartNodeDTO
     */
    public static WorkflowChartNodeDTO toChartNodeDTO(NodeJson nodeJson) {
        WorkflowChartNodeDTO dto = new WorkflowChartNodeDTO();
        dto.setNodeCode(nodeJson.getNodeCode());
        dto.setNodeName(nodeJson.getNodeName());
        dto.setNodeType(nodeJson.getNodeType());
        dto.setStatus(nodeJson.getStatus());
        dto.setCoordinate(nodeJson.getCoordinate());
        dto.setAnyNodeSkip(nodeJson.getAnyNodeSkip());
        dto.setSkips(toChartSkipDTOList(nodeJson.getSkipList()));
        return dto;
    }

    /**
     * 流程图节点列表转 DTO 列表
     *
     * @param nodeJsonList 引擎流程图节点列表
     * @return List&lt;WorkflowChartNodeDTO&gt;
     */
    public static List<WorkflowChartNodeDTO> toChartNodeDTOList(List<NodeJson> nodeJsonList) {
        List<WorkflowChartNodeDTO> dtoList = new ArrayList<>();
        if (nodeJsonList == null) {
            return dtoList;
        }
        for (NodeJson nodeJson : nodeJsonList) {
            dtoList.add(toChartNodeDTO(nodeJson));
        }
        return dtoList;
    }

    /**
     * 流程跳转线列表转 DTO 列表
     *
     * @param skipJsonList 引擎流程跳转线列表
     * @return List&lt;WorkflowChartSkipDTO&gt;
     */
    public static List<WorkflowChartSkipDTO> toChartSkipDTOList(List<SkipJson> skipJsonList) {
        List<WorkflowChartSkipDTO> dtoList = new ArrayList<>();
        if (skipJsonList == null) {
            return dtoList;
        }
        for (SkipJson skipJson : skipJsonList) {
            dtoList.add(toChartSkipDTO(skipJson));
        }
        return dtoList;
    }

    private static WorkflowChartSkipDTO toChartSkipDTO(SkipJson skipJson) {
        WorkflowChartSkipDTO dto = new WorkflowChartSkipDTO();
        dto.setNowNodeCode(skipJson.getNowNodeCode());
        dto.setNextNodeCode(skipJson.getNextNodeCode());
        dto.setSkipName(skipJson.getSkipName());
        dto.setSkipType(skipJson.getSkipType());
        dto.setSkipCondition(skipJson.getSkipCondition());
        dto.setStatus(skipJson.getStatus());
        return dto;
    }

}
