package com.carlos.workflow.poc;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.carlos.audit.api.log.AuditMqLogStorage;
import com.carlos.core.response.Result;
import com.carlos.log.storage.LogStorage;
import com.carlos.mq.core.MqTemplate;
import com.carlos.mq.core.NoOpMqTemplate;
import com.carlos.mq.spi.MqTransportRegistry;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.position.api.ApiOrgUserPosition;
import com.carlos.redis.stream.RedisStreamTemplate;
import com.carlos.redis.util.RedisUtil;
import com.carlos.workflow.apiimpl.ApiWorkflowDefinitionImpl;
import com.carlos.workflow.apiimpl.ApiWorkflowInstanceImpl;
import com.carlos.workflow.apiimpl.ApiWorkflowTaskImpl;
import com.carlos.workflow.config.WorkflowAutoConfiguration;
import com.carlos.workflow.controller.WorkflowDefinitionController;
import com.carlos.workflow.controller.WorkflowInstanceController;
import com.carlos.workflow.controller.WorkflowTaskController;
import com.carlos.workflow.handler.OrgPermissionHandler;
import com.carlos.workflow.handler.PermissionHandlerStub;
import com.carlos.workflow.handler.WorkflowAssigneeFailFastListener;
import com.carlos.workflow.handler.WorkflowAssignees;
import com.carlos.workflow.service.WorkflowDefinitionService;
import com.carlos.workflow.service.WorkflowInstanceService;
import com.carlos.workflow.service.WorkflowTaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.dromara.warm.flow.core.listener.GlobalListener;
import org.dromara.warm.flow.core.service.ChartService;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.NodeService;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.core.service.UserService;
import org.dromara.warm.flow.core.utils.ExpressionUtil;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import javax.sql.DataSource;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 工作流上下文装配测试：使用内存 H2 启动完整上下文，校验 Warm-Flow 引擎 Bean 全部就位，
 * 且未发生 Bean 名冲突（Spring Boot 默认 allow-bean-definition-overriding=false）。
 * </p>
 * <p>
 * 与旧位置的差别：{@code -bus} 模块自带 Servlet API，因此使用 MOCK Web 环境，
 * 两套 REST 控制器（{@code apiimpl} 包的服务间端点与 {@code controller} 包的前端管理端点）
 * 随组件扫描真实装配——旧模块里只能断言它们「不存在」。
 * </p>
 * <p>
 * 办理人：生产装配会注册 {@link OrgPermissionHandler}（见 {@link WorkflowAutoConfiguration}），
 * 但本测试上下文没有 org 的 Feign 客户端，且测试配置自带了 {@link PermissionHandlerStub}，
 * 两道条件都让生产实现退让。本测试因此只断言「宿主注册了处理器时引擎能取到」；
 * 生产实现的装配由 {@code WorkflowAssigneeWiringTest} 覆盖。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Slf4j
@SpringBootTest(
    classes = WorkflowContextStartupTest.TestApplication.class,
    properties = {
        // 本模块已接入 carlos-spring-boot-starter-mybatis，数据源由框架的动态数据源栈接管
        // （Druid + dynamic-datasource），H2 必须配在 spring.datasource.dynamic.datasource.master.* 下：
        // 只写 spring.datasource.url 会被静默忽略（不报错，只是没有任何可用数据源）。
        "spring.datasource.dynamic.primary=master",
        "spring.datasource.dynamic.datasource.master.url=jdbc:h2:mem:carlos_workflow_poc;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.dynamic.datasource.master.driver-class-name=org.h2.Driver",
        "spring.datasource.dynamic.datasource.master.username=sa",
        "spring.datasource.dynamic.datasource.master.password=",
        // carlos-spring-boot-starter-snowflake（随 starter-mybatis 传入）要求 tag 非空：
        // 取 carlos.snowflake.tag，缺省回落到 spring.application.name；两者都空会直接启动失败。
        // boot 的 application.yml 也是靠 spring.application.name 满足这一条。
        "spring.application.name=carlos-workflow",
        // 故意配成 dm：框架默认是 POSTGRE_SQL，只有 carlos.mybatis.db-type 真的被读到，方言才会是 DM。
        "carlos.mybatis.db-type=dm",
        "carlos.workflow.enabled=true",
        "warm-flow.banner=false",
        "warm-flow.permission-handler-path="
    }
)
public class WorkflowContextStartupTest {

    /**
     * 测试作用域的桩办理人标识，仅本测试使用
     */
    private static final String TEST_HANDLER_ID = "carlos-workflow-test";

