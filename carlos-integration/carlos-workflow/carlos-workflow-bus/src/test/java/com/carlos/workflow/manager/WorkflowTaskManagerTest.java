package com.carlos.workflow.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.workflow.api.pojo.param.ApiWorkflowTaskPageParam;
import com.carlos.workflow.dto.WorkflowHisTaskDTO;
import com.carlos.workflow.dto.WorkflowTaskDTO;
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
 * 待办 / 已办分页的真实 SQL 测试：在既有装配测试所用的那个 Spring 上下文里，用真实的
 * {@code flow_task} / {@code flow_user} / {@code flow_his_task} / {@code flow_definition} /
 * {@code flow_instance} 表跑查询。
 * </p>
 * <p>
 * <b>为什么必须起上下文：</b>本 Manager 依赖引擎 {@code @MapperScan} 注册的 5 个 Mapper，
 * 分页又依赖框架的 {@code PaginationInnerInterceptor}；「按 id 集合二次查询」与
 * 「{@code MAX(id)} + {@code GROUP BY}」两种写法的实际 SQL 是否可执行，只能真跑一遍才知道。
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
        // （Druid + dynamic-datasource），H2 必须配在 spring.datasource.dynamic.datasource.master.* 下。
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
class WorkflowTaskManagerTest {

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
        """, """
        CREATE TABLE flow_task (
            id            BIGINT       NOT NULL,
            definition_id BIGINT       NOT NULL,
            instance_id   BIGINT       NOT NULL,
            node_code     VARCHAR(100) NOT NULL,
            node_name     VARCHAR(100) NULL,
            node_type     SMALLINT     NOT NULL,
            flow_status   VARCHAR(20)  NOT NULL,
            form_custom   CHAR(1)      NULL DEFAULT 'N',
            form_path     VARCHAR(100) NULL,
            create_time   TIMESTAMP    NULL,
            create_by     VARCHAR(64)  NULL,
            update_time   TIMESTAMP    NULL,
            update_by     VARCHAR(64)  NULL,
            del_flag      CHAR(1)      NULL DEFAULT '0',
            tenant_id     VARCHAR(40)  NULL,
            CONSTRAINT flow_task_pkey PRIMARY KEY (id))
        """, """
        CREATE TABLE flow_user (
            id           BIGINT       NOT NULL,
            type         CHAR(1)      NOT NULL,
            processed_by VARCHAR(80)  NULL,
            associated   BIGINT       NOT NULL,
            create_time  TIMESTAMP    NULL,
            create_by    VARCHAR(64)  NULL,
            update_time  TIMESTAMP    NULL,
            update_by    VARCHAR(64)  NULL,
            del_flag     CHAR(1)      NULL DEFAULT '0',
            tenant_id    VARCHAR(40)  NULL,
            CONSTRAINT flow_user_pk PRIMARY KEY (id))
        """, """
        CREATE TABLE flow_his_task (
            id               BIGINT       NOT NULL,
            definition_id    BIGINT       NOT NULL,
            instance_id      BIGINT       NOT NULL,
            task_id          BIGINT       NOT NULL,
            node_code        VARCHAR(100) NULL,
            node_name        VARCHAR(100) NULL,
            node_type        SMALLINT     NULL,
            target_node_code VARCHAR(200) NULL,
            target_node_name VARCHAR(200) NULL,
            approver         VARCHAR(40)  NULL,
            cooperate_type   SMALLINT     NOT NULL DEFAULT 1,
            collaborator     VARCHAR(500) NULL,
            skip_type        VARCHAR(10)  NULL,
            flow_status      VARCHAR(20)  NOT NULL,
            form_custom      CHAR(1)      NULL DEFAULT 'N',
            form_path        VARCHAR(100) NULL,
            ext              CLOB         NULL,
            message          VARCHAR(500) NULL,
            variable         CLOB         NULL,
            create_time      TIMESTAMP    NULL,
            update_time      TIMESTAMP    NULL,
            del_flag         CHAR(1)      NULL DEFAULT '0',
            tenant_id        VARCHAR(40)  NULL,
            CONSTRAINT flow_his_task_pkey PRIMARY KEY (id))
        """);

    private static final String INSERT_DEFINITION = """
        INSERT INTO flow_definition (id, flow_code, flow_name, model_value, version, is_publish, activity_status,
            del_flag, tenant_id) VALUES (?, ?, ?, 'CLASSICS', '1', 1, 1, '0', '1')
        """;

    private static final String INSERT_INSTANCE = """
        INSERT INTO flow_instance (id, definition_id, business_id, node_type, node_code, node_name, flow_status,
            activity_status, del_flag, tenant_id) VALUES (?, ?, ?, 1, 'nodeB', '经理审批', '1', ?, '0', '1')
        """;

    private static final String INSERT_TASK = """
        INSERT INTO flow_task (id, definition_id, instance_id, node_code, node_name, node_type, flow_status,
            form_custom, form_path, create_time, update_time, del_flag, tenant_id)
        VALUES (?, ?, ?, 'nodeB', ?, ?, ?, 'N', NULL, ?, ?, '0', '1')
        """;

    private static final String INSERT_USER = """
        INSERT INTO flow_user (id, type, processed_by, associated, del_flag, tenant_id)
        VALUES (?, ?, ?, ?, '0', '1')
        """;

    private static final String INSERT_HIS_TASK = """
        INSERT INTO flow_his_task (id, definition_id, instance_id, task_id, node_code, node_name, node_type,
            target_node_code, approver, cooperate_type, collaborator, skip_type, flow_status, message,
            create_time, del_flag, tenant_id)
        VALUES (?, ?, ?, ?, 'nodeA', ?, 1, 'nodeB', ?, 1, ?, 'PASS', ?, ?, ?, '0', '1')
        """;

    @Autowired
    private WorkflowTaskManager taskManager;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void resetTables() throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            // flow_definition 由 WorkflowDefinitionManagerTest 也用着：两个测试类各自在 @BeforeEach
            // 重建它，互不依赖执行顺序
            for (String table : List.of("flow_his_task", "flow_user", "flow_task", "flow_instance", "flow_definition")) {
                statement.execute("DROP TABLE IF EXISTS " + table);
            }
            for (String ddl : DDL) {
                statement.execute(ddl);
            }
        }
        insertDefinition(7L, "leave", "请假流程");
        insertDefinition(8L, "travel", "出差流程");
        insertInstance(303L, 7L, "BIZ-303", 1);
        insertInstance(304L, 8L, "BIZ-304", 0);
        insertInstance(305L, 8L, "BIZ-305", 1);

        insertTask(101L, 7L, 303L, "经理审批", 1, "1", "2026-01-01 10:00:00");
        insertTask(102L, 8L, 304L, "财务审批", 1, "1", "2026-01-02 10:00:00");
        insertTask(103L, 7L, 303L, "总监审批", 1, "1", "2026-01-03 10:00:00");
        // node_type=0（开始节点）：不是待办，任何查询都不该返回
        insertTask(104L, 7L, 303L, "发起", 0, "1", "2026-01-04 10:00:00");
        insertTask(105L, 8L, 305L, "财务审批", 1, "2", "2026-01-05 10:00:00");

        // 任务 101 有三个办理人：审批人 1001 / 1002，转办人 1003 —— 同一任务命中两个办理人时必须只出现一次
        insertUser(1L, "1", "1001", 101L);
        insertUser(2L, "1", "1002", 101L);
        insertUser(3L, "2", "1003", 101L);
        insertUser(4L, "1", "1001", 102L);
        insertUser(5L, "1", "2002", 103L);
        insertUser(6L, "1", "1001", 104L);
        insertUser(7L, "1", "1001", 105L);

        // 实例 303 上 1001 办理过两次（201 发起、202 经理审批）→ 已办只保留 202
        insertHisTask(201L, 7L, 303L, "发起", "1001", null, "2", "2026-01-01 10:00:00");
        insertHisTask(202L, 7L, 303L, "经理审批", "1001", "1002,1003", "2", "2026-01-02 10:00:00");
        insertHisTask(203L, 8L, 304L, "经理审批", "1001", null, "2", "2026-01-03 10:00:00");
        insertHisTask(204L, 8L, 304L, "财务审批", "1002", null, "2", "2026-01-04 10:00:00");
        insertHisTask(205L, 8L, 305L, "经理审批", "1001", null, "9", "2026-01-05 10:00:00");
    }

    @Test
    void todoPageOnlyReturnsBetweenNodesAssignedToTheGivenHandlers() {
        Paging<WorkflowTaskDTO> paging = taskManager.getTodoPage(pageParam(1, 10), List.of("1001"));

        assertThat(paging.getTotal())
            .as("命中 1001 的任务有 101/102/104/105，其中 104 是开始节点（node_type=0），必须被挡掉")
            .isEqualTo(3);
        assertThat(paging.getRecords())
            .extracting(WorkflowTaskDTO::getTaskId)
            .as("按 create_time 倒序")
            .containsExactly(105L, 102L, 101L);
    }

    @Test
    void todoPageDeduplicatesTasksWithSeveralHandlers() {
        Paging<WorkflowTaskDTO> paging = taskManager.getTodoPage(pageParam(1, 10), List.of("1001", "1002"));

        assertThat(paging.getRecords())
            .as("任务 101 有三条 flow_user（1001/1002/1003），待办里只能出现一次")
            .extracting(WorkflowTaskDTO::getTaskId)
            .containsExactly(105L, 102L, 101L);
        assertThat(paging.getRecords()).extracting(WorkflowTaskDTO::getTaskId).doesNotHaveDuplicates();
    }

    @Test
    void todoPageReturnsNothingForHandlersWithoutTasks() {
        Paging<WorkflowTaskDTO> paging = taskManager.getTodoPage(pageParam(1, 10), List.of("9999"));

        assertThat(paging.getTotal()).isZero();
        assertThat(paging.getRecords()).isEmpty();
    }

    @Test
    void todoPageReturnsNothingWhenTheHandlerSetIsEmpty() {
        assertThat(taskManager.getTodoPage(pageParam(1, 10), List.of()).getRecords()).isEmpty();
        assertThat(taskManager.getTodoPage(pageParam(1, 10), null).getRecords()).isEmpty();
    }

    @Test
    void todoPageAppliesNodeNameLikeAndFlowStatusEqualsFilters() {
        ApiWorkflowTaskPageParam byNodeName = pageParam(1, 10);
        byNodeName.setNodeName("财务");
        assertThat(taskManager.getTodoPage(byNodeName, List.of("1001")).getRecords())
            .extracting(WorkflowTaskDTO::getTaskId)
            .containsExactly(105L, 102L);

        ApiWorkflowTaskPageParam byFlowStatus = pageParam(1, 10);
        byFlowStatus.setFlowStatus("2");
        assertThat(taskManager.getTodoPage(byFlowStatus, List.of("1001")).getRecords())
            .as("flowStatus 是等值过滤，本夹具里只有 105 的 flow_status 是 2")
            .extracting(WorkflowTaskDTO::getTaskId)
            .containsExactly(105L);

        byFlowStatus.setFlowStatus("9");
        assertThat(taskManager.getTodoPage(byFlowStatus, List.of("1001")).getRecords()).isEmpty();
    }

    @Test
    void todoPagePaginates() {
        Paging<WorkflowTaskDTO> paging = taskManager.getTodoPage(pageParam(2, 2), List.of("1001"));

        assertThat(paging.getTotal()).isEqualTo(3);
        assertThat(paging.getPages()).isEqualTo(2);
        assertThat(paging.getCurrent()).isEqualTo(2);
        assertThat(paging.getRecords()).extracting(WorkflowTaskDTO::getTaskId).containsExactly(101L);
    }

    @Test
    void todoPageEnrichesFlowNameBusinessIdActivityStatusAndHandlers() {
        Paging<WorkflowTaskDTO> paging = taskManager.getTodoPage(pageParam(1, 10), List.of("1001"));

        WorkflowTaskDTO task101 = paging.getRecords().stream()
            .filter(dto -> dto.getTaskId().equals(101L)).findFirst().orElseThrow();

        assertThat(task101.getFlowName()).isEqualTo("请假流程");
        assertThat(task101.getBusinessId()).isEqualTo("BIZ-303");
        assertThat(task101.getActivityStatus()).isEqualTo(1);
        assertThat(task101.getCreateTime()).isNotNull();
        // 本测试上下文没有 org 契约（ApiOrgUser），姓名解析退化为原始用户标识
        assertThat(task101.getApprover()).isEqualTo("1001;1002");
        assertThat(task101.getTransferredBy()).isEqualTo("1003");
        assertThat(task101.getDelegate()).isNull();

        WorkflowTaskDTO task102 = paging.getRecords().stream()
            .filter(dto -> dto.getTaskId().equals(102L)).findFirst().orElseThrow();
        assertThat(task102.getFlowName()).isEqualTo("出差流程");
        assertThat(task102.getBusinessId()).isEqualTo("BIZ-304");
        assertThat(task102.getActivityStatus()).isZero();
    }

    @Test
    void donePageKeepsOnlyTheLatestRecordPerInstance() {
        Paging<WorkflowHisTaskDTO> paging = taskManager.getDonePage(pageParam(1, 10), "1001");

        assertThat(paging.getTotal())
            .as("1001 在 3 个实例上办理过 4 条历史任务，每个实例只保留 id 最大的那条")
            .isEqualTo(3);
        assertThat(paging.getRecords())
            .extracting(WorkflowHisTaskDTO::getHisTaskId)
            .as("实例 303 只留 202（201 被同一实例的 202 顶掉），按 create_time 倒序")
            .containsExactly(205L, 203L, 202L);
    }

    @Test
    void donePageAppliesNodeNameOnTheLatestPickedRecord() {
        ApiWorkflowTaskPageParam param = pageParam(1, 10);
        param.setNodeName("经理");

        assertThat(taskManager.getDonePage(param, "1001").getRecords())
            .extracting(WorkflowHisTaskDTO::getHisTaskId)
            .containsExactly(205L, 203L, 202L);

        param.setNodeName("发起");
        assertThat(taskManager.getDonePage(param, "1001").getRecords())
            .extracting(WorkflowHisTaskDTO::getHisTaskId)
            .as("实例 303 上也办理过「发起」，但它不是该实例上我最近办理的那一条")
            .containsExactly(201L);
    }

    @Test
    void donePageAppliesFlowStatusAndOnlyReturnsOwnRecords() {
        ApiWorkflowTaskPageParam param = pageParam(1, 10);
        param.setFlowStatus("9");
        assertThat(taskManager.getDonePage(param, "1001").getRecords())
            .extracting(WorkflowHisTaskDTO::getHisTaskId)
            .containsExactly(205L);

        assertThat(taskManager.getDonePage(pageParam(1, 10), "1002").getRecords())
            .extracting(WorkflowHisTaskDTO::getHisTaskId)
            .containsExactly(204L);
    }

    @Test
    void donePageEnrichesFlowNameBusinessIdAndResolvesCollaborators() {
        Paging<WorkflowHisTaskDTO> paging = taskManager.getDonePage(pageParam(1, 10), "1001");
        WorkflowHisTaskDTO first = paging.getRecords().get(0);

        assertThat(first.getFlowName()).isEqualTo("出差流程");
        assertThat(first.getBusinessId()).isEqualTo("BIZ-305");
        assertThat(first.getSkipType()).isEqualTo("PASS");
        assertThat(first.getMessage()).isEqualTo("同意");

        WorkflowHisTaskDTO withCollaborator = paging.getRecords().stream()
            .filter(dto -> dto.getHisTaskId().equals(202L)).findFirst().orElseThrow();
        // 没有 org 契约时姓名解析退化为原始标识，分隔符保持与表内一致
        assertThat(withCollaborator.getCollaborator()).isEqualTo("1002,1003");
    }

    @Test
    void donePageReturnsNothingWithoutAnApprover() {
        assertThat(taskManager.getDonePage(pageParam(1, 10), null).getRecords()).isEmpty();
        assertThat(taskManager.getDonePage(pageParam(1, 10), "  ").getRecords()).isEmpty();
    }

    @Test
    void listTaskHandlerIdsReturnsEveryCollaboratorOfTheTask() {
        assertThat(taskManager.listTaskHandlerIds(101L)).containsExactlyInAnyOrder("1001", "1002", "1003");
        assertThat(taskManager.listTaskHandlerIds(103L)).containsExactly("2002");
        assertThat(taskManager.listTaskHandlerIds(404L)).isEmpty();
        assertThat(taskManager.listTaskHandlerIds(null)).isEmpty();
    }

    private static ApiWorkflowTaskPageParam pageParam(int current, int size) {
        ApiWorkflowTaskPageParam param = new ApiWorkflowTaskPageParam();
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

    private void insertInstance(long id, long definitionId, String businessId, int activityStatus)
        throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_INSTANCE)) {
            statement.setLong(1, id);
            statement.setLong(2, definitionId);
            statement.setString(3, businessId);
            statement.setInt(4, activityStatus);
            statement.executeUpdate();
        }
    }

    private void insertTask(long id, long definitionId, long instanceId, String nodeName, int nodeType,
                            String flowStatus, String createTime) throws SQLException {
        Timestamp timestamp = Timestamp.valueOf(createTime);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_TASK)) {
            statement.setLong(1, id);
            statement.setLong(2, definitionId);
            statement.setLong(3, instanceId);
            statement.setString(4, nodeName);
            statement.setInt(5, nodeType);
            statement.setString(6, flowStatus);
            statement.setTimestamp(7, timestamp);
            statement.setTimestamp(8, timestamp);
            statement.executeUpdate();
        }
    }

    private void insertUser(long id, String type, String processedBy, long associated) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_USER)) {
            statement.setLong(1, id);
            statement.setString(2, type);
            statement.setString(3, processedBy);
            statement.setLong(4, associated);
            statement.executeUpdate();
        }
    }

    private void insertHisTask(long id, long definitionId, long instanceId, String nodeName, String approver,
                               String collaborator, String flowStatus, String createTime) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_HIS_TASK)) {
            statement.setLong(1, id);
            statement.setLong(2, definitionId);
            statement.setLong(3, instanceId);
            statement.setLong(4, id);
            statement.setString(5, nodeName);
            statement.setString(6, approver);
            statement.setString(7, collaborator);
            statement.setString(8, flowStatus);
            statement.setString(9, "同意");
            statement.setTimestamp(10, Timestamp.valueOf(createTime));
            statement.executeUpdate();
        }
    }

}
