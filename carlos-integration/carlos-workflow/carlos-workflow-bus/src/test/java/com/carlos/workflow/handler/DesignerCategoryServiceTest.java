package com.carlos.workflow.handler;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.carlos.workflow.manager.WorkflowDefinitionManager;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.warm.flow.core.dto.Tree;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>
 * {@link DesignerCategoryService} 单元测试：分类来自 {@code flow_definition.category} 的既有取值，
 * 去重、去空、排序，并且<b>任何异常都不许让设计器打不开</b>。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class DesignerCategoryServiceTest {

    private WorkflowDefinitionManager workflowDefinitionManager;

    /**
     * 纯单测没有 MyBatis 启动流程，{@code LambdaQueryWrapper} 的「实体 → 列」缓存要靠这一步初始化，
     * 否则 {@code select(FlowDefinition::getCategory)} 会抛
     * {@code can not find lambda cache for this entity}。
     * 生产链路里这一步由 MapperScan 在容器启动时完成。
     */
    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), FlowDefinition.class.getName()),
            FlowDefinition.class);
    }

    @BeforeEach
    void setUp() {
        workflowDefinitionManager = mock(WorkflowDefinitionManager.class);
        // queryWrapper() 是 BaseService 的 default 方法，mock 不会执行默认实现，这里给一个真的构造器
        when(workflowDefinitionManager.queryWrapper()).thenReturn(Wrappers.lambdaQuery(FlowDefinition.class));
    }

    @Test
    void returnsDistinctCategoriesAsFlatSingleLevelTree() {
        when(workflowDefinitionManager.list(anyWrapper())).thenReturn(List.of(
            definition("人事"), definition("财务"), definition("人事")));

        List<Tree> categories = service().queryCategory();

        assertThat(categories).extracting(Tree::getId)
            .as("去重后按分类名排序（字符串序：人 U+4EBA < 财 U+8D22）")
            .containsExactly("人事", "财务");
        assertThat(categories).extracting(Tree::getName).containsExactly("人事", "财务");
        assertThat(categories)
            .as("parentId 全空 = 单层分类，设计器渲染成单选，这是「分类」想要的形态")
            .allSatisfy(tree -> assertThat(tree.getParentId()).isNull());
    }

    @Test
    void skipsNullAndBlankCategories() {
        when(workflowDefinitionManager.list(anyWrapper())).thenReturn(List.of(
            definition(null), definition(""), definition("   "), definition("人事")));

        assertThat(service().queryCategory()).extracting(Tree::getId).containsExactly("人事");
    }

    @Test
    void returnsEmptyListWhenNoDefinitionCarriesACategory() {
        when(workflowDefinitionManager.list(anyWrapper())).thenReturn(List.of());

        assertThat(service().queryCategory())
            .as("没有分类时返回空列表（不是 null）：引擎会直接 buildTree(返回值)，null 会 NPE 并让设计器打不开")
            .isNotNull()
            .isEmpty();
    }

    @Test
    void returnsEmptyListWhenTheQueryFails() {
        when(workflowDefinitionManager.list(anyWrapper())).thenThrow(new IllegalStateException("database unavailable"));

        assertThat(service().queryCategory())
            .as("分类只是下拉候选，查不到就退回自由输入，不值得让「打开设计器」整体失败")
            .isNotNull()
            .isEmpty();
    }

    @Test
    void queriesTheDatabaseOnceAndAsksTheDatabaseToDoTheDistinct() {
        when(workflowDefinitionManager.list(anyWrapper())).thenReturn(List.of(definition("人事")));

        service().queryCategory();

        ArgumentCaptor<LambdaQueryWrapper<FlowDefinition>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(workflowDefinitionManager).list(captor.capture());

        assertThat(captor.getValue().getSqlSegment())
            .as("去重交给数据库（GROUP BY category），而不是把每条定义拉回来在内存里去重")
            .contains("GROUP BY category")
            .as("category 为 null 或空串的定义不算分类")
            .contains("category IS NOT NULL");
    }

    private DesignerCategoryService service() {
        return new DesignerCategoryService(workflowDefinitionManager);
    }

    /**
     * {@code IRepository} 同时有 {@code list(Wrapper)} 与 {@code list(IPage)} 两个重载，
     * 裸 {@code any()} 会歧义，这里把类型钉死
     */
    private static Wrapper<FlowDefinition> anyWrapper() {
        return ArgumentMatchers.any();
    }

    private static FlowDefinition definition(String category) {
        FlowDefinition definition = new FlowDefinition();
        definition.setCategory(category);
        return definition;
    }

}