    /**
     * Warm-Flow BeanConfig 的 @Bean 方法名与 @Component 显式名，是最可能撞名的固定短名
     */
    private static final List<String> WARM_FLOW_FIXED_BEAN_NAMES = List.of(
        "definitionDao", "definitionService",
        "nodeDao", "nodeService",
        "skipDao", "skipService",
        "instanceDao", "instanceService",
        "taskDao", "taskService",
        "hisTaskDao", "hisTaskService",
        "flowUserDao", "flowUserService",
        "formDao", "flowFormService",
        "chartService", "initFlow",
        "warmFlowSpringUtil");

    /**
     * 由 Spring 或 MapperScan 推导出的 Bean 名：@Import/@Configuration 注册的为全限定名，MapperScan 注册的为接口简单名
     */
    private static final List<String> WARM_FLOW_DERIVED_BEAN_NAMES = List.of(
        "org.dromara.warm.plugin.modes.sb.helper.SpelHelper",
        "org.dromara.warm.flow.spring.boot.config.FlowAutoConfig",
        "org.dromara.warm.flow.ui.config.WarmFlowUiConfig",
        "org.dromara.warm.flow.ui.controller.WarmFlowController",
        "org.dromara.warm.flow.ui.controller.WarmFlowUiController",
        "warm-flow-org.dromara.warm.plugin.modes.sb.config.WarmFlowProperties",
        "warmMapper",
        "flowDefinitionMapper", "flowNodeMapper", "flowSkipMapper", "flowInstanceMapper",
        "flowTaskMapper", "flowHisTaskMapper", "flowUserMapper", "flowFormMapper");

    /**
     * 测试应用：补一个测试作用域的 PermissionHandler 与 org 岗位契约桩。
     * <p>
     * 生产装配会注册 {@link OrgPermissionHandler}，但本上下文没有 org Feign 客户端，
     * 该 Bean 的两个条件（{@code @ConditionalOnMissingBean} / {@code @ConditionalOnBean}）都不成立；
     * 这里再显式提供桩实现，用于断言「宿主注册了处理器时引擎能取到」。
     * </p>
     */
    @SpringBootApplication(scanBasePackages = "com.carlos.workflow")
    public static class TestApplication {

        @Bean
        PermissionHandler permissionHandlerStub() {
            return new PermissionHandlerStub(TEST_HANDLER_ID);
        }

        /**
         * SpEL 助手 {@code workflowAssignees} 的装配条件，为它补一个 org 契约桩
         */
        @Bean
        ApiOrgUserPosition apiOrgUserPosition() {
            return mock(ApiOrgUserPosition.class);
        }
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ApiOrgUserPosition apiOrgUserPosition;

    @Test
    void contextStartsWithEveryWarmFlowFixedBeanName() {
        assertThat(applicationContext.getBeanDefinitionNames())
            .as("Warm-Flow 使用固定短 Bean 名，同名 Bean 会导致 BeanDefinitionOverrideException")
            .containsAll(WARM_FLOW_FIXED_BEAN_NAMES);
    }

    @Test
    void warmFlowServicesResolveFromContext() {
        assertThat(applicationContext.getBean("definitionService")).isInstanceOf(DefService.class);
        assertThat(applicationContext.getBean("instanceService")).isInstanceOf(InsService.class);
        assertThat(applicationContext.getBean("taskService")).isInstanceOf(TaskService.class);
        assertThat(applicationContext.getBean("nodeService")).isInstanceOf(NodeService.class);
        assertThat(applicationContext.getBean("chartService")).isInstanceOf(ChartService.class);
        assertThat(applicationContext.getBean("flowUserService")).isInstanceOf(UserService.class);
        assertThat(applicationContext.containsBean("initFlow")).isTrue();
    }

    @Test
    void workflowApiServicesResolveFromContext() {
        assertThat(applicationContext.getBean(WorkflowTaskService.class)).isNotNull();
        assertThat(applicationContext.getBean(WorkflowInstanceService.class)).isNotNull();
        assertThat(applicationContext.getBean(WorkflowDefinitionService.class)).isNotNull();
    }

