package com.carlos.workflow.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.workflow.api.pojo.param.ApiWorkflowInstancePageParam;
import com.carlos.workflow.convert.WorkflowDtoConvert;
import com.carlos.workflow.dto.WorkflowInstanceDTO;
import com.carlos.workflow.manager.WorkflowInstanceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.mapper.FlowDefinitionMapper;
import org.dromara.warm.flow.orm.mapper.FlowInstanceMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 流程实例 查询封装实现类。
 * </p>
 *
 * <h2>为什么「按流程名称过滤」要先查定义</h2>
 * <p>
 * {@code flow_instance} 上<b>没有</b> {@code flow_name} 列（名称只在 {@code flow_definition} 上），
 * 参考实现（RuoYi 版）用一条 LEFT JOIN 的 XML 一次取回；本模块禁止手写 SQL，因此改为
 * <b>「先按名称取出定义 id 集合，再用 {@code definition_id IN (...)} 过滤实例」</b>两步：
 * 定义表里同名流程通常只有个位数版本，id 集合很小，不会把 SQL 撑成超长 IN。
 * </p>
 * <p>
 * <b>名称同时也是展示字段</b>，所以本页实例仍要按定义 id 批量补齐一次 {@code flowName}
 * （与 {@code WorkflowTaskManagerImpl} 同一套做法：一次查询，不按行回查）。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowInstanceManagerImpl extends BaseServiceImpl<FlowInstanceMapper, FlowInstance>
    implements WorkflowInstanceManager {

    private final FlowDefinitionMapper flowDefinitionMapper;

    @Override
    public Paging<WorkflowInstanceDTO> getMyPage(ApiWorkflowInstancePageParam param, String createBy) {
        if (StrUtil.isBlank(createBy)) {
            // 未登录：没有「我发起的」可言，也不该去查库
            return MybatisPage.emptyPaging();
        }
        ApiWorkflowInstancePageParam query =
            param == null ? new ApiWorkflowInstancePageParam() : param;

        List<Long> definitionIds = null;
        if (StrUtil.isNotBlank(query.getFlowName())) {
            definitionIds = listDefinitionIdsByFlowName(query.getFlowName());
            if (definitionIds.isEmpty()) {
                // 没有任何定义匹配该名称：结果必然为空，省掉一次实例查询
                return MybatisPage.emptyPaging();
            }
        }

        LambdaQueryWrapper<FlowInstance> wrapper = queryWrapper();
        wrapper.eq(FlowInstance::getCreateBy, createBy)
            .in(definitionIds != null, FlowInstance::getDefinitionId, definitionIds)
            .eq(StrUtil.isNotBlank(query.getFlowStatus()), FlowInstance::getFlowStatus, query.getFlowStatus())
            .eq(StrUtil.isNotBlank(query.getBusinessId()), FlowInstance::getBusinessId, query.getBusinessId());
        // 分页需要稳定排序：create_time 可能同秒，补 id 作次级键
        wrapper.orderByDesc(FlowInstance::getCreateTime).orderByDesc(FlowInstance::getId);

        PageInfo<FlowInstance> page = page(pageInfo(query), wrapper);
        List<WorkflowInstanceDTO> records = enrichFlowNames(page.getRecords());
        log.debug("分页查询我发起的流程实例: createBy:{}, current:{}, size:{}, total:{}",
            createBy, page.getCurrent(), page.getSize(), page.getTotal());
        return MybatisPage.convert(page, ignored -> records);
    }

    /**
     * 流程名称模糊匹配到的定义 id
     *
     * @param flowName 流程名称（模糊）
     * @return 定义 id 列表；无匹配时为空列表
     */
    private List<Long> listDefinitionIdsByFlowName(String flowName) {
        LambdaQueryWrapper<FlowDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(FlowDefinition::getId).like(FlowDefinition::getFlowName, flowName);
        return flowDefinitionMapper.selectList(wrapper).stream()
            .map(FlowDefinition::getId)
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * 批量补齐本页实例的流程名称
     *
     * @param instances 本页实例
     * @return List&lt;WorkflowInstanceDTO&gt;
     */
    private List<WorkflowInstanceDTO> enrichFlowNames(List<FlowInstance> instances) {
        List<WorkflowInstanceDTO> dtoList = WorkflowDtoConvert.toInstanceDTOList(instances);
        if (dtoList.isEmpty()) {
            return dtoList;
        }
        Map<Long, String> flowNames = mapFlowNames(instances.stream()
            .map(FlowInstance::getDefinitionId).filter(Objects::nonNull).distinct().toList());
        for (WorkflowInstanceDTO dto : dtoList) {
            dto.setFlowName(flowNames.get(dto.getDefinitionId()));
        }
        return dtoList;
    }

    private Map<Long, String> mapFlowNames(List<Long> definitionIds) {
        Map<Long, String> result = new LinkedHashMap<>();
        if (definitionIds.isEmpty()) {
            return result;
        }
        LambdaQueryWrapper<FlowDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(FlowDefinition::getId, FlowDefinition::getFlowName)
            .in(FlowDefinition::getId, definitionIds);
        for (FlowDefinition definition : flowDefinitionMapper.selectList(wrapper)) {
            result.put(definition.getId(), definition.getFlowName());
        }
        return result;
    }

}
