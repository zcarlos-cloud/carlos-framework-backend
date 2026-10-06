package com.carlos.org.service;


import cn.hutool.core.util.StrUtil;
import com.carlos.org.api.enums.OrgErrorCode;
import com.carlos.org.config.OrgProperties;
import com.carlos.org.encoder.PasswordEncoderProvider;
import com.carlos.org.manager.OrgTenantManager;
import com.carlos.org.manager.OrgUserManager;
import com.carlos.org.pojo.dto.OrgTenantDTO;
import com.carlos.org.pojo.dto.OrgUserDTO;
import com.carlos.org.pojo.enums.OrgTenantStateEnum;
import com.carlos.org.pojo.enums.OrgUserStateEnum;
import com.carlos.org.pojo.param.OrgTenantInitAdminParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;


/**
 * <p>
 * 租户 业务
 * </p>
 * <p>实现租户管理与租户管理员初始化</p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgTenantService {

    private final OrgTenantManager tenantManager;

    private final OrgUserManager userManager;

    private final OrgProperties orgProperties;

    private final PasswordEncoderProvider passwordEncoderProvider;


    /**
     * 新增租户
     *
     * @param dto 租户数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void addOrgTenant(OrgTenantDTO dto) {
        // 校验租户编码唯一性
        if (StrUtil.isNotBlank(dto.getTenantCode())) {
            OrgTenantDTO existTenant = tenantManager.getByTenantCode(dto.getTenantCode());
            if (existTenant != null) {
                throw OrgErrorCode.ORG_TENANT_CODE_ALREADY_EXISTS.exception();
            }
        }

        // 设置默认状态为正常
        if (dto.getTenantStatus() == null) {
            dto.setTenantStatus(OrgTenantStateEnum.NORMAL);
        }

        boolean success = tenantManager.add(dto);
        if (!success) {
            throw OrgErrorCode.ORG_SYSTEM_ERROR.exception("新增租户失败");
        }
        log.info("新增租户成功：id={}, tenantCode={}", dto.getId(), dto.getTenantCode());
    }


    /**
     * 删除租户（逻辑删除）
     *
     * @param ids 租户id集合
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrgTenant(Set<Long> ids) {
        for (Serializable id : ids) {
            OrgTenantDTO tenant = tenantManager.getDtoById(id);
            if (tenant == null) {
                throw OrgErrorCode.ORG_TENANT_NOT_FOUND.exception();
            }

            // 平台租户不允许删除
            if (isPlatformTenant(tenant.getId())) {
                throw OrgErrorCode.ORG_TENANT_PLATFORM_NOT_EDITABLE.exception();
            }

            // 租户下存在用户时不允许删除。
            // 已知限制（代码评审 M3）：只校验 org_user；空壳部门/角色/职位可能残留为孤儿数据，
            // 删除租户前应先在对应管理页清理，后续可扩展为全资源校验
            Long userCount = userManager.countByTenantId(tenant.getId());
            if (userCount > 0) {
                throw OrgErrorCode.ORG_TENANT_HAS_USERS.exception();
            }

            boolean success = tenantManager.delete(id);
            if (!success) {
                log.warn("删除租户失败：id={}", id);
            }
        }
        log.info("删除租户成功：ids={}", ids);
    }


    /**
     * 编辑租户
     *
     * @param dto 对象信息（租户编码创建后不可修改，更新参数不含 tenantCode）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOrgTenant(OrgTenantDTO dto) {
        OrgTenantDTO existTenant = tenantManager.getDtoById(dto.getId());
        if (existTenant == null) {
            throw OrgErrorCode.ORG_TENANT_NOT_FOUND.exception();
        }

        // 不触碰编码与状态：保留原值（更新参数本身不含这两个字段，此处防御 DTO 透出 null 覆盖）
        dto.setTenantCode(existTenant.getTenantCode());
        dto.setTenantStatus(existTenant.getTenantStatus());

        boolean success = tenantManager.modify(dto);
        if (!success) {
            throw OrgErrorCode.ORG_SYSTEM_ERROR.exception("修改租户失败");
        }
        log.info("修改租户成功：id={}", dto.getId());
    }


    /**
     * 停用/启用租户
     *
     * @param id     租户ID
     * @param status 状态：1正常，2停用
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Serializable id, Integer status) {
        OrgTenantStateEnum state = parseState(status);

        OrgTenantDTO tenant = tenantManager.getDtoById(id);
        if (tenant == null) {
            throw OrgErrorCode.ORG_TENANT_NOT_FOUND.exception();
        }

        // 平台租户不允许停用
        if (isPlatformTenant(tenant.getId())) {
            throw OrgErrorCode.ORG_TENANT_PLATFORM_NOT_EDITABLE.exception();
        }

        tenant.setTenantStatus(state);
        boolean success = tenantManager.modify(tenant);
        if (!success) {
            throw OrgErrorCode.ORG_SYSTEM_ERROR.exception("修改租户状态失败");
        }
        log.info("修改租户状态成功：id={}, status={}", id, status);
    }


    /**
     * 为新租户初始化管理员账号
     *
     * <p>管理员创建在目标租户内（显式 tenantId，INSERT 显式值优先于租户拦截器默认值）；
     * 账号/手机号唯一性在目标租户内校验（并发由 uk_org_user_tenant_account /
     * uk_org_user_tenant_phone 唯一索引兜底）。</p>
     *
     * <p>已知限制（代码评审 H3）：初始管理员不分配任何角色，登录后需由平台管理员
     * 在「用户管理」中为其授予该租户的管理角色；「租户管理员角色模板」属于后续增强。</p>
     *
     * @param param 初始化参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void initTenantAdmin(OrgTenantInitAdminParam param) {
        // 校验租户存在且可用
        OrgTenantDTO tenant = tenantManager.getDtoById(param.getTenantId());
        if (tenant == null) {
            throw OrgErrorCode.ORG_TENANT_NOT_FOUND.exception();
        }
        if (tenant.getTenantStatus() == OrgTenantStateEnum.DISABLE) {
            throw OrgErrorCode.ORG_TENANT_DISABLED.exception();
        }
        if (isExpired(tenant.getExpireTime())) {
            throw OrgErrorCode.ORG_TENANT_EXPIRED.exception();
        }

        Long tenantId = tenant.getId();

        // 账号在目标租户内唯一
        if (userManager.getUserByAccountAndTenant(param.getAccount(), tenantId) != null) {
            throw OrgErrorCode.ORG_TENANT_ADMIN_ALREADY_EXISTS.exception();
        }

        // 手机号在目标租户内唯一
        if (StrUtil.isNotBlank(param.getPhone())
            && userManager.getUserByPhoneAndTenant(param.getPhone(), tenantId) != null) {
            throw OrgErrorCode.ORG_USER_PHONE_ALREADY_EXISTS.exception();
        }

        OrgUserDTO userDto = new OrgUserDTO();
        userDto.setAccount(param.getAccount());
        userDto.setNickname(param.getNickname());
        userDto.setPhone(param.getPhone());
        userDto.setPwd(passwordEncoderProvider.encode(param.getPassword()));
        userDto.setUserStatus(OrgUserStateEnum.ENABLE);
        // 显式指定租户：MyBatis-Plus 租户拦截器对自带 tenant_id 列的 INSERT 不再覆盖
        userDto.setTenantId(tenantId);

        boolean success;
        try {
            success = userManager.add(userDto);
        } catch (DuplicateKeyException e) {
            // 并发兜底：前面的 check-then-insert 存在竞态，由 uk_org_user_tenant_account /
            // uk_org_user_tenant_phone 唯一索引在数据库层拦截，这里翻译为业务异常
            throw OrgErrorCode.ORG_TENANT_ADMIN_ALREADY_EXISTS.exception();
        }
        if (!success) {
            throw OrgErrorCode.ORG_SYSTEM_ERROR.exception("初始化租户管理员失败");
        }
        log.info("初始化租户管理员成功：tenantId={}, account={}, userId={}",
            tenantId, param.getAccount(), userDto.getId());
    }


    /**
     * 是否平台租户
     */
    private boolean isPlatformTenant(Long tenantId) {
        Long platformTenantId = orgProperties.getTenant().getPlatformTenantId();
        return platformTenantId != null && platformTenantId.equals(tenantId);
    }


    /**
     * 租户是否已过期
     */
    private boolean isExpired(LocalDateTime expireTime) {
        return expireTime != null && expireTime.isBefore(LocalDateTime.now());
    }


    /**
     * 解析租户状态
     */
    private OrgTenantStateEnum parseState(Integer status) {
        if (status != null) {
            for (OrgTenantStateEnum state : OrgTenantStateEnum.values()) {
                if (state.getCode().equals(status)) {
                    return state;
                }
            }
        }
        throw OrgErrorCode.ORG_SYSTEM_ERROR.exception("租户状态值不正确，只能是1（正常）或2（停用）");
    }

}
