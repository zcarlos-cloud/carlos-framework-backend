package com.carlos.org.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OrgTenantLineHandler 租户拦截清单单元测试
 *
 * <p>规则:只有真实存在 tenant_id 列的表才应被拦截,
 * 否则拦截器会向无该列的表拼接 tenant_id 条件导致 SQL 报错(R-1)。</p>
 *
 * @author Carlos
 */
class OrgTenantLineHandlerTest {

    private final OrgTenantLineHandler handler = new OrgTenantLineHandler(new OrgProperties());

    @Test
    @DisplayName("应该忽略没有 tenant_id 列的 org_user_department 表")
    void should_ignore_user_department_table_without_tenant_column() {
        assertTrue(handler.ignoreTable("org_user_department"));
    }

    @Test
    @DisplayName("应该忽略自身无 tenant_id 列的 org_tenant 表")
    void should_ignore_org_tenant_table_without_tenant_column() {
        assertTrue(handler.ignoreTable("org_tenant"));
    }

    @Test
    @DisplayName("应该拦截有 tenant_id 列的职位体系四张表")
    void should_not_ignore_position_tables_with_tenant_column() {
        assertFalse(handler.ignoreTable("org_position_category"));
        assertFalse(handler.ignoreTable("org_position_level"));
        assertFalse(handler.ignoreTable("org_position_role"));
        assertFalse(handler.ignoreTable("org_position_history"));
    }

    @Test
    @DisplayName("应该继续拦截核心租户表")
    void should_keep_intercepting_core_tenant_tables() {
        assertFalse(handler.ignoreTable("org_user"));
        assertFalse(handler.ignoreTable("org_department"));
        assertFalse(handler.ignoreTable("org_role"));
        assertFalse(handler.ignoreTable("org_position"));
        assertFalse(handler.ignoreTable("org_user_role"));
        assertFalse(handler.ignoreTable("org_user_position"));
        assertFalse(handler.ignoreTable("org_department_role"));
        assertFalse(handler.ignoreTable("org_role_operation"));
    }

    @Test
    @DisplayName("应该忽略没有 tenant_id 列的表与无关表")
    void should_ignore_tables_without_tenant_column_and_unrelated() {
        // org_docking_mapping / org_user_import 均无 tenant_id 列
        assertTrue(handler.ignoreTable("org_docking_mapping"));
        assertTrue(handler.ignoreTable("org_user_import"));
        assertTrue(handler.ignoreTable("sys_menu"));
    }
}
