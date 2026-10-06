package com.carlos.workflow.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.workflow.api.pojo.param.ApiWorkflowDefinitionPageParam;
import com.carlos.workflow.dto.WorkflowDefinitionDTO;
import com.carlos.workflow.manager.impl.WorkflowDefinitionManagerImpl;
import com.carlos.workflow.poc.WorkflowContextStartupTest;
import org.dromara.warm.flow.orm.mapper.FlowDefinitionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * 流程定义 Manager 的真实 SQL 测试：在 <b>既有装配测试所用的那个 Spring 上下文</b> 里用真实的
 * {@code FlowDefinitionMapper} 跑分页、过滤、排序与按主键查询。
 * </p>
 * <p>
 * <b>为什么必须起上下文而不是 mock：</b>本模块的流程定义表属于 Warm-Flow 引擎
 * （{@code org.dromara.warm.flow.orm.mapper.FlowDefinitionMapper}），它是否能被注入取决于
 * 引擎自带的 {@code @MapperScan("org.dromara.warm.flow.orm.mapper")}
 * （在 {@code FlowAutoConfig} 上，经 {@code AutoConfiguration.imports} 生效，条件是
 * {@code warm-flow.enabled} 缺省为 true）是否真的在本模块生效。只读代码看不出结论，
 * 这里用「Bean 真的在容器里 + 分页真的返回了数据」两个断言把它钉死。
 * </p>
 * <p>
 * <b>为什么复用 {@link WorkflowContextStartupTest.TestApplication} 且属性必须逐字一致：</b>
 * Warm-Flow 的 {@code SpringUtil} 用静态字段缓存第一个 {@code ApplicationContext}
 * （{@code setApplicationContext} 只在字段为 null 时赋值），而该上下文里的 {@code initFlow}
 * Bean 的运行期类型正是 {@code WarmFlowProperties}。因此同一个 JVM 里出现<b>第二个</b>带
 * {@code FlowAutoConfig} 的 Spring 上下文时，后者的 {@code initFlow} 会去第一个上下文里
 * 解析 {@code WarmFlowProperties}，撞上「找到 2 个候选」而启动失败。属性完全一致时 Spring
 * Test 会命中上下文缓存只建一个上下文；一旦这里改了属性，先坏的会是
 * {@code WorkflowContextStartupTest}，不会静默。
 * </p>
 * <p>
 * 表结构由本测试用 JDBC 建出——它是测试夹具，对应
 * {@code carlos-workflow/docs/sql/workflow_postgresql.sql} 的 flow_definition 段
 * （官方脚本是 PostgreSQL 专用语法，H2 无法直接执行）。业务代码里没有任何 SQL。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
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
class WorkflowDefinitionManagerTest {

    /**
     * 与 docs/sql/workflow_postgresql.sql 的 flow_definition 段等价，仅按 H2 语法改写
     */
    private static final String FLOW_DEFINITION_DDL = """
        CREATE TABLE flow_definition (
            id              BIGINT       NOT NULL,
            flow_code       VARCHAR(40)  NOT NULL,
            flow_name       VARCHAR(100) NOT NULL,
            model_value     VARCHAR(40)  NOT NULL DEFAULT 'CLASSICS',
            category        VARCHAR(100) NULL,
            version         VARCHAR(20)  NOT NULL,
            is_publish      SMALLINT     NOT NULL DEFAULT 0,
            form_custom     CHAR(1)      NULL DEFAULT 'N',
            form_path       VARCHAR(100) NULL,
            activity_status SMALLINT     NOT NULL DEFAULT 1,
            listener_type   VARCHAR(100) NULL,
            listener_path   VARCHAR(400) NULL,
            ext             VARCHAR(500) NULL,
            create_time     TIMESTAMP    NULL,
            create_by       VARCHAR(64)  NULL,
            update_time     TIMESTAMP    NULL,
            update_by       VARCHAR(64)  NULL,
            del_flag        CHAR(1)      NULL DEFAULT '0',
            tenant_id       VARCHAR(40)  NULL,
            CONSTRAINT flow_definition_pkey PRIMARY KEY (id)
        )
        """;

