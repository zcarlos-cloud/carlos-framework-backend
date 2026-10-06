package com.carlos.workflow.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.workflow.api.pojo.param.ApiWorkflowInstancePageParam;
import com.carlos.workflow.dto.WorkflowInstanceDTO;
import com.carlos.workflow.poc.WorkflowContextStartupTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
 * 「我发起的」流程实例分页的真实 SQL 测试：在既有装配测试所用的那个 Spring 上下文里，用真实的
 * {@code flow_instance} / {@code flow_definition} 表跑分页、过滤与排序。
 * </p>
 * <p>
 * <b>为什么必须起上下文：</b>本 Manager 继承 {@code BaseServiceImpl}、依赖引擎
 * {@code @MapperScan} 注册的 {@code FlowInstanceMapper} / {@code FlowDefinitionMapper}，
 * 分页又依赖框架的 {@code PaginationInnerInterceptor}；「按流程名称先查定义 id、再用
 * {@code definition_id IN (...)} 过滤实例」这种两段式写法能否真的执行，只能真跑一遍才知道。
 * </p>
 * <p>
 * <b>为什么复用 {@link WorkflowContextStartupTest.TestApplication} 且属性逐字一致：</b>
 * 同一个 JVM 里出现第二个带 {@code FlowAutoConfig} 的 Spring 上下文会因 Warm-Flow 的
 * {@code SpringUtil} 静态缓存而启动失败（详见 {@code WorkflowDefinitionManagerTest} 的说明）。
 * </p>
 * <p>
 * 表结构由本测试用 JDBC 建出——它是测试夹具，对应
 * {@code carlos-workflow/docs/sql/workflow_postgresql.sql}（官方脚本是 PostgreSQL 专用语法，
 * H2 无法直接执行）。业务代码里没有任何 SQL。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@SpringBootTest(
    classes = WorkflowContextStartupTest.TestApplication.class,
    properties = {
        // 本模块已接入 carlos-spring-boot-starter-mybatis，数据源由框架的动态数据源栈接管
        // （Druid + dynamic-datasource），H2 必须配在 spring.datasource.dynamic.datasource.master.* 下
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
class WorkflowInstanceManagerTest {

    /**
     * 发起人：本测试的「当前用户」
     */
    private static final String CURRENT_USER = "1001";

    /**
     * 另一个发起人：他的实例不该出现在「我发起的」里
     */
    private static final String OTHER_USER = "2002";

    /**
     * 与 docs/sql/workflow_postgresql.sql 等价，仅按 H2 语法改写
     */
    private static final List<String> DDL = List.of("""
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
            CONSTRAINT flow_definition_pkey PRIMARY KEY (id))
        """, """
        CREATE TABLE flow_instance (
            id              BIGINT       NOT NULL,
            definition_id   BIGINT       NOT NULL,
            business_id     VARCHAR(40)  NOT NULL,
            node_type       SMALLINT     NOT NULL,
            node_code       VARCHAR(40)  NOT NULL,
            node_name       VARCHAR(100) NULL,
            variable        CLOB         NULL,
            flow_status     VARCHAR(20)  NOT NULL,
            activity_status SMALLINT     NOT NULL DEFAULT 1,
            def_json        CLOB         NULL,
            ext             VARCHAR(500) NULL,
            create_time     TIMESTAMP    NULL,
            create_by       VARCHAR(64)  NULL,
            update_time     TIMESTAMP    NULL,
            update_by       VARCHAR(64)  NULL,
            del_flag        CHAR(1)      NULL DEFAULT '0',
            tenant_id       VARCHAR(40)  NULL,
            CONSTRAINT flow_instance_pkey PRIMARY KEY (id))
        """);

    private static final String INSERT_DEFINITION = """
        INSERT INTO flow_definition (id, flow_code, flow_name, model_value, version, is_publish, activity_status,
            del_flag, tenant_id) VALUES (?, ?, ?, 'CLASSICS', '1', 1, 1, '0', '1')
        """;

    private static final String INSERT_INSTANCE = """
        INSERT INTO flow_instance (id, definition_id, business_id, node_type, node_code, node_name, flow_status,
            activity_status, create_time, create_by, update_time, update_by, del_flag, tenant_id)
        VALUES (?, ?, ?, 1, 'nodeB', '经理审批', ?, 1, ?, ?, ?, ?, '0', '1')
        """;

    @Autowired
    private WorkflowInstanceManager instanceManager;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void resetTables() throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            // flow_definition 也被别的测试类用着：各自在 @BeforeEach 重建，互不依赖执行顺序
            for (String table : List.of("flow_instance", "flow_definition")) {
                statement.execute("DROP TABLE IF EXISTS " + table);
            }
            for (String ddl : DDL) {
                statement.execute(ddl);
            }
        }
        insertDefinition(7L, "leave", "请假流程");
        insertDefinition(8L, "travel", "出差流程");

        // 1001 发起的三条，create_time 递增；2002 发起的一条不该出现在「我发起的」里
        insertInstance(303L, 7L, "BIZ-303", "1", CURRENT_USER, "2026-01-01 10:00:00");
        insertInstance(304L, 8L, "BIZ-304", "2", CURRENT_USER, "2026-01-02 10:00:00");
        insertInstance(305L, 8L, "BIZ-305", "1", CURRENT_USER, "2026-01-03 10:00:00");
        insertInstance(306L, 7L, "BIZ-306", "1", OTHER_USER, "2026-01-04 10:00:00");
    }

    @Test
    void pageReturnsOnlyInstancesStartedByTheCurrentUserNewestFirst() {
        Paging<WorkflowInstanceDTO> paging = instanceManager.getMyPage(pageParam(1, 10), CURRENT_USER);

        assertThat(paging.getTotal()).isEqualTo(3);
        assertThat(paging.getRecords())
            .as("按 create_time 倒序，且 2002 发起的 306 必须被挡在结果外")
            .extracting(WorkflowInstanceDTO::getInstanceId)
            .containsExactly(305L, 304L, 303L);
        assertThat(paging.getRecords()).extracting(WorkflowInstanceDTO::getCreateBy)
            .containsOnly(CURRENT_USER);
    }

    @Test
    void pageAppliesLimitAndOffset() {
        Paging<WorkflowInstanceDTO> paging = instanceManager.getMyPage(pageParam(2, 2), CURRENT_USER);

        assertThat(paging.getTotal()).isEqualTo(3);
        assertThat(paging.getPages()).isEqualTo(2);
        assertThat(paging.getCurrent()).isEqualTo(2);
        assertThat(paging.getSize()).isEqualTo(2);
        assertThat(paging.getRecords())
            .extracting(WorkflowInstanceDTO::getInstanceId)
            .containsExactly(303L);
    }

    @Test
    void pageFillsTheFlowNameFromTheDefinitionTable() {
        List<WorkflowInstanceDTO> records =
            instanceManager.getMyPage(pageParam(1, 10), CURRENT_USER).getRecords();

        assertThat(records).extracting(WorkflowInstanceDTO::getFlowName)
            .as("flow_instance 没有 flow_name 列，名称必须由定义表批量补齐")
            .containsExactly("出差流程", "出差流程", "请假流程");
        assertThat(records).extracting(WorkflowInstanceDTO::getBusinessId)
            .containsExactly("BIZ-305", "BIZ-304", "BIZ-303");
    }

    @Test
    void pageFiltersFlowNameByLikeThroughTheDefinitionTable() {
        ApiWorkflowInstancePageParam param = pageParam(1, 10);
        param.setFlowName("请假");

        assertThat(instanceManager.getMyPage(param, CURRENT_USER).getRecords())
            .extracting(WorkflowInstanceDTO::getInstanceId)
            .containsExactly(303L);

        param.setFlowName("流程");
        assertThat(instanceManager.getMyPage(param, CURRENT_USER).getRecords())
            .as("模糊匹配，'流程' 命中全部三条")
            .extracting(WorkflowInstanceDTO::getInstanceId)
            .containsExactly(305L, 304L, 303L);
    }

    @Test
    void pageReturnsNothingWhenNoDefinitionMatchesTheFlowName() {
        ApiWorkflowInstancePageParam param = pageParam(1, 10);
        param.setFlowName("报销");

        Paging<WorkflowInstanceDTO> paging = instanceManager.getMyPage(param, CURRENT_USER);

        assertThat(paging.getTotal()).isZero();
        assertThat(paging.getRecords()).isEmpty();
    }

    @Test
    void pageFiltersFlowStatusAndBusinessIdByEquality() {
        ApiWorkflowInstancePageParam byFlowStatus = pageParam(1, 10);
        byFlowStatus.setFlowStatus("1");
        assertThat(instanceManager.getMyPage(byFlowStatus, CURRENT_USER).getRecords())
            .extracting(WorkflowInstanceDTO::getInstanceId)
            .containsExactly(305L, 303L);

        ApiWorkflowInstancePageParam byBusinessId = pageParam(1, 10);
        byBusinessId.setBusinessId("BIZ-304");
        assertThat(instanceManager.getMyPage(byBusinessId, CURRENT_USER).getRecords())
            .extracting(WorkflowInstanceDTO::getInstanceId)
            .containsExactly(304L);
    }

    @Test
    void pageCombinesEveryFilterWithAnd() {
        ApiWorkflowInstancePageParam param = pageParam(1, 10);
        param.setFlowName("出差");
        param.setFlowStatus("1");
        param.setBusinessId("BIZ-305");

        assertThat(instanceManager.getMyPage(param, CURRENT_USER).getRecords())
            .extracting(WorkflowInstanceDTO::getInstanceId)
            .containsExactly(305L);

        param.setFlowStatus("2");
        assertThat(instanceManager.getMyPage(param, CURRENT_USER).getRecords())
            .as("多个条件同时给出时按 and 组合")
            .isEmpty();
    }

    @Test
    void pageReturnsNothingWhenTheCurrentUserIsUnknown() {
        assertThat(instanceManager.getMyPage(pageParam(1, 10), null).getRecords()).isEmpty();
        assertThat(instanceManager.getMyPage(pageParam(1, 10), "  ").getRecords()).isEmpty();
    }

    @Test
    void pageAcceptsANullParamAndFallsBackToTheDefaultPage() {
        Paging<WorkflowInstanceDTO> paging = instanceManager.getMyPage(null, CURRENT_USER);

        assertThat(paging.getTotal()).isEqualTo(3);
        assertThat(paging.getCurrent()).isEqualTo(1);
    }

    private static ApiWorkflowInstancePageParam pageParam(int current, int size) {
        ApiWorkflowInstancePageParam param = new ApiWorkflowInstancePageParam();
        param.setCurrent(current);
        param.setSize(size);
        return param;
    }

    private void insertDefinition(long id, String flowCode, String flowName) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_DEFINITION)) {
            statement.setLong(1, id);
            statement.setString(2, flowCode);
            statement.setString(3, flowName);
            statement.executeUpdate();
        }
    }

    private void insertInstance(long id, long definitionId, String businessId, String flowStatus, String createBy,
                                String createTime) throws SQLException {
        Timestamp timestamp = Timestamp.valueOf(createTime);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_INSTANCE)) {
            statement.setLong(1, id);
            statement.setLong(2, definitionId);
            statement.setString(3, businessId);
            statement.setString(4, flowStatus);
            statement.setTimestamp(5, timestamp);
            statement.setString(6, createBy);
            statement.setTimestamp(7, timestamp);
            statement.setString(8, createBy);
            statement.executeUpdate();
        }
    }

}
