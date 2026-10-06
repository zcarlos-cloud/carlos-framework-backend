package com.carlos.workflow.config;

import com.carlos.org.api.ApiOrgDepartment;
import com.carlos.org.api.ApiOrgRole;
import com.carlos.org.position.api.ApiOrgUserPosition;
import com.carlos.workflow.handler.OrgPermissionHandler;
import com.carlos.workflow.handler.WorkflowAssigneeFailFastListener;
import com.carlos.workflow.handler.WorkflowAssignees;
import com.carlos.workflow.manager.WorkflowDefinitionManager;
import com.carlos.workflow.service.WorkflowDefinitionService;
import com.carlos.workflow.service.WorkflowInstanceService;
import com.carlos.workflow.service.WorkflowTaskService;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.dromara.warm.flow.core.listener.GlobalListener;
import org.dromara.warm.flow.core.service.ChartService;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.service.HisTaskService;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.TaskService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * <p>
 * 工作流服务层装配，只注册 Carlos 侧的扩展 Bean。
 * </p>
 * <p>
 * 引擎自身的 DAO、Service、{@code initFlow} 等 Bean 由 Warm-Flow 的
 * {@code FlowAutoConfig} / {@code BeanConfig} 装配，本类<b>不重复注册</b>，
 * 服务层通过构造器注入这些引擎 Bean。
 * </p>
 * <p>
 * 控制器在本模块内，由启动类的组件扫描装配，不在本类中再以 {@code @Bean} 注册——重复注册会产生
 * 同一路径的两个 MVC handler 并抛出 {@code Ambiguous mapping}：
 * {@code com.carlos.workflow.apiimpl} 下是实现 Feign 契约的服务间端点（{@code /api/workflow/**}，
 * 不面向前端），{@code com.carlos.workflow.controller} 下是前端管理端点（{@code /workflow/**}，经网关）。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(WorkflowProperties.class)
@ConditionalOnProperty(prefix = "carlos.workflow", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WorkflowAutoConfiguration {

    /**
     * 待办任务服务（审批页加载 / 已办详情 / 统一审批动作）
     *
     * @param taskService    引擎待办任务服务
     * @param hisTaskService 引擎历史任务服务
     * @return WorkflowTaskService
     */
    @Bean
    @ConditionalOnMissingBean
    public WorkflowTaskService workflowTaskService(TaskService taskService, HisTaskService hisTaskService) {
        return new WorkflowTaskService(taskService, hisTaskService);
    }

    /**
     * 流程实例服务（发起 / 流程图）
     *
     * @param instanceService   引擎流程实例服务（Bean 名为 instanceService，不是 insService）
     * @param definitionService 引擎流程定义服务
     * @param chartService      引擎流程图服务
     * @return WorkflowInstanceService
     */
    @Bean
    @ConditionalOnMissingBean
    public WorkflowInstanceService workflowInstanceService(InsService instanceService, DefService definitionService,
                                                           ChartService chartService) {
        return new WorkflowInstanceService(instanceService, definitionService, chartService);
    }

    /**
     * 流程定义服务（已发布清单 / 保存发布 / 管理端分页与增删改）
     *
     * @param definitionService 引擎流程定义服务
     * @param definitionManager 流程定义查询封装（管理端分页与详情）
     * @return WorkflowDefinitionService
     */
    @Bean
    @ConditionalOnMissingBean
    public WorkflowDefinitionService workflowDefinitionService(DefService definitionService,
                                                               WorkflowDefinitionManager definitionManager) {
        return new WorkflowDefinitionService(definitionService, definitionManager);
    }

    /*
     * ============================================================================
     * 办理人解析（org.dromara.warm.flow.core.handler.PermissionHandler）
     *
     * 引擎的 TaskServiceImpl#checkAuth 会取「当前用户的权限标识」（permissions()）与任务的
     * flow_user.processed_by（convertPermissions() 展开后的结果）求交集；节点配了办理人而权限
     * 标识为空时，审批动作必然以 20704 失败。因此生产装配必须给出真实实现，
     * 返回空列表的兜底实现比不注册更难排查。
     *
     * ⚠️ warm-flow.permission-handler-path 必须保持为空：
     *   该配置项走 Class#getConstructor() + newInstance()，绕过 Spring 注入，
     *   而 OrgPermissionHandler 依赖 org Feign 客户端，没有无参构造器。
     *   引擎在按路径实例化失败后会回退到按类型取 Spring Bean（FlowEngine#initBean）。
     * ============================================================================
     */

    /**
     * 办理人权限处理器：把 {@code role:5} / {@code dept:12} / {@code position:7} 等表达式展开成用户 id。
     *
     * <p>{@code @ConditionalOnMissingBean} 允许宿主提供自己的实现覆盖；
     * {@code @ConditionalOnBean} 保证 org Feign 客户端未注册（宿主未开启
     * {@code @EnableFeignClients}）时不强行装配一个调不通 org 的处理器。
     * carlos-spring-cloud-starter 的 {@code FeignConfig} 已声明
     * {@code @EnableFeignClients(basePackages = "com.carlos")}，云端部署天然满足。</p>
     *
     * @param apiOrgRole         角色 Feign 契约（role:）
     * @param apiOrgDepartment   部门 Feign 契约（dept: / dept:rec / deptLeader: / roleInDept:）
     * @param apiOrgUserPosition 用户岗位 Feign 契约（position:）
     * @return OrgPermissionHandler
     */
    @Bean
    @ConditionalOnMissingBean(PermissionHandler.class)
    @ConditionalOnBean({ApiOrgRole.class, ApiOrgDepartment.class, ApiOrgUserPosition.class})
    public OrgPermissionHandler orgPermissionHandler(ApiOrgRole apiOrgRole, ApiOrgDepartment apiOrgDepartment,
                                                     ApiOrgUserPosition apiOrgUserPosition) {
        return new OrgPermissionHandler(apiOrgRole, apiOrgDepartment, apiOrgUserPosition);
    }

    /**
     * 动态办理人 SpEL 助手，Bean 名 {@code workflowAssignees} 即 {@code #{@workflowAssignees.xxx}} 的契约。
     *
     * <p>方法名不能改：SpEL 的 {@code @beanName} 按 Bean 名解析，改名会让已发布流程定义里的
     * 表达式一起失效。</p>
     *
     * @param apiOrgUserPosition 用户岗位 Feign 契约（report_to 汇报线）
     * @return WorkflowAssignees
     */
    @Bean
    @ConditionalOnMissingBean(WorkflowAssignees.class)
    @ConditionalOnBean(ApiOrgUserPosition.class)
    public WorkflowAssignees workflowAssignees(ApiOrgUserPosition apiOrgUserPosition) {
        return new WorkflowAssignees(apiOrgUserPosition);
    }

    /*
     * ============================================================================
     * 办理人失败即拒（org.dromara.warm.flow.core.listener.GlobalListener）
     *
     * 节点配了办理人却展开为空时，引擎的 checkAuth 对「办理人为空」的任务整段短路，
     * 该节点等于对所有人开放，且不会出现在任何人的待办里；引擎自己的兜底（表达式求值为空时
     * 原样保留表达式字符串）只会让审批谁也批不了，同样只能靠 WARN 日志发现。
     *
     * 拦截点选 assignment 而不是 create：assignment 在写库前触发（TaskServiceImpl#skip 216 行、
     * InsServiceImpl#start 100 行），create 在写库后（231 / 107 行），而引擎的流转服务（core +
     * mybatis-plus-core）没有任何 @Transactional——从本模块的审批入口进来时也没有事务，
     * create 里抛异常拦不住已提交的流转。详见 WorkflowAssigneeFailFastListener 类注释。
     *
     * ⚠️ warm-flow.global-listener-path 必须保持为空：
     *   该配置项同样走 Class#getConstructor() + newInstance()，绕过 Spring 注入，
     *   而本实现依赖 WorkflowProperties，没有无参构造器。
     * ============================================================================
     */

    /**
     * 办理人失败即拒全局监听器：节点配了办理人却解析不到任何人时拒绝创建待办任务
     * （{@code carlos.workflow.assignee.fail-fast=false} 可降级为仅告警）。
     *
     * <p>引擎只认一个 {@code GlobalListener}，{@code @ConditionalOnMissingBean} 让宿主可以换成自己的实现。</p>
     *
     * @param workflowProperties 工作流配置（读取降级开关）
     * @return WorkflowAssigneeFailFastListener
     */
    @Bean
    @ConditionalOnMissingBean(GlobalListener.class)
    public WorkflowAssigneeFailFastListener workflowAssigneeFailFastListener(WorkflowProperties workflowProperties) {
        return new WorkflowAssigneeFailFastListener(workflowProperties);
    }

}
