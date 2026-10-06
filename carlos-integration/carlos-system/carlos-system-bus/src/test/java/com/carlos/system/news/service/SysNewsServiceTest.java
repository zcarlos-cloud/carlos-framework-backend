package com.carlos.system.news.service;

import com.carlos.core.exception.BusinessException;
import com.carlos.security.auth.UserContext;
import com.carlos.security.util.SecurityUtils;
import com.carlos.system.news.manager.SysNewsManager;
import com.carlos.system.news.manager.SysNewsReadManager;
import com.carlos.system.news.pojo.dto.SysNewsDTO;
import com.carlos.system.news.pojo.entity.SysNewsRead;
import com.carlos.system.news.pojo.enums.SysNewsScopeEnum;
import com.carlos.system.news.pojo.enums.SysNewsStatusEnum;
import com.carlos.system.news.pojo.enums.SysNewsTypeEnum;
import com.carlos.system.news.pojo.param.SysNewsMyPageParam;
import com.carlos.system.news.pojo.vo.SysNewsVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysNewsService 单元测试
 *
 * <p>S3 公告：草稿/发布/下线状态机、发布范围角色过滤、已读/未读数。</p>
 *
 * @author carlos
 */
@ExtendWith(MockitoExtension.class)
class SysNewsServiceTest {

    @Mock
    private SysNewsManager newsManager;

    @Mock
    private SysNewsReadManager newsReadManager;

    @InjectMocks
    private SysNewsService newsService;

    private SysNewsDTO draftNews;

    @BeforeEach
    void setUp() {
        draftNews = new SysNewsDTO()
            .setId(100L)
            .setTitle("系统升级公告")
            .setNewsType(SysNewsTypeEnum.ANNOUNCEMENT)
            .setContent("本周六凌晨系统升级")
            .setPublishScope(SysNewsScopeEnum.ALL)
            .setStatus(SysNewsStatusEnum.DRAFT);
    }

    private UserContext mockLoginUser(Long userId, Set<String> roleCodes) {
        UserContext user = new UserContext();
        user.setUserId(userId);
        user.setRoleCodes(roleCodes);
        return user;
    }

    @Test
    @DisplayName("新增公告：默认草稿状态且清空发布时间")
    void should_create_draft_news_when_add() {
        when(newsManager.add(any())).thenReturn(true);

        newsService.addNews(draftNews, null);

        assertEquals(SysNewsStatusEnum.DRAFT, draftNews.getStatus());
        verify(newsManager).add(draftNews);
    }

    @Test
    @DisplayName("新增公告：指定角色发布但未选角色时抛异常")
    void should_throw_when_role_scope_without_roles() {
        draftNews.setPublishScope(SysNewsScopeEnum.ROLE);

        assertThrows(BusinessException.class, () -> newsService.addNews(draftNews, null));
        verify(newsManager, never()).add(any());
    }

    @Test
    @DisplayName("新增公告：指定角色发布时序列化角色标识为 JSON 数组")
    void should_serialize_scope_roles_when_role_scope() {
        when(newsManager.add(any())).thenReturn(true);

        newsService.addNews(draftNews.setPublishScope(SysNewsScopeEnum.ROLE), List.of("10", "20"));

        assertEquals("[\"10\",\"20\"]", draftNews.getScopeRoleIds());
    }

    @Test
    @DisplayName("新增公告：全员发布时清空角色列表")
    void should_clear_scope_roles_when_all_scope() {
        when(newsManager.add(any())).thenReturn(true);

        newsService.addNews(draftNews, List.of("10"));

        assertNull(draftNews.getScopeRoleIds());
    }

    @Test
    @DisplayName("修改公告：已发布公告不可编辑")
    void should_throw_when_update_published() {
        draftNews.setStatus(SysNewsStatusEnum.PUBLISHED);
        when(newsManager.getDtoById(100L)).thenReturn(draftNews);

        SysNewsDTO update = new SysNewsDTO().setId(100L).setPublishScope(SysNewsScopeEnum.ALL);
        assertThrows(BusinessException.class, () -> newsService.updateNews(update, null));
        verify(newsManager, never()).modify(any());
    }

