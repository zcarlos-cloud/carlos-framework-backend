package com.carlos.org.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.org.convert.OrgDepartmentRoleConvert;
import com.carlos.org.manager.OrgDepartmentRoleManager;
import com.carlos.org.mapper.OrgDepartmentRoleMapper;
import com.carlos.org.pojo.dto.OrgDepartmentRoleDTO;
import com.carlos.org.pojo.entity.OrgDepartmentRole;
import com.carlos.org.pojo.param.OrgDepartmentRolePageParam;
import com.carlos.org.pojo.vo.OrgDepartmentRoleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 部门角色 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OrgDepartmentRoleManagerImpl extends BaseServiceImpl<OrgDepartmentRoleMapper, OrgDepartmentRole> implements OrgDepartmentRoleManager {

    @Override
    public boolean add(OrgDepartmentRoleDTO dto) {
        OrgDepartmentRole entity = OrgDepartmentRoleConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'OrgDepartmentRole' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'OrgDepartmentRole' data: id:{}", entity.getId());
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
            log.warn("Remove 'OrgDepartmentRole' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'OrgDepartmentRole' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(OrgDepartmentRoleDTO dto) {
        OrgDepartmentRole entity = OrgDepartmentRoleConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'OrgDepartmentRole' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操作
        log.debug("Update 'OrgDepartmentRole' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public OrgDepartmentRoleDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        OrgDepartmentRole entity = getBaseMapper().selectById(id);
        return OrgDepartmentRoleConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<OrgDepartmentRoleVO> getPage(OrgDepartmentRolePageParam param) {
        LambdaQueryWrapper<OrgDepartmentRole> wrapper = queryWrapper();
        wrapper.select(

            OrgDepartmentRole::getId,
            OrgDepartmentRole::getDeptId,
            OrgDepartmentRole::getRoleId,
            OrgDepartmentRole::getDefaultRole,
            OrgDepartmentRole::getCreateBy,
            OrgDepartmentRole::getCreateTime,
            OrgDepartmentRole::getUpdateTime,
            OrgDepartmentRole::getTenantId
        );
        PageInfo<OrgDepartmentRole> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, OrgDepartmentRoleConvert.INSTANCE::toVO);
    }

    @Override
    public boolean removeByDeptIdAndRoleId(Serializable deptId, Serializable roleId) {
        if (deptId == null || roleId == null) {
            return false;
        }
        LambdaQueryWrapper<OrgDepartmentRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrgDepartmentRole::getDeptId, deptId)
            .eq(OrgDepartmentRole::getRoleId, roleId);
        return remove(wrapper);
    }

    @Override
    public List<OrgDepartmentRoleDTO> listByDeptId(Serializable deptId) {
        if (deptId == null) {
            return List.of();
        }
        LambdaQueryWrapper<OrgDepartmentRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrgDepartmentRole::getDeptId, deptId);
        List<OrgDepartmentRole> list = list(wrapper);
        return OrgDepartmentRoleConvert.INSTANCE.toDTO(list);
    }

}
