package com.carlos.workflow.config;

import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgDepartment;
import com.carlos.org.api.ApiOrgRole;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.position.api.ApiOrgUserPosition;
import com.carlos.workflow.handler.OrgPermissionHandler;
import com.carlos.workflow.handler.WorkflowAssigneeFailFastListener;
import com.carlos.workflow.handler.WorkflowAssignees;
import com.carlos.workflow.manager.WorkflowDefinitionManager;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.dromara.warm.flow.core.listener.GlobalListener;
import org.dromara.warm.flow.core.service.ChartService;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.service.HisTaskService;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 办理人解析相关 Bean 的装配测试，验证四件事：
 * </p>
 * <ol>
 *   <li>org Feign 契约在位时，{@link OrgPermissionHandler} 与 {@link WorkflowAssignees} 被注册，
 *       且后者的 Bean 名就是 SpEL 契约 {@code workflowAssignees}；</li>
 *   <li>宿主自带 {@code PermissionHandler} 时生产实现退让（{@code @ConditionalOnMissingBean}）；</li>
 *   <li>org Feign 契约缺席（宿主未开启 {@code @EnableFeignClients}）时不注册，
 *       而不是装配一个调不通 org 的处理器；</li>
 *   <li>{@code WorkflowAssigneeFailFastListener} 作为 {@code GlobalListener} 注册（不依赖 org 契约），
 *       宿主自带 {@code GlobalListener} 时退让。</li>
 * </ol>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowAssigneeWiringTest {

    /**
     * 只带自动配置与引擎 Bean 桩的上下文，org 契约由各用例按需追加
     */
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(WorkflowAutoConfiguration.class))
        .withBean(TaskService.class, () -> mock(TaskService.class))
        .withBean(HisTaskService.class, () -> mock(HisTaskService.class))
        .withBean(InsService.class, () -> mock(InsService.class))
        .withBean(DefService.class, () -> mock(DefService.class))
        .withBean(ChartService.class, () -> mock(ChartService.class))
        // 流程定义服务（本类不测它）依赖 Manager，缺这个桩 WorkflowAutoConfiguration 起不来
        .withBean(WorkflowDefinitionManager.class, () -> mock(WorkflowDefinitionManager.class));

    @Test
    void registersOrgPermissionHandlerWhenOrgContractsArePresent() {
        withOrgContracts(runner)
            .run(context -> {
                assertThat(context).hasSingleBean(OrgPermissionHandler.class);
                assertThat(context.getBean(PermissionHandler.class)).isInstanceOf(OrgPermissionHandler.class);
            });
    }

    @Test
    void registersSpelAssigneesHelperUnderItsBeanName() {
        withOrgContracts(runner)
            .run(context -> {
                assertThat(context).hasSingleBean(WorkflowAssignees.class);
                assertThat(context)
                    .as("SpEL 的 @workflowAssignees 按 Bean 名解析，Bean 名必须等于规范里写的名字")
                    .hasBean("workflowAssignees");
            });
    }

    @Test
    void backsOffWhenHostProvidesItsOwnPermissionHandler() {
        withOrgContracts(runner)
            .withBean("hostPermissionHandler", PermissionHandler.class, () -> mock(PermissionHandler.class))
            .run(context -> {
                assertThat(context.getBeanNamesForType(OrgPermissionHandler.class))
                    .as("宿主可提供自己的实现覆盖生产实现")
                    .isEmpty();
                assertThat(context).hasSingleBean(PermissionHandler.class);
            });
    }

    @Test
    void doesNotRegisterPermissionHandlerWithoutOrgContracts() {
        runner.run(context -> {
            assertThat(context.getBeanNamesForType(OrgPermissionHandler.class))
                .as("org Feign 客户端未注册时不应装配处理器：它在运行期调不通 org，只会把审批判成无权限")
                .isEmpty();
            assertThat(context).doesNotHaveBean(WorkflowAssignees.class);
        });
    }

    @Test
    void doesNotRegisterPermissionHandlerWithoutUserPositionContract() {
        runner
            .withBean(ApiOrgRole.class, () -> mock(ApiOrgRole.class))
            .withBean(ApiOrgDepartment.class, () -> mock(ApiOrgDepartment.class))
            .run(context -> assertThat(context.getBeanNamesForType(OrgPermissionHandler.class))
                .as("position: 词汇依赖用户岗位契约，缺一个依赖就不能装配一个「半边可用」的处理器")
                .isEmpty());
    }

    @Test
    void registersFailFastGlobalListenerAsBean() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(WorkflowAssigneeFailFastListener.class);
            assertThat(context.getBean(GlobalListener.class))
                .as("节点配了办理人却解析不到人时，必须由这个全局监听器在创建待办前拦住；"
                    + "引擎只认一个 GlobalListener，注册成 Bean 才会被 FlowEngine 取到")
                .isInstanceOf(WorkflowAssigneeFailFastListener.class);
        });
    }

    @Test
    void failFastGlobalListenerStaysWhenOrgContractsAreAbsent() {
        runner.run(context -> assertThat(context.getBeanNamesForType(WorkflowAssigneeFailFastListener.class))
            .as("拦截逻辑只看节点配置与展开结果，不调 org，因此不依赖 org Feign 契约")
            .isNotEmpty());
    }

    @Test
    void backsOffWhenHostProvidesItsOwnGlobalListener() {
        runner
            .withBean("hostGlobalListener", GlobalListener.class, () -> mock(GlobalListener.class))
            .run(context -> {
                assertThat(context.getBeanNamesForType(WorkflowAssigneeFailFastListener.class))
                    .as("宿主可提供自己的全局监听器覆盖本实现")
                    .isEmpty();
                assertThat(context).hasSingleBean(GlobalListener.class);
            });
    }

    @Test
    void wiresUserPositionContractIntoPermissionHandler() {
        ApiOrgUserPosition apiOrgUserPosition = mock(ApiOrgUserPosition.class);
        OrgUserAO user = new OrgUserAO();
        user.setId(77L);
        user.setUserStatus(1);
        when(apiOrgUserPosition.listUsersByPositionId(7L)).thenReturn(Result.success(List.of(user)));

        withOrgContracts(runner, apiOrgUserPosition).run(context ->
            assertThat(context.getBean(OrgPermissionHandler.class).convertPermissions(List.of("position:7")))
                .as("装配后的处理器必须真的能展开 position: 词汇（第三个构造参数确实接上了）")
                .containsExactly("77"));
    }

    private ApplicationContextRunner withOrgContracts(ApplicationContextRunner contextRunner) {
        return withOrgContracts(contextRunner, mock(ApiOrgUserPosition.class));
    }

    private ApplicationContextRunner withOrgContracts(ApplicationContextRunner contextRunner,
                                                      ApiOrgUserPosition apiOrgUserPosition) {
        return contextRunner
            .withBean(ApiOrgRole.class, () -> mock(ApiOrgRole.class))
            .withBean(ApiOrgDepartment.class, () -> mock(ApiOrgDepartment.class))
            .withBean(ApiOrgUserPosition.class, () -> apiOrgUserPosition);
    }

}