    @Test
    void workflowApiControllersAreRegisteredInServletContext() {
        assertThat(applicationContext.getBeanNamesForType(ApiWorkflowTaskImpl.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(ApiWorkflowInstanceImpl.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(ApiWorkflowDefinitionImpl.class)).isNotEmpty();
    }

    @Test
    void workflowFrontendControllersAreRegisteredInServletContext() {
        assertThat(applicationContext.getBeanNamesForType(WorkflowTaskController.class))
            .as("管理端（/workflow/task/**，前端经网关）控制器必须随组件扫描装配")
            .isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(WorkflowInstanceController.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(WorkflowDefinitionController.class)).isNotEmpty();
    }

    @Test
    void productionPermissionHandlerBacksOffForHostProvidedOne() {
        assertThat(applicationContext.getBeanNamesForType(PermissionHandler.class))
            .as("宿主提供了 PermissionHandler 时，生产装配的 OrgPermissionHandler 应因 @ConditionalOnMissingBean 退让")
            .containsExactly("permissionHandlerStub");

        PermissionHandler permissionHandler = applicationContext.getBean(PermissionHandler.class);
        assertThat(permissionHandler).isInstanceOf(PermissionHandlerStub.class);
        assertThat(permissionHandler.getHandler()).isEqualTo(TEST_HANDLER_ID);
        assertThat(permissionHandler.permissions()).isEmpty();
        assertThat(permissionHandler.convertPermissions(List.of("user:1", "role:2")))
            .containsExactly("user:1", "role:2");
    }

    @Test
    void engineResolvesPermissionHandlerFromSpringWhenProvided() {
        assertThat(FlowEngine.permissionHandler())
            .as("warm-flow.permission-handler-path 留空时，引擎按类型从 Spring 容器取 PermissionHandler")
            .isInstanceOf(PermissionHandlerStub.class);
    }

    @Test
    void workflowAutoConfigurationRegistersRealPermissionHandler() {
        assertThat(beanReturnTypes())
            .as("生产装配必须给出真实的办理人解析实现：返回空权限列表的兜底实现会让接口看似可用、真实审批全部失败")
            .contains(OrgPermissionHandler.class)
            .doesNotContain(PermissionHandlerStub.class);
    }

    @Test
    void workflowAutoConfigurationRegistersSpelAssigneesUnderSpecBeanName() {
        assertThat(Arrays.stream(WorkflowAutoConfiguration.class.getDeclaredMethods())
            .filter(method -> AnnotatedElementUtils.hasAnnotation(method, Bean.class))
            .filter(method -> method.getReturnType() == WorkflowAssignees.class)
            .map(Method::getName))
            .as("SpEL 的 @workflowAssignees 按 Bean 名解析，Bean 名即 @Bean 方法名，改名会让已发布流程定义失效")
            .containsExactly("workflowAssignees");
    }

    @Test
    void workflowAutoConfigurationRegistersFailFastGlobalListenerAndEnginePicksItUp() {
        assertThat(applicationContext.getBean(GlobalListener.class))
            .as("办理人 fail-fast 拦截器必须注册成 Bean：宿主提供自己的 GlobalListener 时退让，缺省则由本模块装配")
            .isInstanceOf(WorkflowAssigneeFailFastListener.class);

        assertThat(FlowEngine.globalListener())
            .as("warm-flow.global-listener-path 留空时引擎按类型从 Spring 取 GlobalListener；"
                + "取不到就等于拦截器形同不存在，节点配错办理人又会退回「无人鉴权」")
            .isInstanceOf(WorkflowAssigneeFailFastListener.class);
    }

    @Test
    void engineSpelStrategyResolvesWorkflowAssigneesByBeanName() {
        List<String> resolved = ExpressionUtil.evalVariable("#{@workflowAssignees.upLeader(#applicantId)}",
            Map.of("applicantId", 1001L));

        assertThat(resolved)
            .as("引擎的 SpEL 策略（#{...} 模板 + BeanFactoryResolver）能按 Bean 名解析到助手 Bean；"
                + "本上下文的 org 契约是 mock 桩（未配置返回值），upLeader 因此解析不到人并返回空列表，"
                + "引擎随之回退为「原样保留该表达式字符串」——办理人成为一个谁也匹配不上的字面量，该节点无人可办")
            .containsExactly("#{@workflowAssignees.upLeader(#applicantId)}");
    }

    @Test
    void engineSpelStrategyResolvesUpLeaderToUserIdWhenOrgAnswers() {
        OrgUserAO superior = new OrgUserAO();
        superior.setId(2002L);
        superior.setUserStatus(1);
        when(apiOrgUserPosition.getSuperiorByUserId(3003L)).thenReturn(Result.success(superior));

        List<String> resolved = ExpressionUtil.evalVariable("#{@workflowAssignees.upLeader(#applicantId)}",
            Map.of("applicantId", 3003L));

        assertThat(resolved)
            .as("org 能给出上级时，SpEL 表达式的求值结果就是该上级的用户 id（与节点侧的用户 id 词汇一致）")
            .containsExactly("2002");
    }

    @Test
    void enginePassesLiteralVocabularyThroughUntouched() {
        assertThat(ExpressionUtil.evalVariable("role:5", Map.of()))
            .as("无 #/$ 前缀的字面量原样透传，这正是规范把词汇表定成无前缀形式的前提")
            .containsExactly("role:5");
    }

    @Test
    void dataSourceIsFrameworkDynamicRoutingDataSourceBackedByConfiguredUrl() throws SQLException {
        DataSource dataSource = applicationContext.getBean(DataSource.class);

        assertThat(dataSource)
            .as("接入 starter-mybatis 后数据源由框架动态数据源栈提供（Druid + dynamic-datasource）")
            .isInstanceOf(DynamicRoutingDataSource.class);

        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName())
                .as("动态数据源的 master 必须真的指向配置里的 H2；连不上说明数据源配置没生效，上下文只是个空壳")
                .isEqualTo("H2");
        }
    }

