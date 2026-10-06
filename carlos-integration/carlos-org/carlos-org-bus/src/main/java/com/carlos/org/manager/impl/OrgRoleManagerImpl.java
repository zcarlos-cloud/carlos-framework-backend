package com.carlos.org.manager.impl;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.org.convert.OrgRoleConvert;
import com.carlos.org.manager.OrgRoleManager;
import com.carlos.org.manager.OrgRoleOperationManager;
import com.carlos.org.mapper.OrgRoleMapper;
import com.carlos.org.mapper.OrgUserRoleMapper;
import com.carlos.org.pojo.dto.OrgRoleDTO;
import com.carlos.org.pojo.dto.OrgRoleUserDTO;
import com.carlos.org.pojo.entity.*;
import com.carlos.org.pojo.enums.OrgRoleStateEnum;
import com.carlos.org.pojo.param.OrgRolePageParam;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OrgRoleManagerImpl extends BaseServiceImpl<OrgRoleMapper, OrgRole> implements OrgRoleManager {

    private final OrgRoleOperationManager orgRoleOperationManager;
    private final OrgUserRoleMapper orgUserRoleMapper;

    @Override
    public boolean add(OrgRoleDTO dto) {
        OrgRole entity = OrgRoleConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'OrgRole' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        log.debug("Insert 'OrgRole' data: id:{}", entity.getId());
        return true;
    }

    @Override
    public boolean delete(Serializable id) {
        if (id == null) {
            log.warn("id can't be null");
            return false;
        }
        boolean success = removeById(id);
        if (!success) {
            log.warn("Remove 'OrgRole' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'OrgRole' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(OrgRoleDTO dto) {
        OrgRole entity = OrgRoleConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'OrgRole' data fail, entity:{}", entity);
            return false;
        }
        log.debug("Update 'OrgRole' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public OrgRoleDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        OrgRole entity = getBaseMapper().selectById(id);
        OrgRoleDTO dto = OrgRoleConvert.INSTANCE.toDTO(entity);
        if (dto != null) {
            // 查询用户数量
            dto.setUserCount(getUserCountByRoleId(id));
            // 查询操作ID列表
            dto.setOperationIds(getOperationIdsByRoleId(id));
        }
        return dto;
    }

    @Override
    public Paging<OrgRoleDTO> getPage(OrgRolePageParam param) {
        LambdaQueryWrapper<OrgRole> wrapper = new LambdaQueryWrapper<>();
        if (param.getRoleName() != null && !param.getRoleName().isEmpty()) {
            wrapper.like(OrgRole::getRoleName, param.getRoleName());
        }
        if (param.getRoleCode() != null && !param.getRoleCode().isEmpty()) {
            wrapper.like(OrgRole::getRoleCode, param.getRoleCode());
        }
        if (param.getRoleType() != null) {
            wrapper.eq(OrgRole::getRoleType, param.getRoleType());
        }
        if (param.getRoleStatus() != null) {
            wrapper.eq(OrgRole::getRoleStatus, param.getRoleStatus());
        }
        wrapper.orderByDesc(OrgRole::getCreateTime);
        PageInfo<OrgRole> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, entities -> entities.stream()
            .map(OrgRoleConvert.INSTANCE::toDTO)
            .collect(Collectors.toList()));
    }

    @Override
    public OrgRoleDTO getByRoleCode(String roleCode) {
        LambdaQueryWrapper<OrgRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrgRole::getRoleCode, roleCode);
        OrgRole entity = getBaseMapper().selectOne(wrapper);
        return OrgRoleConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public List<OrgRoleDTO> listEnabled() {
        LambdaQueryWrapper<OrgRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(OrgRole::getId, OrgRole::getRoleCode, OrgRole::getRoleName);
        wrapper.eq(OrgRole::getRoleStatus, OrgRoleStateEnum.ENABLE);
        wrapper.orderByAsc(OrgRole::getId);
        return list(wrapper).stream().map(OrgRoleConvert.INSTANCE::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<Long> getOperationIdsByRoleId(Serializable roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        return orgRoleOperationManager.listOperationIdsByRoleId((Long) roleId);
    }

    @Override
    public Long getUserCountByRoleId(Serializable roleId) {
        if (roleId == null) {
            return 0L;
        }
        return orgUserRoleMapper.selectCount(
            Wrappers.<OrgUserRole>lambdaQuery()
                .eq(OrgUserRole::getRoleId, roleId)
        );
    }

    @Override
    public boolean assignOperations(Serializable roleId, List<Serializable> operationIds) {
        if (roleId == null) {
            log.warn("roleId can't be null");
            return false;
        }
        if (operationIds == null) {
            operationIds = Collections.emptyList();
        }
        orgRoleOperationManager.saveRoleOperations(
            (Long) roleId,
            operationIds.stream()
                .map(id -> (Long) id)
                .collect(Collectors.toSet())
        );
        return true;
    }

    @Override
    public Paging<OrgRoleUserDTO> getRoleUsers(Serializable roleId, OrgRolePageParam param) {
        if (roleId == null || param == null) {
            return new Paging<>();
        }
        MPJLambdaWrapper<OrgUserRole> wrapper = new MPJLambdaWrapper<OrgUserRole>()
            .selectAs(OrgUser::getId, OrgRoleUserDTO::getUserId)
            .selectAs(OrgUser::getAccount, OrgRoleUserDTO::getAccount)
            .selectAs(OrgUser::getNickname, OrgRoleUserDTO::getName)
            .selectAs(OrgUser::getPhone, OrgRoleUserDTO::getPhone)
            .selectAs(OrgDepartment::getDeptName, OrgRoleUserDTO::getMainDeptName)
            .selectAs(OrgUserRole::getCreateTime, OrgRoleUserDTO::getCreateTime)
            .innerJoin(OrgUser.class, OrgUser::getId, OrgUserRole::getUserId)
            .leftJoin(OrgDepartment.class, OrgDepartment::getId, OrgUser::getMainDeptId)
            .eq(OrgUserRole::getRoleId, roleId)
            .orderByDesc(OrgUserRole::getCreateTime);

        PageInfo<OrgRoleUserDTO> page = new PageInfo<>(param.getCurrent(), param.getSize());
        // selectJoinPage 会修改传入的 page 对象，填充 records 和 total
        orgUserRoleMapper.selectJoinPage(page, OrgRoleUserDTO.class, wrapper);
        return MybatisPage.convert(page, items -> items);
    }

    @Override
    public List<Long> listUserIdsByRoleId(Serializable roleId) {
        Long roleIdValue = Convert.toLong(roleId);
        if (roleIdValue == null) {
            return List.of();
        }
        List<OrgUserRole> relations = orgUserRoleMapper.selectList(
            Wrappers.<OrgUserRole>lambdaQuery()
                .select(OrgUserRole::getUserId)
                .eq(OrgUserRole::getRoleId, roleIdValue)
                // ADR-002: 过期临时授权不生效，口径与 OrgUserRoleManager#listByUserId 保持一致
                .and(w -> w.isNull(OrgUserRole::getExpireTime)
                    .or()
                    .gt(OrgUserRole::getExpireTime, LocalDateTime.now())));
        return relations.stream()
            .map(OrgUserRole::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    @Override
    public List<Long> listUserIdsByRoleIdAndTenant(Serializable roleId, Long tenantId) {
        Long roleIdValue = Convert.toLong(roleId);
        if (roleIdValue == null || tenantId == null) {
            return List.of();
        }
        // 绕过租户拦截器后显式按 tenant_id 过滤，避免无请求上下文时回落到平台租户
        IgnoreStrategy ignoreStrategy = IgnoreStrategy.builder().tenantLine(true).build();
        List<OrgUserRole> relations = InterceptorIgnoreHelper.execute(ignoreStrategy, () -> orgUserRoleMapper.selectList(
            Wrappers.<OrgUserRole>lambdaQuery()
                .select(OrgUserRole::getUserId)
                .eq(OrgUserRole::getRoleId, roleIdValue)
                .eq(OrgUserRole::getTenantId, tenantId)
                // ADR-002: 过期临时授权不生效，口径与 OrgUserRoleManager#listByUserId 保持一致
                .and(w -> w.isNull(OrgUserRole::getExpireTime)
                    .or()
                    .gt(OrgUserRole::getExpireTime, LocalDateTime.now()))));
        return relations.stream()
            .map(OrgUserRole::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

}
