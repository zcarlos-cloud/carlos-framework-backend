package com.carlos.workflow.handler;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.workflow.manager.WorkflowDefinitionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.dto.Tree;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.dromara.warm.flow.ui.service.CategoryService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>
 * 流程设计器的「流程分类」来源：{@code flow_definition.category} 的既有取值。
 * </p>
 *
 * <h2>为什么是「已有定义的分类去重」而不是字典</h2>
 * <p>
 * 首选方案本来是走 {@code carlos-system} 的字典（{@code ApiDict#list(code)}，分类由运营预先维护），
 * 但本任务的文件边界不允许改 {@code carlos-workflow-bus/pom.xml}，而该字典契约
 * （{@code carlos-system-api}）并不在本模块的依赖里——为它新增一条模块依赖同时踩了
 * 「不新增依赖」与「本模块只能新增 handler/Designer*.java」两条红线，因此改用零依赖的
 * {@code flow_definition.category}。代价是：<b>分类只能来自已有定义，不能预先维护</b>——
 * 新分类先随手写在某个定义上，之后就能在下拉里选到。
 * </p>
 * <p>
 * 如果将来允许给本模块加依赖，建议换成字典方案：分类是运营数据，让它在流程定义之外可维护更合理。
 * </p>
 *
 * <h2>parentId 一律为空：分类是单层</h2>
 * <p>
 * 返回的是扁平列表（每个节点 {@code parentId = null}）。引擎在
 * {@code WarmFlowService#queryDef} 里会对本方法的返回值再跑一次
 * {@code TreeUtil.buildTree}，而该方法对「parentId 不在 id 集合内」的节点一律视为顶级——
 * 全空 parentId 得到的仍是一层列表，设计器渲染成单选，这对「分类」正是想要的。
 * 若将来分类要分层，把 {@code parentId} 填上真实父级即可，无需改这里以外的任何代码。
 * </p>
 *
 * <h2>返回值不能是 null</h2>
 * <p>
 * {@code queryDef} 拿到结果直接 {@code TreeUtil.buildTree(treeList)}，null 会 NPE 并被包成
 * {@code FlowException}，整个「打开设计器」的请求就失败了——查不到分类时给空列表，
 * 设计器会显示成可自由输入。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DesignerCategoryService implements CategoryService {

    private final WorkflowDefinitionManager workflowDefinitionManager;

    /**
     * 查询全部流程分类
     *
     * @return 分类列表（扁平、按分类名排序）；查询失败或一个分类都没有时返回空列表
     */
    @Override
    public List<Tree> queryCategory() {
        try {
            // select + groupBy：去重交给数据库，避免把每条定义都拉回来再在内存里 distinct
            LambdaQueryWrapper<FlowDefinition> wrapper = workflowDefinitionManager.queryWrapper()
                .select(FlowDefinition::getCategory)
                .isNotNull(FlowDefinition::getCategory)
                .ne(FlowDefinition::getCategory, StrUtil.EMPTY)
                .groupBy(FlowDefinition::getCategory);
            return workflowDefinitionManager.list(wrapper).stream()
                .map(FlowDefinition::getCategory)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .sorted()
                .map(category -> new Tree().setId(category).setName(category))
                .toList();
        } catch (Exception e) {
            // 引擎会把异常包成 FlowException 让「打开设计器」整体失败；分类只是下拉候选，
            // 拿不到就退回自由输入，不值得让设计器打不开
            log.error("查询流程分类失败，设计器分类下拉将为空（可自由输入）", e);
            return List.of();
        }
    }

}
