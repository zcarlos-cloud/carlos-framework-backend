package com.carlos.workflow.convert;

import com.carlos.workflow.api.pojo.ao.WorkflowCandidateAO;
import com.carlos.workflow.api.pojo.ao.WorkflowChartAO;
import com.carlos.workflow.api.pojo.ao.WorkflowChartNodeAO;
import com.carlos.workflow.api.pojo.ao.WorkflowChartSkipAO;
import com.carlos.workflow.api.pojo.ao.WorkflowDefinitionAO;
import com.carlos.workflow.api.pojo.ao.WorkflowFormAO;
import com.carlos.workflow.api.pojo.ao.WorkflowHandleResultAO;
import com.carlos.workflow.api.pojo.ao.WorkflowHisTaskAO;
import com.carlos.workflow.api.pojo.ao.WorkflowInstanceAO;
import com.carlos.workflow.api.pojo.ao.WorkflowNodeAO;
import com.carlos.workflow.api.pojo.ao.WorkflowTaskAO;
import com.carlos.workflow.dto.WorkflowCandidateDTO;
import com.carlos.workflow.dto.WorkflowChartDTO;
import com.carlos.workflow.dto.WorkflowChartNodeDTO;
import com.carlos.workflow.dto.WorkflowChartSkipDTO;
import com.carlos.workflow.dto.WorkflowDefinitionDTO;
import com.carlos.workflow.dto.WorkflowFormDTO;
import com.carlos.workflow.dto.WorkflowHandleResultDTO;
import com.carlos.workflow.dto.WorkflowHisTaskDTO;
import com.carlos.workflow.dto.WorkflowInstanceDTO;
import com.carlos.workflow.dto.WorkflowNodeDTO;
import com.carlos.workflow.dto.WorkflowTaskDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 服务层 DTO 到 API 响应对象（AO）的转换。
 * </p>
 * <p>
 * 与 {@link WorkflowDtoConvert} 的分工：后者把 Warm-Flow 引擎对象翻译成服务层 DTO，
 * 本转换器只负责把服务层 DTO 投影成对外契约（AO），二者各自独立，互不越界。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Mapper
public interface WorkflowAoConvert {

    WorkflowAoConvert INSTANCE = Mappers.getMapper(WorkflowAoConvert.class);

    /**
     * 待办任务 DTO 转 AO
     *
     * @param dto 待办任务 DTO
     * @return WorkflowTaskAO
     */
    WorkflowTaskAO toTaskAO(WorkflowTaskDTO dto);

    /**
     * 待办任务 DTO 列表转 AO 列表
     *
     * @param dtoList 待办任务 DTO 列表
     * @return List&lt;WorkflowTaskAO&gt;
     */
    List<WorkflowTaskAO> toTaskAOList(List<WorkflowTaskDTO> dtoList);

    /**
     * 历史任务 DTO 转 AO
     *
     * @param dto 历史任务 DTO
     * @return WorkflowHisTaskAO
     */
    WorkflowHisTaskAO toHisTaskAO(WorkflowHisTaskDTO dto);

    /**
     * 历史任务 DTO 列表转 AO 列表
     *
     * @param dtoList 历史任务 DTO 列表
     * @return List&lt;WorkflowHisTaskAO&gt;
     */
    List<WorkflowHisTaskAO> toHisTaskAOList(List<WorkflowHisTaskDTO> dtoList);

    /**
     * 流程节点 DTO 列表转 AO 列表
     *
     * @param dtoList 流程节点 DTO 列表
     * @return List&lt;WorkflowNodeAO&gt;
     */
    List<WorkflowNodeAO> toNodeAOList(List<WorkflowNodeDTO> dtoList);

    /**
     * 候选办理人 DTO 列表转 AO 列表
     *
     * @param dtoList 候选办理人 DTO 列表
     * @return List&lt;WorkflowCandidateAO&gt;
     */
    List<WorkflowCandidateAO> toCandidateAOList(List<WorkflowCandidateDTO> dtoList);

    /**
     * 流程实例 DTO 转 AO
     *
     * @param dto 流程实例 DTO
     * @return WorkflowInstanceAO
     */
    WorkflowInstanceAO toInstanceAO(WorkflowInstanceDTO dto);

    /**
     * 流程实例 DTO 列表转 AO 列表
     *
     * @param dtoList 流程实例 DTO 列表
     * @return List&lt;WorkflowInstanceAO&gt;
     */
    List<WorkflowInstanceAO> toInstanceAOList(List<WorkflowInstanceDTO> dtoList);

    /**
     * 审批动作执行结果 DTO 转 AO
     *
     * @param dto 审批动作执行结果 DTO
     * @return WorkflowHandleResultAO
     */
    WorkflowHandleResultAO toHandleResultAO(WorkflowHandleResultDTO dto);

    /**
     * 流程定义 DTO 转 AO
     *
     * @param dto 流程定义 DTO
     * @return WorkflowDefinitionAO
     */
    WorkflowDefinitionAO toDefinitionAO(WorkflowDefinitionDTO dto);

    /**
     * 流程定义 DTO 列表转 AO 列表
     *
     * @param dtoList 流程定义 DTO 列表
     * @return List&lt;WorkflowDefinitionAO&gt;
     */
    List<WorkflowDefinitionAO> toDefinitionAOList(List<WorkflowDefinitionDTO> dtoList);

    /**
     * 表单 DTO 转 AO
     *
     * @param dto 表单 DTO
     * @return WorkflowFormAO
     */
    WorkflowFormAO toFormAO(WorkflowFormDTO dto);

    /**
     * 流程图 DTO 转 AO
     *
     * @param dto 流程图 DTO
     * @return WorkflowChartAO
     */
    WorkflowChartAO toChartAO(WorkflowChartDTO dto);

    /**
     * 流程图节点 DTO 转 AO
     *
     * @param dto 流程图节点 DTO
     * @return WorkflowChartNodeAO
     */
    WorkflowChartNodeAO toChartNodeAO(WorkflowChartNodeDTO dto);

    /**
     * 流程图跳转线 DTO 转 AO
     *
     * @param dto 流程图跳转线 DTO
     * @return WorkflowChartSkipAO
     */
    WorkflowChartSkipAO toChartSkipAO(WorkflowChartSkipDTO dto);

}
