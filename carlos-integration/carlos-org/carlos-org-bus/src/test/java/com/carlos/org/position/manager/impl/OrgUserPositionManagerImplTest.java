package com.carlos.org.position.manager.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.carlos.org.position.mapper.OrgUserPositionMapper;
import com.carlos.org.position.pojo.entity.OrgUserPosition;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * OrgUserPositionManagerImpl 断言测试
 *
 * <p>「超时升级上级」由 carlos-job 调度线程发起时没有 {@code HttpServletRequest}，
 * 租户条件会静默回落到平台租户；因此 {@code ...AndTenant} 系列必须绕过租户拦截器
 * 后在 wrapper 中**显式**带上 {@code tenant_id}。</p>
 *
 * <p>同时覆盖两个新增查询：按 {@code user_id} 反查汇报对象（取直属上级）与按
 * {@code position_id} 查用户ID。两者的任职状态口径都与 {@code listByReportTo} 一致
 * ——**不过滤** {@code position_status}。</p>
 *
 * @author Carlos
 */
class OrgUserPositionManagerImplTest {

    @BeforeAll
    static void initTableInfo() {
        // 纯单元测试环境下初始化实体 TableInfo, 使 LambdaQueryWrapper 能解析列名
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new Configuration(), "");
        TableInfoHelper.initTableInfo(assistant, OrgUserPosition.class);
    }

    private OrgUserPositionManagerImpl newManager(OrgUserPositionMapper mapper) {
        OrgUserPositionManagerImpl manager = new OrgUserPositionManagerImpl();
        ReflectionTestUtils.setField(manager, "baseMapper", mapper);
        return manager;
    }

    @Test
    @DisplayName("按汇报对象+租户查询任职记录时 wrapper 必须显式带上 tenant_id")
    void should_filter_tenant_id_when_listing_by_report_to_and_tenant() {
        OrgUserPositionMapper mapper = mock(OrgUserPositionMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        OrgUserPositionManagerImpl manager = newManager(mapper);

        assertTrue(manager.listByReportToAndTenant(1001L, 7L).isEmpty());

        ArgumentCaptor<Wrapper<OrgUserPosition>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();

        assertTrue(sqlSegment.contains("report_to ="), "应包含 report_to 条件: " + sqlSegment);
        assertTrue(sqlSegment.contains("tenant_id ="), "必须显式过滤 tenant_id: " + sqlSegment);
    }

    @Test
    @DisplayName("空 reportTo 或空 tenantId 不应产生数据库查询")
    void should_not_hit_db_when_report_to_or_tenant_null() {
        OrgUserPositionMapper mapper = mock(OrgUserPositionMapper.class);
        OrgUserPositionManagerImpl manager = newManager(mapper);

        assertTrue(manager.listByReportToAndTenant(null, 7L).isEmpty());
        assertTrue(manager.listByReportToAndTenant(1001L, null).isEmpty());

        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("查某人的直属上级：读的是他自己的 report_to，优先主岗位记录")
    void should_pick_main_position_report_to_as_superior() {
        OrgUserPositionMapper mapper = mock(OrgUserPositionMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
            new OrgUserPosition().setUserId(1001L).setMain(false).setReportTo(2001L),
            new OrgUserPosition().setUserId(1001L).setMain(true).setReportTo(2002L)));
        OrgUserPositionManagerImpl manager = newManager(mapper);

        assertEquals(2002L, manager.getReportToByUserId(1001L));

        ArgumentCaptor<Wrapper<OrgUserPosition>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("user_id ="), "条件必须是该用户自己的任职记录: " + sqlSegment);
        assertTrue(sqlSegment.contains("report_to IS NOT NULL"), "空汇报对象不应参与取值: " + sqlSegment);
    }

    @Test
    @DisplayName("查某人的直属上级：主岗位未配置汇报对象时回退为其它已配置的记录")
    void should_fall_back_to_any_position_when_main_has_no_report_to() {
        OrgUserPositionMapper mapper = mock(OrgUserPositionMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
            new OrgUserPosition().setUserId(1001L).setMain(false).setReportTo(2001L)));
        OrgUserPositionManagerImpl manager = newManager(mapper);

        assertEquals(2001L, manager.getReportToByUserId(1001L));
    }

    @Test
    @DisplayName("查某人的直属上级：无任职记录时返回 null（不是错误）")
    void should_return_null_when_no_position_record() {
        OrgUserPositionMapper mapper = mock(OrgUserPositionMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        OrgUserPositionManagerImpl manager = newManager(mapper);

        assertNull(manager.getReportToByUserId(1001L));
        verify(mapper).selectList(any());
    }

    @Test
    @DisplayName("空 userId 或空 tenantId 时查上级不应产生数据库查询")
    void should_not_hit_db_when_superior_lookup_argument_null() {
        OrgUserPositionMapper mapper = mock(OrgUserPositionMapper.class);
        OrgUserPositionManagerImpl manager = newManager(mapper);

        assertNull(manager.getReportToByUserId(null));
        assertNull(manager.getReportToByUserIdAndTenant(null, 7L));
        assertNull(manager.getReportToByUserIdAndTenant(1001L, null));

        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("查上级 + 租户：wrapper 必须同时带上 user_id 与 tenant_id")
    void should_filter_tenant_id_when_looking_up_superior() {
        OrgUserPositionMapper mapper = mock(OrgUserPositionMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
            new OrgUserPosition().setUserId(1001L).setMain(true).setReportTo(2002L)));
        OrgUserPositionManagerImpl manager = newManager(mapper);

        assertEquals(2002L, manager.getReportToByUserIdAndTenant(1001L, 7L));

        ArgumentCaptor<Wrapper<OrgUserPosition>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("user_id ="), "应包含 user_id 条件: " + sqlSegment);
        assertTrue(sqlSegment.contains("tenant_id ="), "必须显式过滤 tenant_id: " + sqlSegment);
    }

    @Test
    @DisplayName("按岗位查用户：只过滤 position_id，刻意不过滤 position_status，并按 user_id 去重")
    void should_list_user_ids_by_position_without_status_filter() {
        OrgUserPositionMapper mapper = mock(OrgUserPositionMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
            new OrgUserPosition().setUserId(1001L).setPositionId(3003L).setPositionStatus(1),
            new OrgUserPosition().setUserId(1001L).setPositionId(3003L).setPositionStatus(2),
            new OrgUserPosition().setUserId(1002L).setPositionId(3003L).setPositionStatus(5)));
        OrgUserPositionManagerImpl manager = newManager(mapper);

        assertEquals(List.of(1001L, 1002L), manager.listUserIdsByPositionId(3003L));

        ArgumentCaptor<Wrapper<OrgUserPosition>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("position_id ="), "应包含 position_id 条件: " + sqlSegment);
        assertTrue(!sqlSegment.contains("position_status"),
            "刻意与 listByReportTo 口径一致：不过滤任职状态，否则会漏掉试用期（2）的人: " + sqlSegment);
    }

    @Test
    @DisplayName("按岗位查用户 + 租户：wrapper 必须同时带上 position_id 与 tenant_id")
    void should_filter_tenant_id_when_listing_user_ids_by_position() {
        OrgUserPositionMapper mapper = mock(OrgUserPositionMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        OrgUserPositionManagerImpl manager = newManager(mapper);

        assertTrue(manager.listUserIdsByPositionIdAndTenant(3003L, 7L).isEmpty());

        ArgumentCaptor<Wrapper<OrgUserPosition>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("position_id ="), "应包含 position_id 条件: " + sqlSegment);
        assertTrue(sqlSegment.contains("tenant_id ="), "必须显式过滤 tenant_id: " + sqlSegment);
    }

    @Test
    @DisplayName("按岗位查用户的空入参不应产生数据库查询")
    void should_not_hit_db_when_position_lookup_argument_null() {
        OrgUserPositionMapper mapper = mock(OrgUserPositionMapper.class);
        OrgUserPositionManagerImpl manager = newManager(mapper);

        assertTrue(manager.listUserIdsByPositionId(null).isEmpty());
        assertTrue(manager.listUserIdsByPositionIdAndTenant(null, 7L).isEmpty());
        assertTrue(manager.listUserIdsByPositionIdAndTenant(3003L, null).isEmpty());

        verifyNoInteractions(mapper);
    }
}
