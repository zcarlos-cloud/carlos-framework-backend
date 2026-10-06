package com.carlos.workflow.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.workflow.api.pojo.param.ApiWorkflowDefinitionPageParam;
import com.carlos.workflow.convert.WorkflowDtoConvert;
import com.carlos.workflow.dto.WorkflowDefinitionDTO;
import com.carlos.workflow.manager.WorkflowDefinitionManager;
import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.dromara.warm.flow.orm.mapper.FlowDefinitionMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 流程定义 查询封装实现类。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Slf4j
@Component
public class WorkflowDefinitionManagerImpl extends BaseServiceImpl<FlowDefinitionMapper, FlowDefinition>
    implements WorkflowDefinitionManager {

    @Override
    public Paging<WorkflowDefinitionDTO> getPage(ApiWorkflowDefinitionPageParam param) {
        LambdaQueryWrapper<FlowDefinition> wrapper = new LambdaQueryWrapper<>();
        if (param != null) {
            wrapper.like(StrUtil.isNotBlank(param.getFlowName()), FlowDefinition::getFlowName, param.getFlowName())
                .eq(StrUtil.isNotBlank(param.getFlowCode()), FlowDefinition::getFlowCode, param.getFlowCode())
                .eq(StrUtil.isNotBlank(param.getCategory()), FlowDefinition::getCategory, param.getCategory())
                .eq(param.getIsPublish() != null, FlowDefinition::getIsPublish, param.getIsPublish());
        }
        wrapper.orderByDesc(FlowDefinition::getCreateTime);

        PageInfo<FlowDefinition> page = page(pageInfo(param), wrapper);
        log.debug("分页查询流程定义: current:{}, size:{}, total:{}", page.getCurrent(), page.getSize(), page.getTotal());
        return MybatisPage.convert(page, WorkflowDtoConvert::toDefinitionDTOList);
    }

    @Override
    public WorkflowDefinitionDTO getDtoById(Long definitionId) {
        if (definitionId == null) {
            log.warn("definitionId can't be null");
            return null;
        }
        FlowDefinition entity = getBaseMapper().selectById(definitionId);
        return entity == null ? null : WorkflowDtoConvert.toDefinitionDTO(entity);
    }

    @Override
    public List<WorkflowDefinitionDTO> listByFlowCode(String flowCode) {
        if (StrUtil.isBlank(flowCode)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<FlowDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowDefinition::getFlowCode, flowCode);
        return WorkflowDtoConvert.toDefinitionDTOList(list(wrapper));
    }

}
