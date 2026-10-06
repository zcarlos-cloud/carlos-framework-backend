package com.carlos.org.manager.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.carlos.org.mapper.OrgUserRoleMapper;
import com.carlos.org.pojo.entity.OrgUserRole;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrgUserRoleManagerImpl 单元测试
 *
 * <p>ADR-002: 生效角色判定必须过滤过期授权(expire_time 为空或晚于当前时间),
 * 否则过期角色仍会进入 JWT(R-2)。</p>
 *
 * @author Carlos
 */
class OrgUserRoleManagerImplTest {

    @BeforeAll
    static void initTableInfo() {
        // 纯单元测试环境下初始化实体 TableInfo, 使 LambdaQueryWrapper 能解析列名
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new Configuration(), "");
        TableInfoHelper.initTableInfo(assistant, OrgUserRole.class);
    }

    @Test
    @DisplayName("应该在按用户查询生效角色时过滤已过期的授权")
    void should_filter_expired_roles_when_listing_by_user_id() {
        OrgUserRoleMapper mapper = mock(OrgUserRoleMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());

        OrgUserRoleManagerImpl manager = new OrgUserRoleManagerImpl();
        ReflectionTestUtils.setField(manager, "baseMapper", mapper);

        manager.listByUserId(1001L);

        ArgumentCaptor<Wrapper<OrgUserRole>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();

        assertTrue(sqlSegment.contains("user_id ="), "应包含 user_id 条件: " + sqlSegment);
        assertTrue(sqlSegment.contains("expire_time IS NULL"), "应包含 expire_time IS NULL 条件: " + sqlSegment);
        assertTrue(sqlSegment.contains("expire_time >"), "应包含 expire_time > 当前时间条件: " + sqlSegment);
        // 钉死括号化语义: user_id = ? AND (expire_time IS NULL OR expire_time > ?)
        // 若误写成顶层 or(优先级错误)则该断言失败
        assertTrue(sqlSegment.contains("(expire_time IS NULL OR expire_time >"),
            "expire_time 两个条件必须被括号包裹: " + sqlSegment);
    }

    @Test
    @DisplayName("管理面查询全部授权时不应过滤过期时间")
    void should_not_filter_expire_time_when_listing_all_by_user_id() {
        OrgUserRoleMapper mapper = mock(OrgUserRoleMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());

        OrgUserRoleManagerImpl manager = new OrgUserRoleManagerImpl();
        ReflectionTestUtils.setField(manager, "baseMapper", mapper);

        manager.listAllByUserId(1001L);

        ArgumentCaptor<Wrapper<OrgUserRole>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();

        assertTrue(sqlSegment.contains("user_id ="), "应包含 user_id 条件: " + sqlSegment);
        assertFalse(sqlSegment.contains("expire_time"), "管理面查询不应过滤 expire_time: " + sqlSegment);
    }
}
