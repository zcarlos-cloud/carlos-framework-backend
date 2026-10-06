package com.carlos.workflow.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgUser;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.api.pojo.ao.OrgUserCandidateAO;
import com.carlos.workflow.dto.WorkflowCandidateDTO;
import com.carlos.workflow.exception.WorkflowException;
import com.carlos.workflow.manager.impl.WorkflowOrgUserManagerImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <p>
 * 组织机构用户查询封装的单元测试：姓名批量解析与候选办理人分页的入参口径、降级策略。
 * </p>
 * <p>
 * 这里刻意用 mock 的 {@code ApiOrgUser} 而不是真实 Feign 调用，覆盖三件容易出错的事：
 * 一次调用解析整页姓名（不是逐行）、非法标识（没被表达式求值替换掉的字面量）被跳过、
 * org 不可用时「姓名降级为空、候选清单直接报错」这条不一致但正确的取舍。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowOrgUserManagerTest {

    private final ApiOrgUser apiOrgUser = mock(ApiOrgUser.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<ApiOrgUser> provider = mock(ObjectProvider.class);

    private final WorkflowOrgUserManager manager = new WorkflowOrgUserManagerImpl(provider);

    @Test
    void mapNamesResolvesTheWholePageInOneCall() {
        when(provider.getIfAvailable()).thenReturn(apiOrgUser);
        when(apiOrgUser.listUsersByIds(any())).thenReturn(Result.success(List.of(
            user(1001L, "zhangsan", "张三"), user(1002L, "lisi", "李四"))));

        Map<String, String> names = manager.mapNames(List.of("1001", "1002", "1001"));

        assertThat(names).containsExactlyInAnyOrderEntriesOf(Map.of("1001", "张三", "1002", "李四"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(apiOrgUser, times(1)).listUsersByIds(captor.capture());
        assertThat(captor.getValue())
            .as("一次批量调用，且 id 去重")
            .containsExactly(1001L, 1002L);
    }

    @Test
    void mapNamesSkipsIdentifiersThatAreNotUserIds() {
        when(provider.getIfAvailable()).thenReturn(apiOrgUser);
        when(apiOrgUser.listUsersByIds(any())).thenReturn(Result.success(List.of(user(1001L, "zhangsan", "张三"))));

        Map<String, String> names = manager.mapNames(List.of("1001", "role:5", "  ", ""));

        assertThat(names).containsExactly(Map.entry("1001", "张三"));
    }

    @Test
    void mapNamesFallsBackToTheAccountWhenNicknameIsBlank() {
        when(provider.getIfAvailable()).thenReturn(apiOrgUser);
        when(apiOrgUser.listUsersByIds(any()))
            .thenReturn(Result.success(List.of(user(1001L, "zhangsan", null))));

        assertThat(manager.mapNames(List.of("1001"))).containsEntry("1001", "zhangsan");
    }

    @Test
    void mapNamesDegradesToEmptyWhenOrgIsUnavailable() {
        when(provider.getIfAvailable()).thenReturn(null);

        assertThat(manager.mapNames(List.of("1001"))).isEmpty();

        when(provider.getIfAvailable()).thenReturn(apiOrgUser);
        when(apiOrgUser.listUsersByIds(any())).thenReturn(Result.error("组织机构服务不可用"));
        assertThat(manager.mapNames(List.of("1001"))).isEmpty();
    }

    @Test
    void mapNamesDoesNotCallOrgWithoutIds() {
        assertThat(manager.mapNames(List.of())).isEmpty();
        assertThat(manager.mapNames(null)).isEmpty();

        verifyNoInteractions(provider);
    }

    @Test
    void candidatePagePassesDeptIdIncludeAndExcludeWhenPresent() {
        when(provider.getIfAvailable()).thenReturn(apiOrgUser);
        when(apiOrgUser.pageCandidates(eq(12L), any(), any(), eq(2), eq(5)))
            .thenReturn(Result.success(candidatePaging()));

        Paging<WorkflowCandidateDTO> paging = manager.getCandidatePage(12L, List.of("1001"), List.of("1002"), 2, 5);

        assertThat(paging.getTotal()).isEqualTo(1);
        assertThat(paging.getCurrent()).isEqualTo(2);
        assertThat(paging.getSize()).isEqualTo(5);
        assertThat(paging.getRecords()).singleElement().satisfies(dto -> {
            assertThat(dto.getUserId()).isEqualTo(1001L);
            assertThat(dto.getCode()).isEqualTo("zhangsan");
            assertThat(dto.getName()).isEqualTo("张三");
            assertThat(dto.getDeptId()).isEqualTo(12L);
            assertThat(dto.getDeptName()).isEqualTo("研发部");
        });

        verify(apiOrgUser).pageCandidates(12L, List.of(1001L), List.of(1002L), 2, 5);
    }

    @Test
    void candidatePageOmitsEmptyIncludeAndExclude() {
        when(provider.getIfAvailable()).thenReturn(apiOrgUser);
        when(apiOrgUser.pageCandidates(isNull(), isNull(), isNull(), any(Integer.class), any(Integer.class)))
            .thenReturn(Result.success(candidatePaging()));

        manager.getCandidatePage(null, null, List.of(), 1, 10);

        verify(apiOrgUser).pageCandidates(null, null, null, 1, 10);
    }

    @Test
    void candidatePageReturnsEmptyWithoutCallingOrgWhenIncludeIsAnEmptySet() {
        Paging<WorkflowCandidateDTO> paging = manager.getCandidatePage(null, List.of(), null, 1, 10);

        assertThat(paging.getRecords()).isEmpty();
        verifyNoInteractions(provider);
    }

    @Test
    void candidatePageFailsLoudlyWhenOrgIsUnavailable() {
        when(provider.getIfAvailable()).thenReturn(null);

        Throwable thrown = catchThrowable(() -> manager.getCandidatePage(null, null, null, 1, 10));

        assertThat(thrown)
            .as("候选清单为空与「远程服务不可用」必须区分开，否则前端会以为流程没人可选")
            .isInstanceOf(WorkflowException.class);
        assertThat(((WorkflowException) thrown).getErrorCode()).isEqualTo("30001");
    }

    private OrgUserAO user(Long id, String account, String nickname) {
        OrgUserAO user = new OrgUserAO();
        user.setId(id);
        user.setAccount(account);
        user.setNickname(nickname);
        return user;
    }

    private Paging<OrgUserCandidateAO> candidatePaging() {
        OrgUserCandidateAO candidate = new OrgUserCandidateAO();
        candidate.setUserId(1001L);
        candidate.setCode("zhangsan");
        candidate.setName("张三");
        candidate.setDeptId(12L);
        candidate.setDeptName("研发部");
        Paging<OrgUserCandidateAO> paging = new Paging<>();
        paging.setTotal(1);
        paging.setCurrent(2);
        paging.setSize(5);
        paging.setPages(1);
        paging.setRecords(List.of(candidate));
        return paging;
    }

}
