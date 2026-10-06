package com.carlos.org.manager.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.carlos.org.manager.OrgRoleOperationManager;
import com.carlos.org.mapper.OrgRoleMapper;
import com.carlos.org.mapper.OrgUserRoleMapper;
import com.carlos.org.pojo.dto.OrgRoleDTO;
import com.carlos.org.pojo.entity.OrgRole;
import com.carlos.org.pojo.entity.OrgUserRole;
import com.carlos.org.pojo.enums.OrgDataScopeEnum;
import com.carlos.org.pojo.enums.OrgRoleStateEnum;
import com.carlos.org.pojo.enums.OrgRoleTypeEnum;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrgRoleManagerImpl 单元测试
 *
 * <p>验证：角色操作权限分配和查询切换到 org_role_operation</p>
 */
@ExtendWith(MockitoExtension.class)
class OrgRoleManagerImplTest {

    @Mock
    private OrgRoleMapper orgRoleMapper;

    @Mock
    private OrgRoleOperationManager orgRoleOperationManager;

    @Mock
    private OrgUserRoleMapper orgUserRoleMapper;

    @InjectMocks
    private OrgRoleManagerImpl manager;

    @BeforeAll
    static void initTableInfo() {
        // 纯单元测试环境下初始化实体 TableInfo, 使 LambdaQueryWrapper 能解析列名
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new Configuration(), "");
        TableInfoHelper.initTableInfo(assistant, OrgRole.class);
        TableInfoHelper.initTableInfo(assistant, OrgUserRole.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(manager, "baseMapper", orgRoleMapper);
    }

    // ------------------------- getOperationIdsByRoleId -------------------------

    @Test
    void should_return_empty_when_roleId_null() {
        List<Long> result = manager.getOperationIdsByRoleId(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void should_return_operation_ids_when_role_has_operations() {
        when(orgRoleOperationManager.listOperationIdsByRoleId(100L))
            .thenReturn(List.of(1L, 2L, 3L));

        List<Long> result = manager.getOperationIdsByRoleId(100L);

        assertEquals(3, result.size());
        assertEquals(1L, result.get(0));
        assertEquals(3L, result.get(2));
    }

    @Test
    void should_return_empty_when_role_has_no_operations() {
        when(orgRoleOperationManager.listOperationIdsByRoleId(100L))
            .thenReturn(List.of());

        List<Long> result = manager.getOperationIdsByRoleId(100L);

        assertTrue(result.isEmpty());
    }

    // ------------------------- assignOperations -------------------------

    @Test
    void should_not_throw_when_roleId_null() {
        boolean result = manager.assignOperations(null, List.of());
        assertFalse(result);
        verify(orgRoleOperationManager, never()).saveRoleOperations(anyLong(), any());
    }

    @Test
    void should_save_empty_set_when_operationIds_null() {
        boolean result = manager.assignOperations(100L, null);

        assertTrue(result);
        verify(orgRoleOperationManager).saveRoleOperations(eq(100L), argThat(Set::isEmpty));
    }

    @Test
    void should_save_operations_when_valid() {
        List<Serializable> operationIds = List.of(1L, 2L, 3L);

        boolean result = manager.assignOperations(100L, operationIds);

        assertTrue(result);
        verify(orgRoleOperationManager).saveRoleOperations(
            eq(100L),
            argThat(set -> set.size() == 3 && set.containsAll(Set.of(1L, 2L, 3L)))
        );
    }

    @Test
    void should_replace_existing_operations_on_reassign() {
        // Re-assign role 100 from old ops [1,2] to new ops [5,6]
        manager.assignOperations(100L, List.of(5L, 6L));

        // Verify the save call replaces (via saveRoleOperations which deletes old then inserts new)
        verify(orgRoleOperationManager).saveRoleOperations(eq(100L), argThat(set -> set.containsAll(Set.of(5L, 6L))));
    }

    @Test
    @DisplayName("应该按角色+租户查询用户ID时显式带上 tenant_id 并保留过期授权过滤")
    void should_filter_tenant_id_when_listing_user_ids_by_role_and_tenant() {
        when(orgUserRoleMapper.selectList(any())).thenReturn(List.of());

        List<Long> result = manager.listUserIdsByRoleIdAndTenant(100L, 7L);

        assertTrue(result.isEmpty());
        ArgumentCaptor<Wrapper<OrgUserRole>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(orgUserRoleMapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();

        assertTrue(sqlSegment.contains("role_id ="), "应包含 role_id 条件: " + sqlSegment);
        assertTrue(sqlSegment.contains("tenant_id ="), "必须显式过滤 tenant_id: " + sqlSegment);
        assertTrue(sqlSegment.contains("expire_time IS NULL"), "应保留过期授权过滤: " + sqlSegment);
    }

    @Test
    @DisplayName("应该在不带租户查询用户ID时不追加 tenant_id 条件")
    void should_not_filter_tenant_id_when_role_id_only() {
        when(orgUserRoleMapper.selectList(any())).thenReturn(List.of());

        manager.listUserIdsByRoleId(100L);

        ArgumentCaptor<Wrapper<OrgUserRole>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(orgUserRoleMapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();

        assertTrue(sqlSegment.contains("role_id ="), "应包含 role_id 条件: " + sqlSegment);
        assertFalse(sqlSegment.contains("tenant_id"), "既有方法不应追加 tenant_id 条件: " + sqlSegment);
    }

    // ------------------------- getDtoById -------------------------

    @Test
    void should_return_null_when_role_not_found() {
        when(orgRoleMapper.selectById(any(Serializable.class))).thenReturn(null);

        OrgRoleDTO result = manager.getDtoById(999L);

        assertNull(result);
    }

    @Test
    void should_populate_operationIds_in_dto() {
        OrgRole entity = new OrgRole();
        entity.setId(100L);
        entity.setRoleName("管理员");
        entity.setRoleCode("ADMIN");
        entity.setRoleType(OrgRoleTypeEnum.CUSTOM);
        entity.setDataScope(OrgDataScopeEnum.ALL);
        entity.setRoleStatus(OrgRoleStateEnum.ENABLE);
        when(orgRoleMapper.selectById(100L)).thenReturn(entity);
        when(orgRoleOperationManager.listOperationIdsByRoleId(100L))
            .thenReturn(List.of(1L, 2L));
        when(orgUserRoleMapper.selectCount(any()))
            .thenReturn(5L);

        OrgRoleDTO result = manager.getDtoById(100L);

        assertNotNull(result);
        assertEquals("管理员", result.getRoleName());
        assertNotNull(result.getOperationIds());
        assertEquals(2, result.getOperationIds().size());
        assertEquals(5L, result.getUserCount());
    }

    // ------------------------- getByRoleCode -------------------------

    @Test
    void should_return_dto_when_role_code_exists() {
        OrgRole entity = new OrgRole();
        entity.setId(1L);
        entity.setRoleCode("SUPER_ADMIN");
        entity.setRoleName("超级管理员");
        when(orgRoleMapper.selectOne(any())).thenReturn(entity);

        OrgRoleDTO result = manager.getByRoleCode("SUPER_ADMIN");

        assertNotNull(result);
        assertEquals("SUPER_ADMIN", result.getRoleCode());
    }

    @Test
    void should_return_null_when_role_code_not_found() {
        when(orgRoleMapper.selectOne(any())).thenReturn(null);

        OrgRoleDTO result = manager.getByRoleCode("NONEXISTENT");

        assertNull(result);
    }
}
