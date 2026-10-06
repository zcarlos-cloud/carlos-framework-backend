package com.carlos.org.position.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.org.position.convert.OrgPositionCategoryConvert;
import com.carlos.org.position.manager.OrgPositionCategoryManager;
import com.carlos.org.position.mapper.OrgPositionCategoryMapper;
import com.carlos.org.position.pojo.dto.OrgPositionCategoryDTO;
import com.carlos.org.position.pojo.entity.OrgPositionCategory;
import com.carlos.org.position.pojo.param.OrgPositionCategoryPageParam;
import com.carlos.org.position.pojo.vo.OrgPositionCategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * <p>
 * 岗位类别表（职系） 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OrgPositionCategoryManagerImpl extends BaseServiceImpl<OrgPositionCategoryMapper, OrgPositionCategory> implements OrgPositionCategoryManager {

    @Override
    public boolean add(OrgPositionCategoryDTO dto) {
        OrgPositionCategory entity = OrgPositionCategoryConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'OrgPositionCategory' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'OrgPositionCategory' data: id:{}", entity.getId());
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
            log.warn("Remove 'OrgPositionCategory' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'OrgPositionCategory' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(OrgPositionCategoryDTO dto) {
        OrgPositionCategory entity = OrgPositionCategoryConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'OrgPositionCategory' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操作
        log.debug("Update 'OrgPositionCategory' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public OrgPositionCategoryDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        OrgPositionCategory entity = getBaseMapper().selectById(id);
        return OrgPositionCategoryConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<OrgPositionCategoryVO> getPage(OrgPositionCategoryPageParam param) {
        LambdaQueryWrapper<OrgPositionCategory> wrapper = queryWrapper();
        wrapper.select(

            OrgPositionCategory::getId,
            OrgPositionCategory::getCategoryCode,
            OrgPositionCategory::getCategoryName,
            OrgPositionCategory::getCategoryType,
            OrgPositionCategory::getDescription,
            OrgPositionCategory::getOrderNum,
            OrgPositionCategory::getPositionCategoryStatus,
            OrgPositionCategory::getTenantId,
            OrgPositionCategory::getCreateBy,
            OrgPositionCategory::getCreateTime,
            OrgPositionCategory::getUpdateBy,
            OrgPositionCategory::getUpdateTime
        );
        PageInfo<OrgPositionCategory> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, OrgPositionCategoryConvert.INSTANCE::toVO);
    }

}
