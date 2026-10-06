package com.carlos.org.manager.impl;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.carlos.org.config.OrgDataScopeHelper;
import com.carlos.org.mapper.OrgDepartmentMapper;
import com.carlos.org.mapper.OrgUserDepartmentMapper;
import com.carlos.org.mapper.OrgUserMapper;
import com.carlos.org.pojo.entity.OrgDepartment;
import com.carlos.org.pojo.entity.OrgUser;
import com.carlos.org.pojo.entity.OrgUserDepartment;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * OrgDepartmentManagerImpl 显式租户查询单元测试
 *
 * <p>{@code org_user_department} 表自身没有 {@code tenant_id} 列，租户维度只能落在
 * {@code org_user} 上：先按部门取直属用户ID，再用绕过租户拦截器的 wrapper
 * **显式**过滤 {@code tenant_id} 批量取用户。</p>
 *
 * <p>另覆盖递归查询（{@link OrgDepartmentManagerImpl#getUsersByDeptIdRecursive}）：
 * 部门集合必须在服务端展开，且只产生**一次**用户归属查询。</p>
 *
 * @author Carlos
 */
class OrgDepartmentManagerImplTest {

    @BeforeAll
    static void initTableInfo() {
        // 纯单元测试环境下初始化实体 TableInfo, 使 LambdaQueryWrapper 能解析列名
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new Configuration(), "");
        TableInfoHelper.initTableInfo(assistant, OrgUser.class);
        TableInfoHelper.initTableInfo(assistant, OrgUserDepartment.class);
        TableInfoHelper.initTableInfo(assistant, OrgDepartment.class);
    }

    private OrgDepartmentManagerImpl newManager(OrgUserDepartmentMapper deptUserMapper, OrgUserMapper userMapper) {
        return new OrgDepartmentManagerImpl(mock(OrgDataScopeHelper.class), deptUserMapper, userMapper);
    }

    private OrgDepartmentManagerImpl newRecursiveManager(OrgUserDepartmentMapper deptUserMapper, OrgUserMapper userMapper,
                                                         OrgDepartmentMapper deptMapper) {
        OrgDepartmentManagerImpl manager = newManager(deptUserMapper, userMapper);
        ReflectionTestUtils.setField(manager, "baseMapper", deptMapper);
        return manager;
    }

    @Test
    @DisplayName("按部门+租户查询用户时 org_user 的 wrapper 必须显式带上 tenant_id")
    void should_filter_tenant_id_when_listing_users_by_dept_and_tenant() {
        OrgUserDepartmentMapper deptUserMapper = mock(OrgUserDepartmentMapper.class);
        OrgUserMapper userMapper = mock(OrgUserMapper.class);
        OrgUserDepartment relation = new OrgUserDepartment().setUserId(1001L).setDeptId(2002L);
        when(deptUserMapper.selectList(any())).thenReturn(List.of(relation, relation));
        when(userMapper.selectList(any())).thenReturn(List.of());
        OrgDepartmentManagerImpl manager = newManager(deptUserMapper, userMapper);

        assertTrue(manager.getUsersByDeptIdAndTenant(2002L, 7L).isEmpty());

        ArgumentCaptor<Wrapper<OrgUser>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(userMapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();

        assertTrue(sqlSegment.contains("id IN"), "应包含 id IN 条件: " + sqlSegment);
        assertTrue(sqlSegment.contains("tenant_id ="), "必须显式过滤 tenant_id: " + sqlSegment);
    }

    @Test
    @DisplayName("部门下没有直属用户时不应再查询 org_user")
    void should_not_query_users_when_dept_has_no_relation() {
        OrgUserDepartmentMapper deptUserMapper = mock(OrgUserDepartmentMapper.class);
        OrgUserMapper userMapper = mock(OrgUserMapper.class);
        when(deptUserMapper.selectList(any())).thenReturn(List.of());
        OrgDepartmentManagerImpl manager = newManager(deptUserMapper, userMapper);

        assertTrue(manager.getUsersByDeptIdAndTenant(2002L, 7L).isEmpty());

        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("空 deptId 或空 tenantId 不应产生数据库查询")
    void should_not_hit_db_when_dept_id_or_tenant_null() {
        OrgUserDepartmentMapper deptUserMapper = mock(OrgUserDepartmentMapper.class);
        OrgUserMapper userMapper = mock(OrgUserMapper.class);
        OrgDepartmentManagerImpl manager = newManager(deptUserMapper, userMapper);

        assertTrue(manager.getUsersByDeptIdAndTenant(null, 7L).isEmpty());
        assertTrue(manager.getUsersByDeptIdAndTenant(2002L, null).isEmpty());

        verifyNoInteractions(deptUserMapper);
        verifyNoInteractions(userMapper);
    }

    // ------------------------- 递归查询（dept:{id}:rec） -------------------------

    @Test
    @DisplayName("递归查询：一次 org_user_department 查询就要覆盖自身与全部子孙部门")
    void should_expand_descendant_depts_into_a_single_relation_query() {
        OrgUserDepartmentMapper deptUserMapper = mock(OrgUserDepartmentMapper.class);
        OrgUserMapper userMapper = mock(OrgUserMapper.class);
        OrgDepartmentMapper deptMapper = mock(OrgDepartmentMapper.class);
        // 单测没有 Redis：子孙缓存未命中且部门本体不在缓存里 → 逐层下探数据库
        when(deptMapper.selectList(any()))
            .thenReturn(List.of(new OrgDepartment().setId(2003L).setParentId(2002L)))
            .thenReturn(List.of());
        when(deptUserMapper.selectList(any()))
            .thenReturn(List.of(new OrgUserDepartment().setUserId(1001L).setDeptId(2003L)));
        when(userMapper.selectList(any())).thenReturn(List.of());
        OrgDepartmentManagerImpl manager = newRecursiveManager(deptUserMapper, userMapper, deptMapper);

        assertTrue(manager.getUsersByDeptIdRecursive(2002L).isEmpty());

        ArgumentCaptor<Wrapper<OrgUserDepartment>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(deptUserMapper, times(1)).selectList(captor.capture());
        Wrapper<OrgUserDepartment> wrapper = captor.getValue();
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("dept_id IN"), "应包含 dept_id IN 条件: " + sqlSegment);

        // 绑定值必须同时包含子树根与其子孙部门：一次查询就覆盖整棵子树
        Map<String, Object> boundParams = ((AbstractWrapper<?, ?, ?>) wrapper).getParamNameValuePairs();
        Set<Object> boundDeptIds = new LinkedHashSet<>(boundParams.values());
        assertEquals(Set.of(2002L, 2003L), boundDeptIds,
            "子树里的部门必须一次查询覆盖，不能为每个子部门各查一次: " + boundDeptIds);
    }

    @Test
    @DisplayName("递归查询：子树下无用户时不应再查询 org_user")
    void should_not_query_users_when_no_member_in_dept_subtree() {
        OrgUserDepartmentMapper deptUserMapper = mock(OrgUserDepartmentMapper.class);
        OrgUserMapper userMapper = mock(OrgUserMapper.class);
        OrgDepartmentMapper deptMapper = mock(OrgDepartmentMapper.class);
        when(deptMapper.selectList(any())).thenReturn(List.of());
        when(deptUserMapper.selectList(any())).thenReturn(List.of());
        OrgDepartmentManagerImpl manager = newRecursiveManager(deptUserMapper, userMapper, deptMapper);

        assertTrue(manager.getUsersByDeptIdRecursive(2002L).isEmpty());

        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("递归查询 + 租户：org_user 的 wrapper 必须显式带上 tenant_id")
    void should_filter_tenant_id_when_listing_users_by_dept_subtree_and_tenant() {
        OrgUserDepartmentMapper deptUserMapper = mock(OrgUserDepartmentMapper.class);
        OrgUserMapper userMapper = mock(OrgUserMapper.class);
        OrgDepartmentMapper deptMapper = mock(OrgDepartmentMapper.class);
        when(deptMapper.selectList(any())).thenReturn(List.of());
        when(deptUserMapper.selectList(any()))
            .thenReturn(List.of(new OrgUserDepartment().setUserId(1001L).setDeptId(2002L)));
        when(userMapper.selectList(any())).thenReturn(List.of());
        OrgDepartmentManagerImpl manager = newRecursiveManager(deptUserMapper, userMapper, deptMapper);

        assertTrue(manager.getUsersByDeptIdRecursiveAndTenant(2002L, 7L).isEmpty());

        ArgumentCaptor<Wrapper<OrgUser>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(userMapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("id IN"), "应包含 id IN 条件: " + sqlSegment);
        assertTrue(sqlSegment.contains("tenant_id ="), "必须显式过滤 tenant_id: " + sqlSegment);
    }

    @Test
    @DisplayName("递归查询：空 deptId 或空 tenantId 不应产生数据库查询")
    void should_not_hit_db_when_recursive_dept_id_or_tenant_null() {
        OrgUserDepartmentMapper deptUserMapper = mock(OrgUserDepartmentMapper.class);
        OrgUserMapper userMapper = mock(OrgUserMapper.class);
        OrgDepartmentMapper deptMapper = mock(OrgDepartmentMapper.class);
        OrgDepartmentManagerImpl manager = newRecursiveManager(deptUserMapper, userMapper, deptMapper);

        assertTrue(manager.getUsersByDeptIdRecursive(null).isEmpty());
        assertTrue(manager.getUsersByDeptIdRecursiveAndTenant(2002L, null).isEmpty());
        assertTrue(manager.getUsersByDeptIdRecursiveAndTenant(null, 7L).isEmpty());

        verifyNoInteractions(deptUserMapper);
        verifyNoInteractions(userMapper);
        verifyNoInteractions(deptMapper);
    }
}
