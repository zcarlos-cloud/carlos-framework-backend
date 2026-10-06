package com.carlos.org.service;

import cn.hutool.extra.spring.SpringUtil;
import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.Result;
import com.carlos.org.encoder.PasswordEncoderProvider;
import com.carlos.org.manager.*;
import com.carlos.org.pojo.dto.*;
import com.carlos.org.pojo.enums.*;
import com.carlos.org.position.manager.OrgPositionCategoryManager;
import com.carlos.org.position.manager.OrgPositionManager;
import com.carlos.org.position.pojo.dto.OrgPositionCategoryDTO;
import com.carlos.org.position.pojo.dto.OrgPositionDTO;
import com.carlos.security.permission.cache.PermissionCacheSyncManager;
import com.carlos.system.api.ApiSysOperation;
import com.carlos.system.pojo.ao.SysOperationAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 组织模块初始化业务
 * </p>
 *
 * <p>负责系统首次部署时初始化超级管理员账号、角色、部门、岗位，并赋予全部权限。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgInitService {

    private static final String LOCK_KEY = "org:lock:init:superadmin";

    private static final String SUPER_ADMIN_ACCOUNT = "admin";
    private static final String SUPER_ADMIN_NICKNAME = "超级管理员";
    private static final String SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN";
    private static final String SUPER_ADMIN_ROLE_NAME = "超级管理员";
    private static final String ROOT_DEPT_CODE = "HQ";
    private static final String ROOT_DEPT_NAME = "总部";
    private static final String SUPER_ADMIN_POSITION_CODE = "SYS_ADMIN";
    private static final String SUPER_ADMIN_POSITION_NAME = "系统管理员";
    private static final String DEFAULT_CATEGORY_CODE = "M";
    private static final String DEFAULT_CATEGORY_NAME = "管理系";

    /** 职系类型：1管理通道 */
    private static final int CATEGORY_TYPE_MANAGEMENT = 1;
    /** 岗位类型：1标准岗 */
    private static final int POSITION_TYPE_STANDARD = 1;
    /** 岗位性质：1全职 */
    private static final int POSITION_NATURE_FULL_TIME = 1;
    /** 岗位状态：1启用 */
    private static final int POSITION_STATUS_ENABLE = 1;

    private final OrgUserManager userManager;
    private final OrgRoleManager roleManager;
    private final OrgDepartmentManager departmentManager;
    private final ApiSysOperation apiSysOperation;
    private final OrgUserRoleManager userRoleManager;
    private final OrgUserDepartmentManager userDepartmentManager;
    private final OrgPositionCategoryManager positionCategoryManager;
    private final OrgPositionManager positionManager;
    private final PasswordEncoderProvider passwordEncoderProvider;
    private final RedissonClient redissonClient;


    /**
     * 初始化超级管理员账号
     *
     * <p>前置条件：系统中不存在任何用户。本操作是原子的，任一环节失败全部回滚。</p>
     * <p>通过 Redisson 分布式锁防止并发重复初始化。</p>
     *
     * @param password 初始密码（已通过参数校验）
     */
    /**
     * 初始化超级管理员账号
     *
     * <p>前置条件：系统中不存在任何用户。本操作是原子的，任一环节失败全部回滚。</p>
     * <p>通过 Redisson 分布式锁防止并发重复初始化；锁在事务外层获取，确保事务生效。</p>
     *
     * @param password 初始密码（已通过参数校验）
     */
    @Transactional(rollbackFor = Exception.class)
    public void initSuperAdmin(String password) {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        boolean locked;
        try {
            locked = lock.tryLock(0, 60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("初始化加锁被中断");
        }
        if (!locked) {
            throw new BusinessException("初始化任务正在执行，请勿重复调用");
        }
        try {
            doInit(password);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    /**
     * 实际初始化逻辑，在分布式锁与事务保护下执行。
     */
    private void doInit(String password) {
        // 1. 仅允许空库初始化（锁内再次校验，防止竞态）
        if (userManager.count() > 0) {
            throw new BusinessException("系统已存在用户，不允许重复初始化");
        }

        // 2. 创建根部门
        OrgDepartmentDTO departmentDto = buildRootDepartment();
        if (!departmentManager.add(departmentDto)) {
            throw new BusinessException("初始化部门失败");
        }
        Long deptId = departmentDto.getId();

        // 3. 创建默认职系（管理系）
        OrgPositionCategoryDTO categoryDto = buildAdminCategory();
        if (!positionCategoryManager.add(categoryDto)) {
            throw new BusinessException("初始化职系失败");
        }
        Long categoryId = categoryDto.getId();

        // 4. 创建超管角色
        OrgRoleDTO roleDto = buildSuperAdminRole();
        if (!roleManager.add(roleDto)) {
            throw new BusinessException("初始化角色失败");
        }
        Long roleId = roleDto.getId();

        // 5. 创建岗位
        OrgPositionDTO positionDto = buildAdminPosition(deptId, categoryId);
        if (!positionManager.add(positionDto)) {
            throw new BusinessException("初始化岗位失败");
        }
        Long positionId = positionDto.getId();

        // 6. 创建超管用户
        OrgUserDTO userDto = buildSuperAdminUser(deptId, password);
        if (!userManager.add(userDto)) {
            throw new BusinessException("初始化用户失败");
        }
        Long userId = userDto.getId();

        // 7. 绑定用户-主部门
        OrgUserDepartmentDTO userDeptDto = new OrgUserDepartmentDTO();
        userDeptDto.setUserId(userId);
        userDeptDto.setDeptId(deptId);
        userDeptDto.setMainDept(true);
        if (!userDepartmentManager.add(userDeptDto)) {
            throw new BusinessException("初始化用户部门关联失败");
        }

        // 8. 绑定用户-角色
        OrgUserRoleDTO userRoleDto = new OrgUserRoleDTO();
        userRoleDto.setUserId(userId);
        userRoleDto.setRoleId(roleId);
        if (!userRoleManager.add(userRoleDto)) {
            throw new BusinessException("初始化用户角色关联失败");
        }

        // 9. 赋予角色全部操作权限（通过 carlos-system Feign API 获取）
        Result<List<SysOperationAO>> result = apiSysOperation.listEnabledOperations();
        List<SysOperationAO> allOperations = (result != null && result.isSuccess()) ? result.getData() : null;
        if (allOperations == null || allOperations.isEmpty()) {
            log.warn("系统中未配置任何操作权限，超管角色将不包含权限");
        } else {
            List<Serializable> operationIds = allOperations.stream()
                    .map(SysOperationAO::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (!roleManager.assignOperations(roleId, operationIds)) {
                throw new BusinessException("初始化角色操作权限失败");
            }
        }

        // 10. 发布缓存失效事件
        try {
            PermissionCacheSyncManager cacheSyncManager = SpringUtil.getBean(PermissionCacheSyncManager.class);
            if (cacheSyncManager != null) {
                // 权限缓存以角色编码为 key，不是角色主键
                cacheSyncManager.publishPermissionChange(roleDto.getRoleCode());
            }
        } catch (Exception e) {
            log.warn("初始化时发布权限缓存失效事件失败: roleCode={}, error={}", roleDto.getRoleCode(), e.getMessage());
        }

        log.info("超级管理员初始化完成：roleId={}, deptId={}, positionId={}",
                roleId, deptId, positionId);
    }


    private OrgDepartmentDTO buildRootDepartment() {
        OrgDepartmentDTO dto = new OrgDepartmentDTO();
        dto.setDeptName(ROOT_DEPT_NAME);
        dto.setDeptCode(ROOT_DEPT_CODE);
        dto.setParentId(0L);
        dto.setPath("/" + ROOT_DEPT_CODE);
        dto.setLevel(1);
        dto.setOrderNum(1);
        dto.setDeptStatus(OrgDepartmentStateEnum.ENABLE);
        dto.setDescription("根部门");
        dto.setCreateBy(1L);
        dto.setCreateTime(LocalDateTime.now());
        return dto;
    }

    private OrgRoleDTO buildSuperAdminRole() {
        OrgRoleDTO dto = new OrgRoleDTO();
        dto.setRoleName(SUPER_ADMIN_ROLE_NAME);
        dto.setRoleCode(SUPER_ADMIN_ROLE_CODE);
        dto.setRoleType(OrgRoleTypeEnum.SYSTEM);
        dto.setDataScope(OrgDataScopeEnum.ALL);
        dto.setRoleStatus(OrgRoleStateEnum.ENABLE);
        dto.setCreateBy(1L);
        dto.setCreateTime(LocalDateTime.now());
        return dto;
    }

    private OrgPositionCategoryDTO buildAdminCategory() {
        OrgPositionCategoryDTO dto = new OrgPositionCategoryDTO();
        dto.setCategoryCode(DEFAULT_CATEGORY_CODE);
        dto.setCategoryName(DEFAULT_CATEGORY_NAME);
        dto.setCategoryType(CATEGORY_TYPE_MANAGEMENT);
        dto.setOrderNum(1);
        dto.setPositionCategoryStatus(1);
        dto.setCreateBy(1L);
        dto.setCreateTime(LocalDateTime.now());
        return dto;
    }

    private OrgPositionDTO buildAdminPosition(Long deptId, Long categoryId) {
        OrgPositionDTO dto = new OrgPositionDTO();
        dto.setPositionName(SUPER_ADMIN_POSITION_NAME);
        dto.setPositionCode(SUPER_ADMIN_POSITION_CODE);
        dto.setCategoryId(categoryId);
        dto.setPositionType(POSITION_TYPE_STANDARD);
        dto.setPositionNature(POSITION_NATURE_FULL_TIME);
        dto.setDepartmentId(deptId);
        dto.setPositionStatus(POSITION_STATUS_ENABLE);
        dto.setCreateBy(1L);
        dto.setCreateTime(LocalDateTime.now());
        return dto;
    }

    private OrgUserDTO buildSuperAdminUser(Long deptId, String rawPassword) {
        OrgUserDTO dto = new OrgUserDTO();
        dto.setAccount(SUPER_ADMIN_ACCOUNT);
        dto.setNickname(SUPER_ADMIN_NICKNAME);
        dto.setPwd(passwordEncoderProvider.encode(rawPassword));
        dto.setUserStatus(OrgUserStateEnum.ENABLE);
        dto.setMainDeptId(deptId);
        dto.setCreateBy(1L);
        dto.setCreateTime(LocalDateTime.now());
        return dto;
    }

}
