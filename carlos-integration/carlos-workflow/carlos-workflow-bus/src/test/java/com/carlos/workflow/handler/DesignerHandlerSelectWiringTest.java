package com.carlos.workflow.handler;

import com.carlos.workflow.manager.WorkflowDefinitionManager;
import org.dromara.warm.flow.ui.dto.HandlerQuery;
import org.dromara.warm.flow.ui.service.CategoryService;
import org.dromara.warm.flow.ui.service.HandlerSelectService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * <p>
 * 设计器 SPI 的<b>组件扫描装配</b>测试：不写任何 {@code @Bean}，只靠
 * {@code com.carlos.workflow.handler} 包上的 {@code @Component} 被发现，
 * 且 org Feign 契约<b>完全缺席</b>时仍能装配成功。
 * </p>
 *
 * <h2>为什么这条测试重要</h2>
 * <p>
 * 引擎用 {@code FrameInvoker.getBean(HandlerSelectService.class)} 按类型取实现，
 * 取不到就等同这两个下拉框不存在（这正是本次改动的起因）。而这两个实现依赖 org 的 Feign 客户端，
 * 后者只在宿主开启 {@code @EnableFeignClients} 时才存在：
 * </p>
 * <ul>
 *   <li>如果按普通构造器直接注入 org 契约，org 缺席时<b>整个上下文启动失败</b>——
 *       比「下拉框是空的」严重得多；</li>
 *   <li>因此实现改用 {@code ObjectProvider} 注入，本测试就是钉住这一点：没有 org 契约时
 *       上下文照常起来，页签照常列出，取数降级为空。</li>
 * </ul>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class DesignerHandlerSelectWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(HandlerPackageScan.class)
        // CategoryService 依赖流程定义 Manager；本测试只关心装配，给一个桩
        .withBean(WorkflowDefinitionManager.class, () -> mock(WorkflowDefinitionManager.class));

    @Test
    void componentsAreDiscoveredAndResolvableByTheTypeTheEngineAsksFor() {
        runner.run(context -> {
            assertThat(context)
                .as("引擎按类型取 HandlerSelectService，取不到就不存在「办理人选择」这个功能")
                .hasSingleBean(HandlerSelectService.class);
            assertThat(context.getBean(HandlerSelectService.class)).isInstanceOf(DesignerHandlerSelectService.class);

            assertThat(context)
                .as("分类下拉同理，按 CategoryService 类型取")
                .hasSingleBean(CategoryService.class);
            assertThat(context.getBean(CategoryService.class)).isInstanceOf(DesignerCategoryService.class);
        });
    }

    @Test
    void tabsAreListedEvenWhenTheOrgContractsAreAbsent() {
        runner.run(context -> assertThat(context.getBean(HandlerSelectService.class).getHandlerType())
            .as("页签名不依赖 org，org 不可用时应表现为「页签在、列表空」，而不是功能整体消失")
            .containsExactly("用户", "角色", "部门", "部门(含下级)", "岗位"));
    }

    @Test
    void queriesDegradeToEmptyInsteadOfFailingWhenTheOrgContractsAreAbsent() {
        runner.run(context -> {
            HandlerSelectService service = context.getBean(HandlerSelectService.class);

            service.getHandlerType().forEach(label -> assertThat(
                service.getHandlerSelect(designerQuery(label)).getHandlerAuths().getRows())
                .as("[%s] 页签在没有 org 契约时返回空列表，而不是抛异常让弹窗卡在加载态", label)
                .isEmpty());

            assertThat(service.handlerFeedback(List.of("role:5", "dept:12:rec", "1001")))
                .as("回显在没有 org 契约时给出空名字，长度与顺序不变")
                .hasSize(3)
                .allSatisfy(feedback -> assertThat(feedback.getHandlerName()).isEmpty());
        });
    }

    private static HandlerQuery designerQuery(String handlerType) {
        HandlerQuery query = new HandlerQuery();
        query.setHandlerType(handlerType);
        return query;
    }

    /**
     * 只扫描 handler 包，模拟宿主「组件扫描 + 没有 @EnableFeignClients」的装配环境
     */
    @Configuration
    @ComponentScan("com.carlos.workflow.handler")
    static class HandlerPackageScan {
    }

}
