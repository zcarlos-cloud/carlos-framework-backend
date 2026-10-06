package com.carlos.org.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.org.convert.OrgTenantConvert;
import com.carlos.org.manager.OrgTenantManager;
import com.carlos.org.mapper.OrgTenantMapper;
import com.carlos.org.pojo.dto.OrgTenantDTO;
import com.carlos.org.pojo.entity.OrgTenant;
import com.carlos.org.pojo.param.OrgTenantPageParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.stream.Collectors;

/**
 * <p>
 * 租户 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OrgTenantManagerImpl extends BaseServiceImpl<OrgTenantMapper, OrgTenant> implements OrgTenantManager {

    @Override
    public boolean add(OrgTenantDTO dto) {
        OrgTenant entity = OrgTenantConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'OrgTenant' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        log.debug("Insert 'OrgTenant' data: id:{}", entity.getId());
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
            log.warn("Remove 'OrgTenant' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'OrgTenant' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(OrgTenantDTO dto) {
        OrgTenant entity = OrgTenantConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'OrgTenant' data fail, entity:{}", entity);
            return false;
        }
        log.debug("Update 'OrgTenant' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public OrgTenantDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        OrgTenant entity = getBaseMapper().selectById(id);
        return OrgTenantConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<OrgTenantDTO> getPage(OrgTenantPageParam param) {
        LambdaQueryWrapper<OrgTenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(OrgTenant::getId, OrgTenant::getTenantCode, OrgTenant::getTenantName,
            OrgTenant::getContactName, OrgTenant::getContactPhone, OrgTenant::getTenantStatus,
            OrgTenant::getExpireTime, OrgTenant::getRemark, OrgTenant::getCreateBy,
            OrgTenant::getCreateTime, OrgTenant::getUpdateTime);
        if (param.getTenantCode() != null && !param.getTenantCode().isEmpty()) {
            wrapper.like(OrgTenant::getTenantCode, param.getTenantCode());
        }
        if (param.getTenantName() != null && !param.getTenantName().isEmpty()) {
            wrapper.like(OrgTenant::getTenantName, param.getTenantName());
        }
        if (param.getTenantStatus() != null) {
            wrapper.eq(OrgTenant::getTenantStatus, param.getTenantStatus());
        }
        // 稳定分页排序：create_time 可能相同，需加唯一 id 兜底
        wrapper.orderByDesc(OrgTenant::getCreateTime).orderByDesc(OrgTenant::getId);
        PageInfo<OrgTenant> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, entities -> entities.stream()
            .map(OrgTenantConvert.INSTANCE::toDTO)
            .collect(Collectors.toList()));
    }

    @Override
    public OrgTenantDTO getByTenantCode(String tenantCode) {
        if (tenantCode == null) {
            return null;
        }
        LambdaQueryWrapper<OrgTenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrgTenant::getTenantCode, tenantCode);
        OrgTenant entity = getBaseMapper().selectOne(wrapper);
        return OrgTenantConvert.INSTANCE.toDTO(entity);
    }

}
