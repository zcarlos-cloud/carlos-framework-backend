package com.carlos.org.position.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.org.position.convert.OrgPositionRoleConvert;
import com.carlos.org.position.manager.OrgPositionRoleManager;
import com.carlos.org.position.mapper.OrgPositionRoleMapper;
import com.carlos.org.position.pojo.dto.OrgPositionRoleDTO;
import com.carlos.org.position.pojo.entity.OrgPositionRole;
import com.carlos.org.position.pojo.param.OrgPositionRolePageParam;
import com.carlos.org.position.pojo.vo.OrgPositionRoleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * <p>
 * 岗位角色关联表（岗位默认权限配置） 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OrgPositionRoleManagerImpl extends BaseServiceImpl<OrgPositionRoleMapper, OrgPositionRole> implements OrgPositionRoleManager {

    @Override
    public boolean add(OrgPositionRoleDTO dto) {
        OrgPositionRole entity = OrgPositionRoleConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'OrgPositionRole' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'OrgPositionRole' data: id:{}", entity.getId());
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
            log.warn("Remove 'OrgPositionRole' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'OrgPositionRole' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(OrgPositionRoleDTO dto) {
        OrgPositionRole entity = OrgPositionRoleConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'OrgPositionRole' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操作
        log.debug("Update 'OrgPositionRole' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public OrgPositionRoleDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        OrgPositionRole entity = getBaseMapper().selectById(id);
        return OrgPositionRoleConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<OrgPositionRoleVO> getPage(OrgPositionRolePageParam param) {
        LambdaQueryWrapper<OrgPositionRole> wrapper = queryWrapper();
        wrapper.select(

            OrgPositionRole::getId,
            OrgPositionRole::getPositionId,
            OrgPositionRole::getRoleId,
            OrgPositionRole::getDefaultRole,
            OrgPositionRole::getPositionRoleStatus,
            OrgPositionRole::getCreateBy,
            OrgPositionRole::getCreateTime,
            OrgPositionRole::getUpdateBy,
            OrgPositionRole::getUpdateTime,
            OrgPositionRole::getTenantId
        );
        PageInfo<OrgPositionRole> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, OrgPositionRoleConvert.INSTANCE::toVO);
    }

}
