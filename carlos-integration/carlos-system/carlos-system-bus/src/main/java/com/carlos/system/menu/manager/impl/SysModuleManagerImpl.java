package com.carlos.system.menu.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.system.menu.convert.SysModuleConvert;
import com.carlos.system.menu.manager.SysModuleManager;
import com.carlos.system.menu.mapper.SysModuleMapper;
import com.carlos.system.menu.pojo.dto.SysModuleDTO;
import com.carlos.system.menu.pojo.entity.SysModule;
import com.carlos.system.menu.pojo.param.SysModulePageParam;
import com.carlos.system.menu.pojo.vo.SysModuleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 功能模块 查询封装实现类
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SysModuleManagerImpl extends BaseServiceImpl<SysModuleMapper, SysModule> implements SysModuleManager {

    @Override
    public SysModule getByModuleCode(String moduleCode) {
        if (moduleCode == null || moduleCode.isEmpty()) {
            return null;
        }
        return lambdaQuery()
            .eq(SysModule::getModuleCode, moduleCode)
            .eq(SysModule::getModuleStatus, true)
            .one();
    }

    @Override
    public List<SysModule> listEnabled() {
        return lambdaQuery()
            .eq(SysModule::getModuleStatus, true)
            .orderByAsc(SysModule::getOrderNum)
            .list();
    }

    @Override
    public List<SysModule> listAll() {
        // 不做 moduleStatus 过滤：管理后台的模块树要把禁用模块也显示出来
        return lambdaQuery()
            .orderByAsc(SysModule::getOrderNum)
            .orderByAsc(SysModule::getId)
            .list();
    }

    @Override
    public boolean add(SysModuleDTO dto) {
        SysModule entity = SysModuleConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'SysModule' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        log.debug("Insert 'SysModule' data: id:{}", entity.getId());
        return true;
    }

    @Override
    public boolean delete(Long id) {
        if (id == null) {
            log.warn("id can't be null");
            return false;
        }
        boolean success = removeById(id);
        if (!success) {
            log.warn("Remove 'SysModule' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'SysModule' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(SysModuleDTO dto) {
        SysModule entity = SysModuleConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'SysModule' data fail, entity:{}", entity);
            return false;
        }
        log.debug("Update 'SysModule' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public SysModuleDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        SysModule entity = getBaseMapper().selectById(id);
        return SysModuleConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<SysModuleVO> getPage(SysModulePageParam param) {
        LambdaQueryWrapper<SysModule> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(
            SysModule::getId,
            SysModule::getParentId,
            SysModule::getModuleName,
            SysModule::getModuleCode,
            SysModule::getOrderNum,
            SysModule::getModuleStatus,
            SysModule::getCreateBy,
            SysModule::getCreateTime,
            SysModule::getUpdateBy,
            SysModule::getUpdateTime
        );
        wrapper.orderByAsc(SysModule::getOrderNum);

        if (StrUtil.isNotBlank(param.getModuleName())) {
            wrapper.like(SysModule::getModuleName, param.getModuleName());
        }
        if (StrUtil.isNotBlank(param.getModuleCode())) {
            wrapper.like(SysModule::getModuleCode, param.getModuleCode());
        }
        if (param.getModuleStatus() != null) {
            wrapper.eq(SysModule::getModuleStatus, param.getModuleStatus());
        }

        PageInfo<SysModule> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, SysModuleConvert.INSTANCE::do2vo);
    }

}
