package com.carlos.org.manager.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.carlos.org.config.OrgDataScopeHelper;
import com.carlos.org.manager.OrgDepartmentManager;
import com.carlos.org.mapper.OrgDepartmentMapper;
import com.carlos.org.mapper.OrgUserMapper;
import com.carlos.org.pojo.entity.OrgUser;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * OrgUserManagerImpl 显式租户查询单元测试
 *
 * <p>无 {@code HttpServletRequest} 上下文（后台任务、系统间调用）时租户无法经
 * {@code FeignRequestInterceptor} 透传，{@code OrgTenantLineHandler} 会静默回落到平台租户。
 * 因此 {@code ...AndTenant} 系列必须绕过租户拦截器后在 wrapper 中**显式**带上
 * {@code tenant_id}，本测试钉死该行为。</p>
 *
 * @author Carlos
 */
class OrgUserManagerImplTest {

    @BeforeAll
    static void initTableInfo() {
        // 纯单元测试环境下初始化实体 TableInfo, 使 LambdaQueryWrapper 能解析列名
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new Configuration(), "");
        TableInfoHelper.initTableInfo(assistant, OrgUser.class);
    }

    private OrgUserManagerImpl newManager(OrgUserMapper mapper) {
        OrgUserManagerImpl manager = new OrgUserManagerImpl(mock(OrgDataScopeHelper.class),
            mock(OrgDepartmentMapper.class), mock(OrgDepartmentManager.class));
        ReflectionTestUtils.setField(manager, "baseMapper", mapper);
        return manager;
    }

    @Test
    @DisplayName("按用户ID+租户查询时 wrapper 必须显式带上 tenant_id")
    void should_filter_tenant_id_when_getting_by_id_and_tenant() {
        OrgUserMapper mapper = mock(OrgUserMapper.class);
        when(mapper.selectOne(any(), eq(true))).thenReturn(null);
        OrgUserManagerImpl manager = newManager(mapper);

        assertNull(manager.getDtoByIdAndTenant(1001L, 7L));

        ArgumentCaptor<Wrapper<OrgUser>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectOne(captor.capture(), eq(true));
        String sqlSegment = captor.getValue().getSqlSegment();

        assertTrue(sqlSegment.contains("id ="), "应包含 id 条件: " + sqlSegment);
        assertTrue(sqlSegment.contains("tenant_id ="), "必须显式过滤 tenant_id: " + sqlSegment);
    }

    @Test
    @DisplayName("按用户ID集合+租户批量查询时 wrapper 必须显式带上 tenant_id")
    void should_filter_tenant_id_when_listing_by_ids_and_tenant() {
        OrgUserMapper mapper = mock(OrgUserMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        OrgUserManagerImpl manager = newManager(mapper);

        assertTrue(manager.listDtoByIdsAndTenant(List.of(1001L, 1002L), 7L).isEmpty());

        ArgumentCaptor<Wrapper<OrgUser>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();

        assertTrue(sqlSegment.contains("id IN"), "应包含 id IN 条件: " + sqlSegment);
        assertTrue(sqlSegment.contains("tenant_id ="), "必须显式过滤 tenant_id: " + sqlSegment);
    }

    @Test
    @DisplayName("空 ID 集合或空 tenantId 不应产生数据库查询")
    void should_not_hit_db_when_ids_empty_or_tenant_null() {
        OrgUserMapper mapper = mock(OrgUserMapper.class);
        OrgUserManagerImpl manager = newManager(mapper);

        assertTrue(manager.listDtoByIdsAndTenant(List.of(), 7L).isEmpty());
        assertTrue(manager.listDtoByIdsAndTenant(List.of(1001L), null).isEmpty());
        assertNull(manager.getDtoByIdAndTenant(null, 7L));
        assertNull(manager.getDtoByIdAndTenant(1001L, null));

        verifyNoInteractions(mapper);
    }
}