    @Test
    @DisplayName("修改公告：状态与发布时间不被编辑覆盖")
    void should_keep_status_when_update_draft() {
        when(newsManager.getDtoById(100L)).thenReturn(draftNews);
        when(newsManager.modify(any())).thenReturn(true);

        SysNewsDTO update = new SysNewsDTO().setId(100L).setPublishScope(SysNewsScopeEnum.ALL);
        newsService.updateNews(update, null);

        assertEquals(SysNewsStatusEnum.DRAFT, update.getStatus());
        verify(newsManager).modify(update);
    }

    @Test
    @DisplayName("删除公告：已发布公告不可删除")
    void should_throw_when_delete_published() {
        draftNews.setStatus(SysNewsStatusEnum.PUBLISHED);
        when(newsManager.getDtoById(100L)).thenReturn(draftNews);

        assertThrows(BusinessException.class, () -> newsService.deleteNews(Set.of(100L)));
        verify(newsManager, never()).delete(any());
    }

    @Test
    @DisplayName("发布公告：草稿发布成功并写入发布时间")
    void should_publish_draft_news() {
        when(newsManager.getDtoById(100L)).thenReturn(draftNews);
        when(newsManager.modify(any())).thenReturn(true);

        newsService.publish(100L);

        ArgumentCaptor<SysNewsDTO> captor = ArgumentCaptor.forClass(SysNewsDTO.class);
        verify(newsManager).modify(captor.capture());
        assertEquals(SysNewsStatusEnum.PUBLISHED, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getPublishTime());
    }

    @Test
    @DisplayName("发布公告：已过有效期的公告不可发布")
    void should_throw_when_publish_expired() {
        draftNews.setExpireTime(LocalDateTime.now().minusDays(1));
        when(newsManager.getDtoById(100L)).thenReturn(draftNews);

        assertThrows(BusinessException.class, () -> newsService.publish(100L));
        verify(newsManager, never()).modify(any());
    }

    @Test
    @DisplayName("发布公告：重复发布抛异常")
    void should_throw_when_publish_twice() {
        draftNews.setStatus(SysNewsStatusEnum.PUBLISHED);
        when(newsManager.getDtoById(100L)).thenReturn(draftNews);

        assertThrows(BusinessException.class, () -> newsService.publish(100L));
    }

    @Test
    @DisplayName("下线公告：未发布公告不可下线")
    void should_throw_when_offline_draft() {
        when(newsManager.getDtoById(100L)).thenReturn(draftNews);

        assertThrows(BusinessException.class, () -> newsService.offline(100L));
    }

