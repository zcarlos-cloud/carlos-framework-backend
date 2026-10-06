package com.carlos.org.position.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.org.position.convert.OrgPositionLevelConvert;
import com.carlos.org.position.manager.OrgPositionLevelManager;
import com.carlos.org.position.mapper.OrgPositionLevelMapper;
import com.carlos.org.position.pojo.dto.OrgPositionLevelDTO;
import com.carlos.org.position.pojo.entity.OrgPositionLevel;
import com.carlos.org.position.pojo.param.OrgPositionLevelPageParam;
import com.carlos.org.position.pojo.vo.OrgPositionLevelVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * <p>
 * 职级表 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OrgPositionLevelManagerImpl extends BaseServiceImpl<OrgPositionLevelMapper, OrgPositionLevel> implements OrgPositionLevelManager {

    @Override
    public boolean add(OrgPositionLevelDTO dto) {
        OrgPositionLevel entity = OrgPositionLevelConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'OrgPositionLevel' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'OrgPositionLevel' data: id:{}", entity.getId());
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
            log.warn("Remove 'OrgPositionLevel' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'OrgPositionLevel' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(OrgPositionLevelDTO dto) {
        OrgPositionLevel entity = OrgPositionLevelConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'OrgPositionLevel' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操作
        log.debug("Update 'OrgPositionLevel' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public OrgPositionLevelDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        OrgPositionLevel entity = getBaseMapper().selectById(id);
        return OrgPositionLevelConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<OrgPositionLevelVO> getPage(OrgPositionLevelPageParam param) {
        LambdaQueryWrapper<OrgPositionLevel> wrapper = queryWrapper();
        wrapper.select(

            OrgPositionLevel::getId,
            OrgPositionLevel::getCategoryId,
            OrgPositionLevel::getLevelCode,
            OrgPositionLevel::getLevelName,
            OrgPositionLevel::getLevelSeq,
            OrgPositionLevel::getLevelGroup,
            OrgPositionLevel::getMinSalary,
            OrgPositionLevel::getMaxSalary,
            OrgPositionLevel::getStockLevel,
            OrgPositionLevel::getDescription,
            OrgPositionLevel::getRequirements,
            OrgPositionLevel::getPositionLevelStatus,
            OrgPositionLevel::getTenantId,
            OrgPositionLevel::getCreateBy,
            OrgPositionLevel::getCreateTime,
            OrgPositionLevel::getUpdateBy,
            OrgPositionLevel::getUpdateTime
        );
        PageInfo<OrgPositionLevel> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, OrgPositionLevelConvert.INSTANCE::toVO);
    }

}
