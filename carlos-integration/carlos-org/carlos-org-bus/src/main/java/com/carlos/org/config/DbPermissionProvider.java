package com.carlos.org.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.carlos.org.manager.OrgRoleManager;
import com.carlos.org.manager.OrgRoleOperationManager;
import com.carlos.org.mapper.SysOperationPermMapper;
import com.carlos.org.pojo.dto.OrgRoleDTO;
import com.carlos.org.pojo.entity.SysOperationPerm;
import com.carlos.security.auth.UserContext;
import com.carlos.security.permission.PermissionProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 基于数据库的权限提供者实现
 * </p>
 *
 * <p>从 org_role_operation + sys_operation 表查询权限码。</p>
 *
 * <p><b>关于角色标识：</b>{@link PermissionProvider} 的契约是角色<b>编码</b>（org_role.role_code），
 * 而本模块的授权数据 org_role_operation 以角色主键（Long）关联，因此这里在实现内部完成
 * 「编码 → 主键」的解析（{@link OrgRoleManager#getByRoleCode(String)}）。这个翻译只在本类内部出现，
 * 不外泄到接口契约 —— 否则调用方（UserContextAuthenticationFilter 只有 X-User-Roles 头里的编码，
 * 拿不到授权服务器内部的主键）将无从完成翻译。</p>
 *
 * <p><b>超管短路：</b>超级管理员角色（{@link UserContext#DEFAULT_SUPER_ROLE_CODE}）按设计
 * 不在 org_role_operation 中做任何授权记录，其权限集合定义为「sys_operation 中全部启用
 * （perm_status = 1）的权限码」。这与路由侧的超管短路语义一致（超管不按权限过滤、可见全部菜单），
 * 保证前端按钮权限（hasAuth）对超管同样全部放行。</p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DbPermissionProvider implements PermissionProvider {

    private final OrgRoleOperationManager orgRoleOperationManager;
    private final SysOperationPermMapper sysOperationPermMapper;
    private final OrgRoleManager orgRoleManager;

    @Override
    public Set<String> getPermissions(String roleCode) {
        // 超管不做授权，短路返回全部启用权限码
        if (isSuperRoleCode(roleCode)) {
            return queryAllEnabledPermCodes();
        }

        Long roleId = resolveRoleId(roleCode);
        if (roleId == null) {
            return Collections.emptySet();
        }

        List<Long> operationIds = orgRoleOperationManager.listOperationIdsByRoleId(roleId);
        if (operationIds == null || operationIds.isEmpty()) {
            return Collections.emptySet();
        }

        return queryPermCodes(operationIds);
    }

    @Override
    public Set<String> getPermissions(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Collections.emptySet();
        }

        // 任一角色为超管即短路，返回全部启用权限码
        if (roleCodes.stream().anyMatch(this::isSuperRoleCode)) {
            return queryAllEnabledPermCodes();
        }

        Set<Long> roleIds = roleCodes.stream()
            .map(this::resolveRoleId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());

        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }

        List<Long> operationIds = orgRoleOperationManager.listOperationIdsByRoleIds(roleIds);
        if (operationIds == null || operationIds.isEmpty()) {
            return Collections.emptySet();
        }

        return queryPermCodes(operationIds);
    }

    /**
     * 角色编码 → 角色主键
     *
     * @param roleCode 角色编码
     * @return 角色主键，编码为空或角色不存在时返回 null
     */
    private Long resolveRoleId(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return null;
        }

        OrgRoleDTO role = orgRoleManager.getByRoleCode(roleCode);
        if (role == null || role.getId() == null) {
            log.debug("Role not found by code: {}", roleCode);
            return null;
        }

        return role.getId();
    }

    /**
     * 是否为超级管理员角色编码
     *
     * @param roleCode 角色编码
     * @return true 表示超管角色
     */
    private boolean isSuperRoleCode(String roleCode) {
        return UserContext.DEFAULT_SUPER_ROLE_CODE.equals(roleCode);
    }

    /**
     * 查询 sys_operation 全部启用（perm_status = 1）且 perm_code 非空的权限码，去重返回。
     * 仅供超管短路使用。
     *
     * @return 全部启用权限码集合
     */
    private Set<String> queryAllEnabledPermCodes() {
        List<SysOperationPerm> operations = sysOperationPermMapper.selectList(
            Wrappers.<SysOperationPerm>lambdaQuery()
                .eq(SysOperationPerm::getPermStatus, 1)
        );

        return operations.stream()
            .map(SysOperationPerm::getPermCode)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());
    }

    /**
     * 根据操作ID列表查询权限码
     *
     * @param operationIds 操作ID列表
     * @return 权限码集合
     */
    private Set<String> queryPermCodes(List<Long> operationIds) {
        List<SysOperationPerm> operations = sysOperationPermMapper.selectList(
            Wrappers.<SysOperationPerm>lambdaQuery()
                .in(SysOperationPerm::getId, operationIds)
                .eq(SysOperationPerm::getPermStatus, 1)
        );

        return operations.stream()
            .map(SysOperationPerm::getPermCode)
            .filter(permCode -> permCode != null)
            .collect(Collectors.toSet());
    }

    @Override
    public void savePermissions(String roleCode, Set<String> permissions) {
        log.debug("DbPermissionProvider does not support savePermissions directly");
    }

    @Override
    public void savePermissions(String roleCode, Set<String> permissions, long ttl) {
        log.debug("DbPermissionProvider does not support savePermissions directly");
    }

    @Override
    public void clearCache(String roleCode) {
        log.debug("DbPermissionProvider does not have cache to clear");
    }

}