    @Test
    @DisplayName("我的公告：指定角色公告仅角色匹配用户可见")
    void should_filter_role_scoped_news_when_my_page() {
        SysNewsDTO roleNews = new SysNewsDTO()
            .setId(101L)
            .setTitle("角色公告")
            .setNewsType(SysNewsTypeEnum.NOTICE)
            .setPublishScope(SysNewsScopeEnum.ROLE)
            .setScopeRoleIds("[10,20]")
            .setStatus(SysNewsStatusEnum.PUBLISHED);
        when(newsManager.listPublishedActive()).thenReturn(List.of(draftNews, roleNews));
        lenient().when(newsReadManager.getReadNewsIds(anyLong(), anyCollection())).thenReturn(Set.of(101L));

        UserContext user = mockLoginUser(1L, Set.of("20"));
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(user);

            SysNewsMyPageParam param = new SysNewsMyPageParam();
            IPage<SysNewsVO> page = newsService.myPage(param);

            // 用户角色 20 命中角色公告：两条都可见
            assertEquals(2, page.getTotal());
            SysNewsVO roleVo = page.getRecords().stream().filter(v -> v.getId().equals(101L)).findFirst().orElseThrow();
            assertEquals(Boolean.TRUE, roleVo.getIsRead());
        }
    }

    @Test
    @DisplayName("我的公告：角色不匹配时角色公告不可见")
    void should_hide_role_scoped_news_when_role_not_matched() {
        SysNewsDTO roleNews = new SysNewsDTO()
            .setId(101L)
            .setPublishScope(SysNewsScopeEnum.ROLE)
            .setScopeRoleIds("[10,20]")
            .setStatus(SysNewsStatusEnum.PUBLISHED);
        when(newsManager.listPublishedActive()).thenReturn(List.of(draftNews, roleNews));
        when(newsReadManager.getReadNewsIds(anyLong(), anyCollection())).thenReturn(Set.of());

        UserContext user = mockLoginUser(1L, Set.of("99"));
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(user);

            IPage<SysNewsVO> page = newsService.myPage(new SysNewsMyPageParam());

            assertEquals(1, page.getTotal());
            assertEquals(100L, page.getRecords().get(0).getId());
        }
    }

    @Test
    @DisplayName("未读数：可见公告减去已读")
    void should_count_unread() {
        SysNewsDTO roleNews = new SysNewsDTO()
            .setId(101L)
            .setPublishScope(SysNewsScopeEnum.ALL)
            .setStatus(SysNewsStatusEnum.PUBLISHED);
        when(newsManager.listPublishedActive()).thenReturn(List.of(draftNews, roleNews));
        when(newsReadManager.getReadNewsIds(anyLong(), anyCollection())).thenReturn(Set.of(100L));

        UserContext user = mockLoginUser(1L, Set.of());
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(user);

            assertEquals(1, newsService.unreadCount());
        }
    }

    @Test
    @DisplayName("删除公告：删除成功时级联清理已读记录")
    void should_cascade_read_records_when_delete() {
        when(newsManager.getDtoById(100L)).thenReturn(draftNews);
        when(newsManager.delete(Set.of(100L))).thenReturn(true);

        newsService.deleteNews(Set.of(100L));

        verify(newsReadManager).deleteByNewsIds(Set.of(100L));
    }

    @Test
    @DisplayName("标记已读：公告不存在时抛异常")
    void should_throw_when_read_not_found() {
        when(newsManager.getDtoById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> newsService.read(999L));
        verify(newsReadManager, never()).markRead(any());
    }

    @Test
    @DisplayName("标记已读：角色不匹配的公告按不存在处理")
    void should_throw_when_read_invisible_news() {
        SysNewsDTO roleNews = new SysNewsDTO()
            .setId(101L)
            .setPublishScope(SysNewsScopeEnum.ROLE)
            .setScopeRoleIds("[10,20]")
            .setStatus(SysNewsStatusEnum.PUBLISHED);
        when(newsManager.getDtoById(101L)).thenReturn(roleNews);

        UserContext user = mockLoginUser(1L, Set.of("99"));
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(user);

            assertThrows(BusinessException.class, () -> newsService.read(101L));
            verify(newsReadManager, never()).markRead(any());
        }
    }

    @Test
    @DisplayName("我的公告详情：草稿按不存在处理")
    void should_throw_when_my_detail_draft() {
        when(newsManager.getDtoById(100L)).thenReturn(draftNews);

        UserContext user = mockLoginUser(1L, Set.of());
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(user);

            assertThrows(BusinessException.class, () -> newsService.myDetail(100L));
        }
    }

    @Test
    @DisplayName("我的公告详情：角色不匹配时按不存在处理")
    void should_throw_when_my_detail_invisible() {
        SysNewsDTO roleNews = new SysNewsDTO()
            .setId(101L)
            .setPublishScope(SysNewsScopeEnum.ROLE)
            .setScopeRoleIds("[10,20]")
            .setStatus(SysNewsStatusEnum.PUBLISHED);
        when(newsManager.getDtoById(101L)).thenReturn(roleNews);

        UserContext user = mockLoginUser(1L, Set.of("99"));
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(user);

            assertThrows(BusinessException.class, () -> newsService.myDetail(101L));
        }
    }

    @Test
    @DisplayName("我的公告详情：已发布且可见时返回")
    void should_return_when_my_detail_visible() {
        draftNews.setStatus(SysNewsStatusEnum.PUBLISHED);
        when(newsManager.getDtoById(100L)).thenReturn(draftNews);

        UserContext user = mockLoginUser(1L, Set.of());
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(user);

            assertEquals(100L, newsService.myDetail(100L).getId());
        }
    }

    @Test
    @DisplayName("标记已读：写入当前用户与公告ID")
    void should_mark_read_with_current_user() {
        draftNews.setStatus(SysNewsStatusEnum.PUBLISHED);
        when(newsManager.getDtoById(100L)).thenReturn(draftNews);

        UserContext user = mockLoginUser(7L, Set.of());
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUser).thenReturn(user);

            newsService.read(100L);

            ArgumentCaptor<SysNewsRead> captor = ArgumentCaptor.forClass(SysNewsRead.class);
            verify(newsReadManager).markRead(captor.capture());
            assertEquals(100L, captor.getValue().getNewsId());
            assertEquals(7L, captor.getValue().getUserId());
        }
    }
}
