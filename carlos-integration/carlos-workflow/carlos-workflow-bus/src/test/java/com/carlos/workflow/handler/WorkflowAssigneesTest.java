package com.carlos.workflow.handler;

import com.carlos.core.response.Result;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.position.api.ApiOrgUserPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <p>
 * {@link WorkflowAssignees}（SpEL 助手 Bean {@code @workflowAssignees}）单元测试。
 * </p>
 * <p>
 * 覆盖 {@code report_to} 两个方向的语义：{@code upLeader} 读<b>用户自己的</b> {@code report_to}
 * （{@link ApiOrgUserPosition#getSuperiorByUserId(Long)}，规范 §4 的动态词汇，只能走 SpEL），
 * {@code subordinates} 按 {@code report_to = userId} 取<b>下级</b>
 * （{@link ApiOrgUserPosition#listSubordinatesByUserId(Long)}）。两者方向相反，不可互相替代。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowAssigneesTest {

    private ApiOrgUserPosition apiOrgUserPosition;

    private WorkflowAssignees workflowAssignees;

    @BeforeEach
    void setUp() {
        apiOrgUserPosition = mock(ApiOrgUserPosition.class);
        workflowAssignees = new WorkflowAssignees(apiOrgUserPosition);
    }

    @Test
    void upLeaderResolvesDirectSuperior() {
        when(apiOrgUserPosition.getSuperiorByUserId(1001L)).thenReturn(superiorResult(user(2001L, 1)));

        assertThat(workflowAssignees.upLeader(1001L))
            .as("直属上级读的是申请人自己的 report_to，不是以下级为条件的反查")
            .containsExactly("2001");
        verify(apiOrgUserPosition).getSuperiorByUserId(1001L);
        verify(apiOrgUserPosition, never()).listSubordinatesByUserId(any());
    }

    @Test
    void upLeaderWithLevelsWalksUpStepByStep() {
        when(apiOrgUserPosition.getSuperiorByUserId(1001L)).thenReturn(superiorResult(user(2001L, 1)));
        when(apiOrgUserPosition.getSuperiorByUserId(2001L)).thenReturn(superiorResult(user(3001L, 1)));

        assertThat(workflowAssignees.upLeader(1001L, 2))
            .as("upLeader(id, 2) 是「上级的上级」，逐层向上查")
            .containsExactly("3001");
        verify(apiOrgUserPosition).getSuperiorByUserId(1001L);
        verify(apiOrgUserPosition).getSuperiorByUserId(2001L);
    }

    @Test
    void upLeaderReturnsEmptyWhenChainBreaks() {
        when(apiOrgUserPosition.getSuperiorByUserId(1001L)).thenReturn(superiorResult(user(2001L, 1)));
        when(apiOrgUserPosition.getSuperiorByUserId(2001L)).thenReturn(Result.success(null));

        assertThat(workflowAssignees.upLeader(1001L, 2))
            .as("链断（上级没有上级）时返回空列表，不抛异常、不臆造部门负责人顶替")
            .isEmpty();
    }

    @Test
    void upLeaderReturnsEmptyWhenNoSuperiorConfigured() {
        when(apiOrgUserPosition.getSuperiorByUserId(1001L)).thenReturn(Result.success(null));

        assertThat(workflowAssignees.upLeader(1001L)).isEmpty();
        verify(apiOrgUserPosition).getSuperiorByUserId(1001L);
    }

    @Test
    void upLeaderReturnsEmptyWhenOrgCallFails() {
        when(apiOrgUserPosition.getSuperiorByUserId(1001L)).thenThrow(new IllegalStateException("org unavailable"));

        assertThat(workflowAssignees.upLeader(1001L))
            .as("org 不可用时返回空列表（引擎会把表达式原样保留为字面量），不向上抛异常打断流程")
            .isEmpty();
    }

    @Test
    void upLeaderStopsOnCyclicReportLine() {
        when(apiOrgUserPosition.getSuperiorByUserId(1001L)).thenReturn(superiorResult(user(2001L, 1)));
        when(apiOrgUserPosition.getSuperiorByUserId(2001L)).thenReturn(superiorResult(user(1001L, 1)));

        assertThat(workflowAssignees.upLeader(1001L, 3))
            .as("汇报线成环时按链断处理，不做无限次向上解析")
            .isEmpty();
    }

    @Test
    void upLeaderFiltersDisabledSuperior() {
        when(apiOrgUserPosition.getSuperiorByUserId(1001L)).thenReturn(superiorResult(user(2001L, 0)));

        assertThat(workflowAssignees.upLeader(1001L))
            .as("禁用账号（user_status=0）不能作为办理人")
            .isEmpty();
    }

    @Test
    void upLeaderRejectsInvalidArguments() {
        assertThat(workflowAssignees.upLeader(null)).isEmpty();
        assertThat(workflowAssignees.upLeader(1001L, 0)).isEmpty();
        assertThat(workflowAssignees.upLeader(1001L, 99)).isEmpty();
        verifyNoInteractions(apiOrgUserPosition);
    }

    @Test
    void subordinatesResolvesDirectSubordinates() {
        when(apiOrgUserPosition.listSubordinatesByUserId(1001L))
            .thenReturn(userResult(user(11L, 1), user(12L, 1)));

        assertThat(workflowAssignees.subordinates(1001L))
            .as("这是 report_to 上的另一个方向：直接下级（不递归）")
            .containsExactly("11", "12");
        verify(apiOrgUserPosition).listSubordinatesByUserId(1001L);
    }

    @Test
    void subordinatesReturnsEmptyWhenOrgCallFails() {
        when(apiOrgUserPosition.listSubordinatesByUserId(1001L))
            .thenThrow(new IllegalStateException("org unavailable"));

        assertThat(workflowAssignees.subordinates(1001L))
            .as("与 upLeader 同一口径：org 异常不外抛，降级为空列表")
            .isEmpty();
    }

    @Test
    void subordinatesFiltersDisabledUsersAndEmptyResponse() {
        when(apiOrgUserPosition.listSubordinatesByUserId(1001L))
            .thenReturn(userResult(user(11L, 1), user(12L, 0)));
        when(apiOrgUserPosition.listSubordinatesByUserId(1002L)).thenReturn(Result.success(null));

        assertThat(workflowAssignees.subordinates(1001L)).containsExactly("11");
        assertThat(workflowAssignees.subordinates(1002L)).isEmpty();
    }

    @Test
    void subordinatesRejectsNullUserId() {
        assertThat(workflowAssignees.subordinates(null)).isEmpty();
        verifyNoInteractions(apiOrgUserPosition);
    }

    private static OrgUserAO user(long id, Integer userStatus) {
        OrgUserAO user = new OrgUserAO();
        user.setId(id);
        user.setUserStatus(userStatus);
        return user;
    }

    private static Result<List<OrgUserAO>> userResult(OrgUserAO... users) {
        return Result.success(List.of(users));
    }

    private static Result<OrgUserAO> superiorResult(OrgUserAO user) {
        return Result.success(user);
    }

}
