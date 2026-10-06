package com.carlos.org.manager.impl;

import cn.hutool.core.collection.CollUtil;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.org.convert.OrgDockingMappingConvert;
import com.carlos.org.manager.OrgDockingMappingManager;
import com.carlos.org.mapper.OrgDockingMappingMapper;
import com.carlos.org.pojo.dto.OrgDockingMappingDTO;
import com.carlos.org.pojo.entity.OrgDockingMapping;
import com.carlos.org.pojo.enums.OrgDockingTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 用户信息对接关联表 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2025-2-27 15:41:32
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OrgDockingMappingManagerImpl extends BaseServiceImpl<OrgDockingMappingMapper, OrgDockingMapping> implements OrgDockingMappingManager {

    @Override
    public boolean add(OrgDockingMappingDTO dto) {
        OrgDockingMapping entity = OrgDockingMappingConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'OrgDockingMapping' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'OrgDockingMapping' data: id:{}", entity.getId());
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
            log.warn("Remove 'OrgDockingMapping' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'OrgDockingMapping' data by id:{}", id);
        return true;
    }

    @Override
    public OrgDockingMappingDTO getDockingMapping(OrgDockingTypeEnum type, String targetCode, String targetId) {
        OrgDockingMapping entity = lambdaQuery()
            .select(OrgDockingMapping::getId, OrgDockingMapping::getSystemId, OrgDockingMapping::getTargetId, OrgDockingMapping::getTargetCode, OrgDockingMapping::getDockingType)
            .eq(OrgDockingMapping::getDockingType, type)
            .eq(OrgDockingMapping::getTargetCode, targetCode)
            .eq(OrgDockingMapping::getTargetId, targetId)
            .one();
        return OrgDockingMappingConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public OrgDockingMappingDTO getDockingMappingByTargetId(String targetId) {
        OrgDockingMapping entity = lambdaQuery()
            .select(OrgDockingMapping::getId, OrgDockingMapping::getSystemId, OrgDockingMapping::getTargetId, OrgDockingMapping::getTargetCode, OrgDockingMapping::getDockingType)
            .eq(OrgDockingMapping::getTargetId, targetId)
            .one();
        return OrgDockingMappingConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public boolean deleteByTargetId(String targetId) {
        this.baseMapper.delete(queryWrapper().eq(OrgDockingMapping::getTargetId, targetId));
        return true;
    }

    @Override
    public OrgDockingMappingDTO getSystemIdByTargetId(String targetId) {
        OrgDockingMapping entity = lambdaQuery()
            .eq(OrgDockingMapping::getTargetId, targetId)
            .one();
        return OrgDockingMappingConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public List<OrgDockingMappingDTO> getDockingMappingList(OrgDockingTypeEnum type) {
        List<OrgDockingMapping> list = lambdaQuery()
            .eq(OrgDockingMapping::getDockingType, type)
            .list();
        return OrgDockingMappingConvert.INSTANCE.toDTO(list);
    }

    @Override
    public List<OrgDockingMappingDTO> listByTargetIds(Set<String> targetIds) {
        List<OrgDockingMapping> list = lambdaQuery()
            .in(CollUtil.isNotEmpty(targetIds), OrgDockingMapping::getTargetId, targetIds)
            .list();
        return OrgDockingMappingConvert.INSTANCE.toDTO(list);
    }

    @Override
    public List<OrgDockingMappingDTO> listBySystemIds(Set<String> systemIds) {
        List<OrgDockingMapping> list = lambdaQuery()
            .in(CollUtil.isNotEmpty(systemIds), OrgDockingMapping::getSystemId, systemIds)
            .list();
        return OrgDockingMappingConvert.INSTANCE.toDTO(list);
    }
}