    @Test
    void paginationInterceptorIsRegisteredWithConfiguredDbType() {
        MybatisPlusInterceptor interceptor = applicationContext.getBean(MybatisPlusInterceptor.class);

        assertThat(interceptor.getInterceptors())
            .filteredOn(PaginationInnerInterceptor.class::isInstance)
            .singleElement()
            .as("Warm-Flow 自带的分页接口（UserService#page / FormService#publishedPage）依赖这个拦截器，"
                + "缺它时分页参数不会拼进 SQL，接口看似可用但返回全表")
            .satisfies(inner -> assertThat(((PaginationInnerInterceptor) inner).getDbType())
                .as("分页方言取自 carlos.mybatis.db-type（本测试故意配成 dm，框架默认是 POSTGRE_SQL），"
                    + "只有配置真的被读到才会是 DM")
                .isEqualTo(DbType.DM));
    }

    @Test
    void paginationInterceptorReachesSqlSessionFactory() {
        SqlSessionFactory sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);

        assertThat(sqlSessionFactory.getConfiguration().getInterceptors())
            .as("拦截器必须真的挂到 SqlSessionFactory 上才生效：只注册成 Bean 而不进 MyBatis 插件链，分页同样是空转")
            .anyMatch(MybatisPlusInterceptor.class::isInstance);
    }

    @Test
    void dmAndPostgresJdbcDriversAreOnTheClasspath() {
        assertThatCode(() -> Class.forName("dm.jdbc.driver.DmDriver"))
            .as("达梦（DM）驱动随 carlos-spring-boot-starter-mybatis 传递引入（runtime 作用域），信创环境直接可用")
            .doesNotThrowAnyException();
        assertThatCode(() -> Class.forName("org.postgresql.Driver"))
            .doesNotThrowAnyException();
    }

