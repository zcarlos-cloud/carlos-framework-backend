package com.carlos.workflow.handler;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgDepartment;
import com.carlos.org.api.ApiOrgRole;
import com.carlos.org.api.ApiOrgUser;
import com.carlos.org.api.pojo.ao.OrgDepartmentAO;
import com.carlos.org.api.pojo.ao.OrgRoleAO;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.position.api.ApiOrgPosition;
import com.carlos.org.position.api.pojo.ao.OrgPositionAO;
import org.dromara.warm.flow.ui.dto.HandlerQuery;
import org.dromara.warm.flow.ui.dto.HandlerFeedBackDto;
import org.dromara.warm.flow.ui.vo.HandlerAuth;
import org.dromara.warm.flow.ui.vo.HandlerFeedBackVo;
import org.dromara.warm.flow.ui.vo.HandlerSelectVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>
 * {@link DesignerHandlerSelectService} 单元测试：页签分派、分页与关键字、降级，
 * 以及 {@code handlerFeedback} 的分流批量查名与顺序对齐。
 * </p>
 * <p>
 * 全部协作者为 mock：本机没有可达的 org 服务。org 契约经 {@link StaticListableBeanFactory}
 * 包成 {@code ObjectProvider}，用于覆盖「org 契约缺席」这一条装配路径。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class DesignerHandlerSelectServiceTest {

    private ApiOrgUser apiOrgUser;

    private ApiOrgRole apiOrgRole;

    private ApiOrgDepartment apiOrgDepartment;

    private ApiOrgPosition apiOrgPosition;

    @BeforeEach
    void setUp() {
        apiOrgUser = mock(ApiOrgUser.class);
        apiOrgRole = mock(ApiOrgRole.class);
        apiOrgDepartment = mock(ApiOrgDepartment.class);
        apiOrgPosition = mock(ApiOrgPosition.class);
    }

    // ==================== 页签 ====================

    @Test
    void handlerTypeListsEveryTab() {
        assertThat(service().getHandlerType()).containsExactly("用户", "角色", "部门", "部门(含下级)", "岗位");
    }

    // ==================== 各页签的候选与 storageId ====================

    @Test
    void userTabEmitsBareUserIdAndFallsBackToAccountWhenNicknameIsBlank() {
        when(apiOrgUser.pageUserOptions(any(), any(), anyInt(), anyInt()))
            .thenReturn(Result.success(paging(1, user(1001L, "zhangsan", "张三"))));

        HandlerSelectVo vo = service().getHandlerSelect(query("用户"));

        assertThat(vo.getHandlerAuths().getTotal()).isEqualTo(1);
        assertThat(vo.getHandlerAuths().getRows()).singleElement().satisfies(row -> {
            assertThat(row.getStorageId())
                .as("「用户」页签按官方约定产出裸 id（词汇表判为 LITERAL 原样透传）")
                .isEqualTo("1001");
            assertThat(row.getHandlerCode()).isEqualTo("zhangsan");
            assertThat(row.getHandlerName()).isEqualTo("张三");
            assertThat(row.getCreateTime())
                .as("SPA 的 parseTime 按 yyyy-MM-dd HH:mm:ss 解析，格式不能漂")
                .isEqualTo("2026-09-25 10:30:00");
        });

        when(apiOrgUser.pageUserOptions(any(), any(), anyInt(), anyInt()))
            .thenReturn(Result.success(paging(1, user(1002L, "lisi", "  "))));
        assertThat(rowsOf(service().getHandlerSelect(query("用户"))))
            .singleElement()
            .satisfies(row -> assertThat(row.getHandlerName())
                .as("昵称不是必填，为空时退回账号，别在列表里显示空白行")
                .isEqualTo("lisi"));
    }

    @Test
    void userTabPassesKeywordAndPagingToOrg() {
        when(apiOrgUser.pageUserOptions(any(), any(), anyInt(), anyInt()))
            .thenReturn(Result.success(paging(0)));

        service().getHandlerSelect(query("用户", q -> {
            q.setHandlerCode(" zhang ");
            q.setHandlerName(" 三 ");
            q.setPageNum(3);
            q.setPageSize(50);
        }));

        verify(apiOrgUser).pageUserOptions("zhang", "三", 3, 50);
    }

    @Test
    void roleTabEmitsRolePrefixedStorageId() {
        OrgRoleAO role = new OrgRoleAO();
        role.setId(5L);
        role.setRoleCode("deptAdmin");
        role.setRoleName("部门管理员");
        when(apiOrgRole.pageRoleOptions(any(), any(), anyInt(), anyInt()))
            .thenReturn(Result.success(paging(1, role)));

        assertThat(rowsOf(service().getHandlerSelect(query("角色"))))
            .singleElement()
            .satisfies(row -> {
                assertThat(row.getStorageId()).isEqualTo("role:5");
                assertThat(row.getHandlerCode()).isEqualTo("deptAdmin");
                assertThat(row.getHandlerName()).isEqualTo("部门管理员");
            });
    }

    @Test
    void deptTabEmitsNonRecursiveStorageIdAndDeptRecursiveTabEmitsFlaggedOne() {
        when(apiOrgDepartment.listAllDepartmentOptions())
            .thenReturn(Result.success(List.of(department(12L, 1L, "研发部", "RD"), department(13L, 12L, "后端组", "RD-BE"))));

        assertThat(rowsOf(service().getHandlerSelect(query("部门"))))
            .extracting(HandlerAuth::getStorageId)
            .as("「部门」页签是不含下级的语义")
            .containsExactly("dept:12", "dept:13");
        assertThat(rowsOf(service().getHandlerSelect(query("部门(含下级)"))))
            .extracting(HandlerAuth::getStorageId)
            .as("「部门(含下级)」页签必须带上 rec 标记，两个页签不能产出同一种写法")
            .containsExactly("dept:12:rec", "dept:13:rec");
    }

    @Test
    void deptTabFiltersByKeywordAndPagesInMemory() {
        when(apiOrgDepartment.listAllDepartmentOptions()).thenReturn(Result.success(List.of(
            department(12L, 1L, "研发部", "RD"),
            department(13L, 1L, "研发中心", "RD-CENTER"),
            department(14L, 1L, "市场部", "MKT"))));

        HandlerQuery firstPage = query("部门", q -> {
            q.setHandlerName("研发");
            q.setPageSize(1);
        });
        HandlerSelectVo page1 = service().getHandlerSelect(firstPage);

        assertThat(rowsOf(page1)).extracting(HandlerAuth::getStorageId).containsExactly("dept:12");
        assertThat(page1.getHandlerAuths().getTotal())
            .as("总数是「过滤后」的数量，否则 SPA 的触底加载会一直请求不存在的下一页")
            .isEqualTo(2);

        HandlerSelectVo page2 = service().getHandlerSelect(query("部门", q -> {
            q.setHandlerName("研发");
            q.setPageSize(1);
            q.setPageNum(2);
        }));
        assertThat(rowsOf(page2)).extracting(HandlerAuth::getStorageId).containsExactly("dept:13");
    }

    @Test
    void deptTabFillsGroupNameWithParentDepartmentName() {
        when(apiOrgDepartment.listAllDepartmentOptions()).thenReturn(Result.success(List.of(
            department(1L, null, "总部", "HQ"),
            department(12L, 1L, "研发部", "RD"))));

        assertThat(rowsOf(service().getHandlerSelect(query("部门"))))
            .satisfiesExactly(
                root -> assertThat(root.getGroupName()).as("顶级部门没有上级").isNull(),
                child -> assertThat(child.getGroupName()).isEqualTo("总部"));
    }

    @Test
    void positionTabEmitsPositionPrefixedStorageId() {
        OrgPositionAO position = new OrgPositionAO();
        position.setId(7L);
        position.setPositionCode("JAVA_DEV");
        position.setPositionName("Java开发工程师");
        when(apiOrgPosition.pagePositionOptions(any(), any(), anyInt(), anyInt()))
            .thenReturn(Result.success(paging(1, position)));

        assertThat(rowsOf(service().getHandlerSelect(query("岗位"))))
            .singleElement()
            .satisfies(row -> {
                assertThat(row.getStorageId()).isEqualTo("position:7");
                assertThat(row.getHandlerCode()).isEqualTo("JAVA_DEV");
                assertThat(row.getHandlerName()).isEqualTo("Java开发工程师");
            });
    }

    // ==================== 降级与防御 ====================

    @Test
    void unknownHandlerTypeReturnsEmptyResultInsteadOfNull() {
        assertThat(service().getHandlerSelect(query("审批人")))
            .as("引擎会把 null 原样放进响应，SPA 取 data.handlerAuths.rows 时会直接报错")
            .isNotNull()
            .satisfies(vo -> {
                assertThat(vo.getHandlerAuths().getRows()).isEmpty();
                assertThat(vo.getHandlerAuths().getTotal()).isZero();
                assertThat(vo.getTreeSelections()).isNotNull();
            });
        assertThat(service().getHandlerSelect(null)).isNotNull();
    }

    @Test
    void orgFailureIsDegradedToEmptyResultAndDoNotBreakTheDialog() {
        when(apiOrgDepartment.listAllDepartmentOptions()).thenThrow(new IllegalStateException("org unavailable"));

        HandlerSelectVo vo = service().getHandlerSelect(query("部门"));

        assertThat(vo).isNotNull();
        assertThat(rowsOf(vo)).isEmpty();
    }

    @Test
    void missingOrgContractIsDegradedToEmptyResult() {
        DesignerHandlerSelectService service = service(null, null, null, null);

        for (String label : service.getHandlerType()) {
            assertThat(service.getHandlerSelect(query(label)))
                .as("org 契约未注册（宿主未开启 @EnableFeignClients）时页签照常列出，只是没有数据")
                .isNotNull()
                .satisfies(vo -> assertThat(rowsOf(vo)).isEmpty());
        }
    }

    @Test
    void pageParamsAreDefaultedAndCappedBeforeReachingOrg() {
        when(apiOrgUser.pageUserOptions(any(), any(), anyInt(), anyInt()))
            .thenReturn(Result.success(paging(0)));

        service().getHandlerSelect(query("用户"));
        verify(apiOrgUser).pageUserOptions(null, null, 1, 10);

        service().getHandlerSelect(query("用户", q -> {
            q.setPageNum(0);
            q.setPageSize(5000);
        }));
        verify(apiOrgUser).pageUserOptions(null, null, 1, 200);
    }

    @Test
    void treeSelectionsIsAlwaysAnEmptyListNeverNull() {
        when(apiOrgUser.pageUserOptions(any(), any(), anyInt(), anyInt()))
            .thenReturn(Result.success(paging(0)));

        assertThat(service().getHandlerSelect(query("用户")).getTreeSelections())
            .as("SPA 直接取 data.treeSelections 渲染左侧树，给 null 会让 v-if 之外的渲染路径出错")
            .isNotNull()
            .isEmpty();
    }

    // ==================== 回显 handlerFeedback ====================

    @Test
    void handlerFeedbackReturnsEmptyListForEmptyInput() {
        assertThat(service().handlerFeedback(null)).isEmpty();
        assertThat(service().handlerFeedback(List.of())).isEmpty();
    }

    @Test
    void handlerFeedbackKeepsInputOrderAndResolvesEveryTab() {
        when(apiOrgRole.listRoleOptions(any())).thenReturn(Result.success(List.of(role(5L, "部门管理员"))));
        when(apiOrgDepartment.listDepartmentOptions(any()))
            .thenReturn(Result.success(List.of(department(12L, null, "研发部", "RD"))));
        when(apiOrgUser.listUsersByIds(any())).thenReturn(Result.success(List.of(user(1001L, "zhangsan", "张三"))));
        when(apiOrgPosition.listPositionOptions(any())).thenReturn(Result.success(List.of(position(7L, "Java开发工程师"))));

        HandlerFeedBackDto dto = new HandlerFeedBackDto();
        dto.setStorageIds(List.of(
            "position:7", "dept:12:rec", "1001", "role:5", "dept:12", "user:1001", "manager:5", ""));

        List<HandlerFeedBackVo> feedback = service().handlerFeedback(dto.getStorageIds());

        assertThat(feedback).extracting(HandlerFeedBackVo::getStorageId).containsExactly(
            "position:7", "dept:12:rec", "1001", "role:5", "dept:12", "user:1001", "manager:5", "");
        assertThat(feedback).extracting(HandlerFeedBackVo::getHandlerName).containsExactly(
            "Java开发工程师", "研发部", "张三", "部门管理员", "研发部", "张三",
            "", "");
    }

    @Test
    void handlerFeedbackBatchesOneCallPerNamespaceInsteadOfLoadingEveryTab() {
        when(apiOrgRole.listRoleOptions(any())).thenReturn(Result.success(List.of()));
        when(apiOrgDepartment.listDepartmentOptions(any())).thenReturn(Result.success(List.of()));
        when(apiOrgUser.listUsersByIds(any())).thenReturn(Result.success(List.of()));
        when(apiOrgPosition.listPositionOptions(any())).thenReturn(Result.success(List.of()));

        service().handlerFeedback(List.of("role:5", "role:6", "role:5", "dept:12", "dept:12:rec", "1001", "position:7"));

        verify(apiOrgRole, times(1)).listRoleOptions(any());
        verify(apiOrgDepartment, times(1)).listDepartmentOptions(any());
        verify(apiOrgUser, times(1)).listUsersByIds(any());
        verify(apiOrgPosition, times(1)).listPositionOptions(any());
    }

    @Test
    void handlerFeedbackQueriesEachNamespaceWithTheIdsItActuallyNeeds() {
        when(apiOrgRole.listRoleOptions(any())).thenReturn(Result.success(List.of()));
        when(apiOrgDepartment.listDepartmentOptions(any())).thenReturn(Result.success(List.of()));
        when(apiOrgUser.listUsersByIds(any())).thenReturn(Result.success(List.of()));
        when(apiOrgPosition.listPositionOptions(any())).thenReturn(Result.success(List.of()));

        service().handlerFeedback(List.of("role:5", "role:6", "dept:12", "dept:13:rec", "1001", "position:7"));

        verify(apiOrgRole).listRoleOptions(List.of(5L, 6L));
        verify(apiOrgDepartment).listDepartmentOptions(List.of(12L, 13L));
        verify(apiOrgUser).listUsersByIds(List.of(1001L));
        verify(apiOrgPosition).listPositionOptions(List.of(7L));
    }

    @Test
    void handlerFeedbackResolvesUserPrefixWithCommaSeparatedIds() {
        when(apiOrgUser.listUsersByIds(any())).thenReturn(Result.success(List.of(
            user(1001L, "zhangsan", "张三"), user(1002L, "lisi", "李四"))));

        assertThat(service().handlerFeedback(List.of("user:1001,1002")))
            .singleElement()
            .satisfies(vo -> assertThat(vo.getHandlerName())
                .as("手写的 user:{id,id} 也要回显，多个名字用顿号连起来")
                .isEqualTo("张三、李四"));
    }

    @Test
    void handlerFeedbackReturnsEmptyNameWhenOneNamespaceFailsButKeepsTheOthers() {
        when(apiOrgRole.listRoleOptions(any())).thenThrow(new IllegalStateException("org unavailable"));
        when(apiOrgUser.listUsersByIds(any())).thenReturn(Result.success(List.of(user(1001L, "zhangsan", "张三"))));

        assertThat(service().handlerFeedback(List.of("role:5", "1001")))
            .as("一个命名空间查不到名字不能让整次回显失败，否则设计器里的节点会显示成一串 storageId")
            .satisfiesExactly(
                role -> assertThat(role.getHandlerName()).isEmpty(),
                user -> assertThat(user.getHandlerName()).isEqualTo("张三"));
    }

    @Test
    void handlerFeedbackReturnsEmptyNameForIdsOrgDoesNotKnow() {
        when(apiOrgUser.listUsersByIds(any())).thenReturn(Result.success(List.of()));

        assertThat(service().handlerFeedback(List.of("9999"))).singleElement()
            .satisfies(vo -> assertThat(vo.getHandlerName())
                .as("已删除的用户回显为空串，SPA 会退回显示 storageId 原文")
                .isEmpty());
    }

    @Test
    void handlerFeedbackSkipsOrgCallsForNamespacesThatAreNotReferenced() {
        when(apiOrgUser.listUsersByIds(any())).thenReturn(Result.success(List.of(user(1001L, "zhangsan", "张三"))));

        assertThat(service().handlerFeedback(List.of("1001"))).singleElement()
            .satisfies(vo -> assertThat(vo.getHandlerName()).isEqualTo("张三"));
        verify(apiOrgRole, times(0)).listRoleOptions(any());
        verify(apiOrgDepartment, times(0)).listDepartmentOptions(any());
        verify(apiOrgPosition, times(0)).listPositionOptions(any());
    }

    // ==================== 装配 ====================

    private DesignerHandlerSelectService service() {
        return service(apiOrgUser, apiOrgRole, apiOrgDepartment, apiOrgPosition);
    }

    private static DesignerHandlerSelectService service(ApiOrgUser user, ApiOrgRole role,
                                                        ApiOrgDepartment department, ApiOrgPosition position) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        if (user != null) {
            beanFactory.addBean("apiOrgUser", user);
        }
        if (role != null) {
            beanFactory.addBean("apiOrgRole", role);
        }
        if (department != null) {
            beanFactory.addBean("apiOrgDepartment", department);
        }
        if (position != null) {
            beanFactory.addBean("apiOrgPosition", position);
        }
        return new DesignerHandlerSelectService(
            beanFactory.getBeanProvider(ApiOrgUser.class),
            beanFactory.getBeanProvider(ApiOrgRole.class),
            beanFactory.getBeanProvider(ApiOrgDepartment.class),
            beanFactory.getBeanProvider(ApiOrgPosition.class));
    }

    // ==================== 测试数据 ====================

    private static HandlerQuery query(String handlerType) {
        return query(handlerType, null);
    }

    private static HandlerQuery query(String handlerType, Consumer<HandlerQuery> customizer) {
        HandlerQuery query = new HandlerQuery();
        query.setHandlerType(handlerType);
        if (customizer != null) {
            customizer.accept(query);
        }
        return query;
    }

    private static List<HandlerAuth> rowsOf(HandlerSelectVo vo) {
        return vo.getHandlerAuths().getRows();
    }

    private static <T> Paging<T> paging(long total, T... records) {
        Paging<T> paging = new Paging<>();
        paging.setTotal((int) total);
        paging.setCurrent(1);
        paging.setSize(10);
        paging.setPages(1);
        paging.setRecords(List.of(records));
        return paging;
    }

    private static OrgUserAO user(long id, String account, String nickname) {
        OrgUserAO user = new OrgUserAO();
        user.setId(id);
        user.setAccount(account);
        user.setNickname(nickname);
        user.setCreateTime(LocalDateTime.of(2026, 9, 25, 10, 30));
        return user;
    }

    private static OrgRoleAO role(long id, String name) {
        OrgRoleAO role = new OrgRoleAO();
        role.setId(id);
        role.setRoleName(name);
        return role;
    }

    private static OrgDepartmentAO department(long id, Long parentId, String name, String code) {
        OrgDepartmentAO department = new OrgDepartmentAO();
        department.setId(id);
        department.setParentId(parentId);
        department.setDeptName(name);
        department.setDeptCode(code);
        return department;
    }

    private static OrgPositionAO position(long id, String name) {
        OrgPositionAO position = new OrgPositionAO();
        position.setId(id);
        position.setPositionName(name);
        return position;
    }

}