    private static final String INSERT_DEFINITION = """
        INSERT INTO flow_definition
            (id, flow_code, flow_name, model_value, version, category, is_publish, form_custom,
             activity_status, create_time, create_by, update_time, update_by, del_flag, tenant_id)
        VALUES (?, ?, ?, 'CLASSICS', ?, ?, ?, 'N', 1, ?, 'tester', ?, 'tester', ?, '1')
        """;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private WorkflowDefinitionManager definitionManager;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void resetDefinitionTable() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS flow_definition");
            statement.execute(FLOW_DEFINITION_DDL);
        }
        // create_time 递增，用于断言默认排序；id=5 是逻辑删除行，不应出现在任何查询结果里
        insert(1L, "leave", "请假流程", "HR", 1, "2026-01-01 10:00:00", "0");
        insert(2L, "leave", "请假流程(旧版)", "HR", 0, "2026-01-02 10:00:00", "0");
        insert(3L, "travel", "出差流程", "ADMIN", 9, "2026-01-03 10:00:00", "0");
        insert(4L, "expense", "报销流程", "HR", 1, "2026-01-04 10:00:00", "0");
        insert(5L, "docs", "已删除的流程", "HR", 1, "2026-01-05 10:00:00", "1");
    }

    @Test
    void definitionMapperComesFromTheWarmFlowMapperScanAndIsInjected() {
        assertThat(applicationContext.getBeanNamesForType(FlowDefinitionMapper.class))
            .as("Warm-Flow 的 @MapperScan(\"org.dromara.warm.flow.orm.mapper\") 必须真的覆盖到本模块，"
                + "否则 FlowDefinitionMapper 根本不在容器里，Manager 无从注入")
            .contains("flowDefinitionMapper");

        assertThat(((WorkflowDefinitionManagerImpl) definitionManager).getBaseMapper())
            .as("Manager 继承 BaseServiceImpl<FlowDefinitionMapper, FlowDefinition>，baseMapper 必须是容器里那个 Mapper"
                + "（泛型擦除后按类型注入唯一匹配）")
            .isSameAs(applicationContext.getBean(FlowDefinitionMapper.class));
    }

    @Test
    void pageWithoutFilterReturnsEveryLiveDefinitionNewestFirst() {
        Paging<WorkflowDefinitionDTO> paging = definitionManager.getPage(pageParam(1, 10));

        assertThat(paging.getTotal()).isEqualTo(4);
        assertThat(paging.getPages()).isEqualTo(1);
        assertThat(paging.getRecords())
            .as("默认按 create_time 倒序，且 @TableLogic 必须把 del_flag=1 的行挡在结果外")
            .extracting(WorkflowDefinitionDTO::getDefinitionId)
            .containsExactly(4L, 3L, 2L, 1L);
        assertThat(paging.getRecords().get(0).getFlowName()).isEqualTo("报销流程");
        assertThat(paging.getRecords().get(0).getIsPublish()).isEqualTo(1);
        assertThat(paging.getRecords().get(0).getCreateTime()).isNotNull();
    }

    @Test
    void pageAppliesLimitAndOffset() {
        Paging<WorkflowDefinitionDTO> paging = definitionManager.getPage(pageParam(2, 2));

        assertThat(paging.getTotal()).isEqualTo(4);
        assertThat(paging.getPages()).isEqualTo(2);
        assertThat(paging.getCurrent()).isEqualTo(2);
        assertThat(paging.getSize()).isEqualTo(2);
        assertThat(paging.getRecords())
            .extracting(WorkflowDefinitionDTO::getDefinitionId)
            .containsExactly(2L, 1L);
    }

    @Test
    void pageFiltersFlowNameByLike() {
        ApiWorkflowDefinitionPageParam param = pageParam(1, 10);
        param.setFlowName("请假");

        assertThat(definitionManager.getPage(param).getRecords())
            .extracting(WorkflowDefinitionDTO::getDefinitionId)
            .containsExactly(2L, 1L);

        param.setFlowName("流程");
        assertThat(definitionManager.getPage(param).getRecords())
            .as("模糊匹配，'流程' 命中全部 4 条未删除的定义")
            .hasSize(4);
    }

    @Test
    void pageFiltersFlowCodeAndCategoryAndIsPublishByEquality() {
        ApiWorkflowDefinitionPageParam param = pageParam(1, 10);
        param.setFlowCode("leave");
        param.setCategory("HR");
        assertThat(definitionManager.getPage(param).getRecords())
            .as("flowCode 等值匹配（不是 like），'leave' 只命中两条")
            .extracting(WorkflowDefinitionDTO::getDefinitionId)
            .containsExactly(2L, 1L);

        param.setFlowCode(null);
        param.setCategory("HR");
        assertThat(definitionManager.getPage(param).getRecords())
            .extracting(WorkflowDefinitionDTO::getDefinitionId)
            .containsExactly(4L, 2L, 1L);

        param.setCategory(null);
        param.setIsPublish(1);
        assertThat(definitionManager.getPage(param).getRecords())
            .as("isPublish=1 只命中已发布的定义")
            .extracting(WorkflowDefinitionDTO::getDefinitionId)
            .containsExactly(4L, 1L);

        param.setFlowCode("leave");
        assertThat(definitionManager.getPage(param).getRecords())
            .as("多个条件同时给出时按 and 组合")
            .extracting(WorkflowDefinitionDTO::getDefinitionId)
            .containsExactly(1L);

        param.setFlowCode("leave");
        param.setIsPublish(9);
        assertThat(definitionManager.getPage(param).getRecords())
            .as("已失效（9）与未发布（0）是不同的过滤口径")
            .isEmpty();
    }

    @Test
    void getDtoByIdReturnsTheRowAndNullWhenMissingOrSoftDeleted() {
        assertThat(definitionManager.getDtoById(3L))
            .satisfies(dto -> {
                assertThat(dto.getDefinitionId()).isEqualTo(3L);
                assertThat(dto.getFlowCode()).isEqualTo("travel");
                assertThat(dto.getFlowName()).isEqualTo("出差流程");
                assertThat(dto.getCategory()).isEqualTo("ADMIN");
                assertThat(dto.getIsPublish()).isEqualTo(9);
                assertThat(dto.getVersion()).isEqualTo("1");
            });

        assertThat(definitionManager.getDtoById(404L))
            .as("查不到定义时返回 null，由 Service 层翻译成 20701")
            .isNull();
        assertThat(definitionManager.getDtoById(5L))
            .as("逻辑删除的行等同于不存在")
            .isNull();
        assertThat(definitionManager.getDtoById(null)).isNull();
    }

    @Test
    void listByFlowCodeReturnsEveryVersionOfThatCode() {
        List<WorkflowDefinitionDTO> definitions = definitionManager.listByFlowCode("leave");

        assertThat(definitions)
            .extracting(WorkflowDefinitionDTO::getDefinitionId)
            .containsExactlyInAnyOrder(1L, 2L);
        assertThat(definitionManager.listByFlowCode("not-exists")).isEmpty();
        assertThat(definitionManager.listByFlowCode("  ")).isEmpty();
    }

    private static ApiWorkflowDefinitionPageParam pageParam(int current, int size) {
        ApiWorkflowDefinitionPageParam param = new ApiWorkflowDefinitionPageParam();
        param.setCurrent(current);
        param.setSize(size);
        return param;
    }

    private void insert(long id, String flowCode, String flowName, String category, int isPublish,
                        String createTime, String delFlag) throws SQLException {
        Timestamp timestamp = Timestamp.valueOf(createTime);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_DEFINITION)) {
            statement.setLong(1, id);
            statement.setString(2, flowCode);
            statement.setString(3, flowName);
            statement.setString(4, "1");
            statement.setString(5, category);
            statement.setInt(6, isPublish);
            statement.setTimestamp(7, timestamp);
            statement.setTimestamp(8, timestamp);
            statement.setString(9, delFlag);
            statement.executeUpdate();
        }
    }

}