    /**
     * <p>
     * <b>本方法在 2026-09-26 被反转，反转本身是有意的，此处记录原委。</b>
     * </p>
     * <p>
     * 它此前断言的是「本模块没有任何 Redis/Redisson Bean，且
     * {@code RedisUtil}/{@code RedissonClient} 根本不在类路径上」（彼时成立：
     * starter-mybatis -> starter-snowflake -> starter-redis 是 provided 作用域、不传递）。
     * 接入操作日志后 pom 新增了 {@code carlos-spring-boot-starter-mq} 与
     * {@code carlos-spring-boot-starter-security}，两者都<b>硬依赖</b>
     * {@code carlos-spring-boot-starter-redis}，Redis / Redisson 由此成为本模块的
     * <b>生产硬依赖</b>（取舍见 pom.xml 中对应的注释块）。旧断言于是变成了一条假命题 ——
     * 继续留着它，只会让测试去验证「代码不打算做到的事」。
     * </p>
     * <p>
     * 新的不变式分治两侧，两条都被机器验证：
     * </p>
     * <ol>
     *   <li><b>生产侧</b>：Redis / Redisson 必须留在类路径上。这不是可选项 ——
     *       MQ 侧缺 Redis 类路径时不是静默降级而是<b>启动失败</b>
     *       （{@code MQ_TRANSPORT_NOT_FOUND}），审计链路随之整体不可用。</li>
     *   <li><b>测试侧</b>：不允许出现任何「要求此刻就有一个活的 Redis」的东西。
     *       具体是：不做启动期即建连的 Redisson 装配（{@code RedissonClient} 不在容器里）、
     *       MQ 不选定任何传输。其余 Redis 后端 Bean（连接工厂 / {@code RedisUtil} /
     *       Stream 原语）照常装配，它们只持有连接工厂、不建立连接 ——
     *       <b>上下文能起来</b>本身就是这条不变式最有力的证据。</li>
     * </ol>
     * <p>
     * 测试 profile 的实现开关在 {@code src/test/resources/application.yml}：
     * {@code spring.autoconfigure.exclude} 关掉五条「启动期就会去连 Redis」的自动配置
     * （Redisson 官方两条 + carlos 的 RedissonConfig / 延时队列 / 雪花 Redis 分配器），
     * {@code carlos.mq.type=none} 让 MQ 不选传输。
     * </p>
     */
    @Test
    void redisIsStillNotRequired() {
        assertThatCode(() -> Class.forName("com.carlos.redis.util.RedisUtil"))
            .as("生产侧不变式：Redis 工具类必须在类路径上（旧断言在这里要求的正是 ClassNotFoundException，已被有意反转）")
            .doesNotThrowAnyException();
        assertThatCode(() -> Class.forName("org.redisson.api.RedissonClient"))
            .as("生产侧不变式：Redisson 同在类路径上")
            .doesNotThrowAnyException();

        assertThat(applicationContext.getBeanNamesForType(RedissonClient.class))
            .as("测试侧不变式：RedissonClient 的工厂方法内部就建连，是唯一「没有 Redis 就起不来」的 Bean。"
                + "测试 profile 因此排除了 Redisson 的两条自动配置与自己那条 RedissonConfig；"
                + "容器里若又出现它，本用例在没有 Redis 的机器上必然失败")
            .isEmpty();

        assertThat(applicationContext.getBean(RedisConnectionFactory.class))
            .as("测试侧不变式：连接工厂来自 Boot 的 Lettuce 自动配置（构造不建连），而不是 Redisson 那个 @Primary 实现")
            .isInstanceOf(LettuceConnectionFactory.class);
        assertThat(applicationContext.getBean(RedisUtil.class))
            .as("测试侧不变式：本模块自身的 Redis 工具类 Bean 能构造出来 —— 若它需要活的 Redis 才能构造，"
                + "「测试与环境无关」这条就破了")
            .isNotNull();
        assertThat(applicationContext.getBean(RedisStreamTemplate.class))
            .as("测试侧不变式：Stream 原语层同样只持有连接工厂，构造期不建连")
            .isNotNull();

        assertThat(applicationContext.getBean(MqTemplate.class))
            .as("测试侧不变式：carlos.mq.type=none ⇒ NoOpMqTemplate，测试期间不会有任何消息真的发往 Redis")
            .isInstanceOf(NoOpMqTemplate.class);
        assertThat(applicationContext.getBean(MqTransportRegistry.class).currentOrEmpty())
            .as("测试侧不变式：没有选定任何传输 ⇒ ApplicationReadyEvent 上 MqTransportLifecycle 不会 start() 任何东西")
            .isEmpty();
        assertThat(applicationContext.getBean(LogStorage.class))
            .as("审计桥接仍然装配（@Log 链路完整，没有被悄悄换成只打日志的兜底实现），只是背后的 MqTemplate 是 NoOp")
            .isInstanceOf(AuditMqLogStorage.class);
    }

    private List<Class<?>> beanReturnTypes() {
        return Arrays.stream(WorkflowAutoConfiguration.class.getDeclaredMethods())
            .filter(method -> AnnotatedElementUtils.hasAnnotation(method, Bean.class))
            .map(Method::getReturnType)
            .toList();
    }

    @Test
    void reportWarmFlowRegisteredBeanNames() {
        List<String> beanNames = warmFlowBeanNames();

        log.info("Warm-Flow 运行时注册的 Bean 名共 {} 个", beanNames.size());
        beanNames.forEach(beanName -> log.info("  {}", beanName));

        assertThat(beanNames).containsAll(WARM_FLOW_FIXED_BEAN_NAMES);
        assertThat(beanNames).containsAll(WARM_FLOW_DERIVED_BEAN_NAMES);
    }

    private List<String> warmFlowBeanNames() {
        List<String> beanNames = new ArrayList<>();
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Class<?> beanType = beanType(beanName);
            if (beanType != null && beanType.getPackageName().startsWith("org.dromara.warm")) {
                beanNames.add(beanName);
            }
        }
        Collections.sort(beanNames);
        return beanNames;
    }

    private Class<?> beanType(String beanName) {
        try {
            return applicationContext.getType(beanName);
        } catch (BeansException e) {
            return null;
        }
    }

}
