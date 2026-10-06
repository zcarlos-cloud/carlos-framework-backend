package com.carlos.org.service;


import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.org.config.DbPermissionProvider;
import com.carlos.org.exception.*;
import com.carlos.org.manager.OrgRoleManager;
import com.carlos.org.pojo.dto.OrgRoleDTO;
import com.carlos.org.pojo.dto.OrgRoleUserDTO;
import com.carlos.org.pojo.enums.OrgDataScopeEnum;
import com.carlos.org.pojo.enums.OrgRoleStateEnum;
import com.carlos.org.pojo.enums.OrgRoleTypeEnum;
import com.carlos.org.pojo.param.OrgRoleAssignPermissionParam;
import com.carlos.org.pojo.param.OrgRoleCopyParam;
import com.carlos.org.pojo.param.OrgRolePageParam;
import com.carlos.org.pojo.param.OrgRoleSetDataScopeParam;
import com.carlos.security.permission.PermissionService;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.Set;


/**
 * <p>
 * 角色 业务
 * </p>
 * <p>实现所有角色管理需求</p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgRoleService {

    private final OrgRoleManager roleManager;


    /**
     * 新增角色
     *
     * @param dto 角色数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void addOrgRole(OrgRoleDTO dto) {
        // 校验角色编码唯一性
        if (StrUtil.isNotBlank(dto.getRoleCode())) {
            OrgRoleDTO existRole = roleManager.getByRoleCode(dto.getRoleCode());
            if (existRole != null) {
                throw new RoleCodeExistsException(dto.getRoleCode());
            }
        }

        // 设置默认类型为自定义角色
        if (dto.getRoleType() == null) {
            dto.setRoleType(OrgRoleTypeEnum.CUSTOM);
        }

        // 设置默认状态为启用
        if (dto.getRoleStatus() == null) {
            dto.setRoleStatus(OrgRoleStateEnum.ENABLE);
        }

        // 设置默认数据范围为仅本人
        if (dto.getDataScope() == null) {
            dto.setDataScope(OrgDataScopeEnum.SELF_ONLY);
        }

        boolean success = roleManager.add(dto);
        if (!success) {
            throw new OrgModuleException("新增角色失败");
        }
        log.info("新增角色成功：id={}, roleName={}", dto.getId(), dto.getRoleName());
    }


    /**
     * 删除角色
     * 删除前校验：角色未被使用
     *
     * @param ids 角色id集合
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrgRole(Set<Long> ids) {
        for (Serializable id : ids) {
            // 校验角色是否存在
            OrgRoleDTO role = roleManager.getDtoById(id);
            if (role == null) {
                throw new RoleNotFoundException(String.valueOf(id));
            }

            // 校验是否为系统角色
            if (role.getRoleType() == OrgRoleTypeEnum.SYSTEM) {
                throw new SystemRoleNotEditableException(String.valueOf(id));
            }

            // 校验角色是否被使用
            Long userCount = roleManager.getUserCountByRoleId(id);
            if (userCount > 0) {
                throw new RoleInUseException(String.valueOf(id));
            }

            // 逻辑删除
            boolean success = roleManager.removeById(id);
            if (!success) {
                log.warn("删除角色失败：id={}", id);
            }
        }
        log.info("删除角色成功：ids={}", ids);
    }


    /**
     * 编辑角色
     *
     * @param dto 对象信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrgRole(OrgRoleDTO dto) {
        // 校验角色是否存在
        OrgRoleDTO existRole = roleManager.getDtoById(dto.getId());
        if (existRole == null) {
            throw new RoleNotFoundException(String.valueOf(dto.getId()));
        }

        // 校验是否为系统角色
        if (existRole.getRoleType() == OrgRoleTypeEnum.SYSTEM) {
            throw new SystemRoleNotEditableException(String.valueOf(dto.getId()));
        }

        boolean success = roleManager.modify(dto);
        if (!success) {
            throw new OrgModuleException("修改角色失败");
        }
        log.info("修改角色成功：id={}", dto.getId());
    }


    /**
     * 启用/禁用角色
     *
     * @param id    角色ID
     * @param state 状态：0禁用，1启用
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeState(Serializable id, Integer state) {
        if (state == null || (state != 0 && state != 1)) {
            throw new OrgModuleException("状态值不正确，只能是0（禁用）或1（启用）");
        }

        // 前端把雪花id作为字符串下发，Jackson 落成 String；统一转 Long 后再走查询，避免 selectById 强转失败
        Long roleId = Convert.toLong(id);
        if (roleId == null) {
            throw new OrgModuleException("角色id格式不正确：" + id);
        }

        OrgRoleDTO role = roleManager.getDtoById(roleId);
        if (role == null) {
            throw new RoleNotFoundException(String.valueOf(id));
        }

        // 校验是否为系统角色
        if (role.getRoleType() == OrgRoleTypeEnum.SYSTEM) {
            throw new SystemRoleNotEditableException(String.valueOf(id));
        }

        role.setRoleStatus(state == 1 ? OrgRoleStateEnum.ENABLE : OrgRoleStateEnum.DISABLE);
        boolean success = roleManager.modify(role);
        if (!success) {
            throw new OrgModuleException("修改角色状态失败");
        }
        log.info("修改角色状态成功：id={}, state={}", id, state);
    }


    /**
     * 配置数据权限
     *
     * @param param 数据权限参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void setDataScope(OrgRoleSetDataScopeParam param) {
        // 校验角色是否存在
        OrgRoleDTO role = roleManager.getDtoById(param.getRoleId());
        if (role == null) {
            throw new RoleNotFoundException(String.valueOf(param.getRoleId()));
        }

        // 校验数据范围值
        OrgDataScopeEnum dataScope = getDataScopeByCode(param.getDataScope());
        if (dataScope == null) {
            throw new OrgModuleException("数据范围值不正确");
        }

        role.setDataScope(dataScope);
        boolean success = roleManager.modify(role);
        if (!success) {
            throw new OrgModuleException("设置数据权限失败");
        }
        log.info("设置数据权限成功：roleId={}, dataScope={}", param.getRoleId(), param.getDataScope());
    }


    /**
     * 根据code获取数据范围枚举
     */
    private OrgDataScopeEnum getDataScopeByCode(Integer code) {
        for (OrgDataScopeEnum scope : OrgDataScopeEnum.values()) {
            if (scope.getCode().equals(code)) {
                return scope;
            }
        }
        return null;
    }


    /**
     * 配置角色操作权限
     *
     * @param param 权限配置参数（operationIds = sys_operation.id 列表）
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(OrgRoleAssignPermissionParam param) {
        // 校验角色是否存在
        OrgRoleDTO role = roleManager.getDtoById(param.getRoleId());
        if (role == null) {
            throw new RoleNotFoundException(String.valueOf(param.getRoleId()));
        }

        boolean success = roleManager.assignOperations(param.getRoleId(), Lists.newArrayList(param.getOperationIds()));
        if (!success) {
            throw new OrgModuleException("配置角色操作权限失败");
        }

        // 写通权限缓存：先按最新的 org_role_operation 解析出权限码，写入 Redis（auth:permissions:{roleCode}）
        // 并广播失效，通知各节点清本地 Caffeine。
        // 此前只广播「失效」却不回写，Redis 里 auth:permissions:{roleCode} 恒为空，
        // 没有 DB 委托的下游服务（如 carlos-system）解析出的权限永远为空 —— 前后端鉴权与菜单过滤随之失效。
        try {
            PermissionService permissionService = SpringUtil.getBean(PermissionService.class);
            DbPermissionProvider dbProvider = SpringUtil.getBean(DbPermissionProvider.class);
            // 权限缓存的 key 是角色编码，不是角色主键
            Set<String> permCodes = dbProvider.getPermissions(role.getRoleCode());
            if (permCodes == null) {
                permCodes = Set.of();
            }
            permissionService.updateRolePermissions(role.getRoleCode(), permCodes, true);
            log.info("角色权限已写通缓存：roleCode={}, permCount={}", role.getRoleCode(), permCodes.size());
        } catch (Exception e) {
            log.warn("写通角色权限缓存失败: roleId={}, error={}", param.getRoleId(), e.getMessage());
        }

        log.info("配置角色操作权限成功：roleId={}, operationCount={}",
                param.getRoleId(), param.getOperationIds().size());
    }


    /**
     * 查看角色用户
     *
     * @param roleId 角色ID
     * @param param  分页参数
     * @return 用户分页列表
     */
    public Paging<OrgRoleUserDTO> getRoleUsers(Serializable roleId, OrgRolePageParam param) {
        // 校验角色是否存在
        OrgRoleDTO role = roleManager.getDtoById(roleId);
        if (role == null) {
            throw new RoleNotFoundException(String.valueOf(roleId));
        }

        return roleManager.getRoleUsers(roleId, param);
    }


    /**
     * 复制角色
     *
     * @param param 复制参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void copyRole(OrgRoleCopyParam param) {
        // 校验源角色是否存在
        OrgRoleDTO sourceRole = roleManager.getDtoById(param.getSourceRoleId());
        if (sourceRole == null) {
            throw new RoleNotFoundException(String.valueOf(param.getSourceRoleId()));
        }

        // 校验新角色编码唯一性
        if (StrUtil.isNotBlank(param.getNewRoleCode())) {
            OrgRoleDTO existRole = roleManager.getByRoleCode(param.getNewRoleCode());
            if (existRole != null) {
                throw new RoleCodeExistsException(param.getNewRoleCode());
            }
        }

        // 创建新角色
        OrgRoleDTO newRole = new OrgRoleDTO();
        newRole.setRoleName(param.getNewRoleName());
        newRole.setRoleCode(param.getNewRoleCode());
        newRole.setRoleType(OrgRoleTypeEnum.CUSTOM); // 复制后的角色为自定义类型
        newRole.setDataScope(sourceRole.getDataScope());
        newRole.setRoleStatus(OrgRoleStateEnum.ENABLE);
        newRole.setDescription(sourceRole.getDescription());

        boolean success = roleManager.add(newRole);
        if (!success) {
            throw new OrgModuleException("复制角色失败");
        }

        // 复制操作权限关联
        List<Long> operationIds = roleManager.getOperationIdsByRoleId(param.getSourceRoleId());
        if (operationIds != null && !operationIds.isEmpty()) {
            roleManager.assignOperations(newRole.getId(),
                    operationIds.stream().map(id -> (Serializable) id).collect(java.util.stream.Collectors.toList()));
        }

        log.info("复制角色成功：sourceRoleId={}, newRoleId={}, newRoleName={}",
                param.getSourceRoleId(), newRole.getId(), param.getNewRoleName());
    }

}
