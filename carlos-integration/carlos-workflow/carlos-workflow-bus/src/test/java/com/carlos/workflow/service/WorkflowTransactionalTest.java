package com.carlos.workflow.service;

import com.carlos.workflow.api.pojo.param.ApiWorkflowDefinitionBasicParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowDefinitionSaveParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowHandleParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowInstanceActionParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowInstancePageParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowStartParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowTaskPageParam;
import com.carlos.workflow.apiimpl.ApiWorkflowDefinitionImpl;
import com.carlos.workflow.apiimpl.ApiWorkflowInstanceImpl;
import com.carlos.workflow.apiimpl.ApiWorkflowTaskImpl;
import com.carlos.workflow.controller.WorkflowDefinitionController;
import com.carlos.workflow.controller.WorkflowInstanceController;
import com.carlos.workflow.controller.WorkflowTaskController;
import com.carlos.workflow.poc.WorkflowContextStartupTest;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * 写操作的事务断言：引擎一次流转会写多张表（{@code flow_task} / {@code flow_his_task} /
 * {@code flow_user} / {@code flow_instance}），而 Warm-Flow 自身的 Service 没有任何
 * {@code @Transactional}，中途失败会留下脏数据。因此「写方法必须开着事务」这件事需要被钉住：
 * </p>
 * <ol>
 *   <li>Service 层的每个写方法都带 {@code @Transactional(rollbackFor = Exception.class)}
 *       （只写 {@code @Transactional} 时默认只回滚 {@code RuntimeException}）；</li>
 *   <li>Controller 层不得加事务注解（事务边界必须落在业务方法内，而不是 Web 入口）；</li>
 *   <li>上下文里真的存在 {@code PlatformTransactionManager}——否则注解只是注释，
 *       代理会静默退化为无事务执行。</li>
 * </ol>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@SpringBootTest(
    classes = WorkflowContextStartupTest.TestApplication.class,
    properties = {
        "spring.datasource.dynamic.primary=master",
        "spring.datasource.dynamic.datasource.master.url=jdbc:h2:mem:carlos_workflow_poc;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.dynamic.datasource.master.driver-class-name=org.h2.Driver",
        "spring.datasource.dynamic.datasource.master.username=sa",
        "spring.datasource.dynamic.datasource.master.password=",
        "spring.application.name=carlos-workflow",
        "carlos.mybatis.db-type=dm",
        "carlos.workflow.enabled=true",
        "warm-flow.banner=false",
        "warm-flow.permission-handler-path="
    }
)
class WorkflowTransactionalTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void workflowTaskServiceHandleIsTransactional() {
        assertTransactional(WorkflowTaskService.class, "handle", ApiWorkflowHandleParam.class);
    }

    @Test
    void workflowInstanceServiceWriteMethodsAreTransactional() {
        assertTransactional(WorkflowInstanceService.class, "start", ApiWorkflowStartParam.class);
        assertTransactional(WorkflowInstanceService.class, "revoke", ApiWorkflowInstanceActionParam.class);
        assertTransactional(WorkflowInstanceService.class, "terminate", ApiWorkflowInstanceActionParam.class);
        assertTransactional(WorkflowInstanceService.class, "takeBack", ApiWorkflowInstanceActionParam.class);
        assertTransactional(WorkflowInstanceService.class, "suspend", Long.class);
        assertTransactional(WorkflowInstanceService.class, "activate", Long.class);
    }

    @Test
    void workflowDefinitionServiceWriteMethodsAreTransactional() {
        assertTransactional(WorkflowDefinitionService.class, "save", ApiWorkflowDefinitionSaveParam.class);
        assertTransactional(WorkflowDefinitionService.class, "createBasic", ApiWorkflowDefinitionBasicParam.class);
        assertTransactional(WorkflowDefinitionService.class, "updateBasic", ApiWorkflowDefinitionBasicParam.class);
        assertTransactional(WorkflowDefinitionService.class, "publish", List.class);
        assertTransactional(WorkflowDefinitionService.class, "unpublish", List.class);
        assertTransactional(WorkflowDefinitionService.class, "copy", Long.class);
        assertTransactional(WorkflowDefinitionService.class, "remove", List.class);
        assertTransactional(WorkflowDefinitionService.class, "importDefinition", MultipartFile.class);
        assertTransactional(WorkflowDefinitionService.class, "activate", Long.class);
        assertTransactional(WorkflowDefinitionService.class, "suspend", Long.class);
    }

    @Test
    void readMethodsAndControllersStayOutOfTransaction() {
        assertNotTransactional(WorkflowTaskService.class, "load", Long.class);
        assertNotTransactional(WorkflowTaskService.class, "todoPage", ApiWorkflowTaskPageParam.class);
        assertNotTransactional(WorkflowInstanceService.class, "chart", Long.class);
        assertNotTransactional(WorkflowInstanceService.class, "listAnyNode", Long.class);
        assertNotTransactional(WorkflowInstanceService.class, "myPage", ApiWorkflowInstancePageParam.class);
        assertNotTransactional(WorkflowDefinitionService.class, "exportDefinition", Long.class);

        for (Class<?> controller : List.of(ApiWorkflowTaskImpl.class, ApiWorkflowInstanceImpl.class,
            ApiWorkflowDefinitionImpl.class, WorkflowTaskController.class,
            WorkflowInstanceController.class, WorkflowDefinitionController.class)) {
            for (Method method : controller.getDeclaredMethods()) {
                assertThat(AnnotatedElementUtils.findMergedAnnotation(method, Transactional.class))
                    .as("%s#%s 不应带事务注解：事务边界属于 Service 层", controller.getSimpleName(), method.getName())
                    .isNull();
            }
        }
    }

    @Test
    void aTransactionManagerIsActuallyWired() {
        assertThat(applicationContext.getBean(PlatformTransactionManager.class))
            .as("没有事务管理器时 @Transactional 只是装饰，代理不会真正开启事务")
            .isNotNull();
    }

    @Test
    void writeServicesAreProxiedSoTheAnnotationsTakeEffect() {
        assertThat(AopUtils.isAopProxy(applicationContext.getBean(WorkflowTaskService.class)))
            .as("WorkflowTaskService 由 @Bean 注册，@Transactional 生效的前提是容器为它建了代理")
            .isTrue();
        assertThat(AopUtils.isAopProxy(applicationContext.getBean(WorkflowInstanceService.class))).isTrue();
        assertThat(AopUtils.isAopProxy(applicationContext.getBean(WorkflowDefinitionService.class))).isTrue();
    }

    private void assertTransactional(Class<?> type, String methodName, Class<?>... parameterTypes) {
        Method method = methodOf(type, methodName, parameterTypes);
        Transactional transactional = AnnotatedElementUtils.findMergedAnnotation(method, Transactional.class);
        assertThat(transactional)
            .as("%s#%s 是写方法，必须开启事务", type.getSimpleName(), methodName)
            .isNotNull();
        assertThat(transactional.rollbackFor())
            .as("%s#%s 必须声明 rollbackFor = Exception.class，否则受检异常不会回滚", type.getSimpleName(), methodName)
            .contains(Exception.class);
    }

    private void assertNotTransactional(Class<?> type, String methodName, Class<?>... parameterTypes) {
        assertThat(AnnotatedElementUtils.findMergedAnnotation(methodOf(type, methodName, parameterTypes),
            Transactional.class))
            .as("%s#%s 是只读方法，不该占用事务", type.getSimpleName(), methodName)
            .isNull();
    }

    private Method methodOf(Class<?> type, String methodName, Class<?>... parameterTypes) {
        try {
            return type.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(type.getSimpleName() + " 缺少方法 " + methodName, e);
        }
    }

}
