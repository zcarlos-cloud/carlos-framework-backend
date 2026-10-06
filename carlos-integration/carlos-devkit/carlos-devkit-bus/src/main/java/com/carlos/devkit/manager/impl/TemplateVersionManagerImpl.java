package com.carlos.devkit.manager.impl;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.devkit.convert.TemplateVersionConvert;
import com.carlos.devkit.manager.TemplateVersionManager;
import com.carlos.devkit.mapper.DevTemplateVersionMapper;
import com.carlos.devkit.pojo.dto.TemplateVersionDTO;
import com.carlos.devkit.pojo.entity.DevTemplateVersion;
import com.carlos.devkit.pojo.param.TemplateVersionPageParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 模板版本历史 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class TemplateVersionManagerImpl extends BaseServiceImpl<DevTemplateVersionMapper, DevTemplateVersion>
    implements TemplateVersionManager {

    @Override
    public boolean add(final TemplateVersionDTO dto) {
        DevTemplateVersion entity = TemplateVersionConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("新增模板版本失败, templateId:{}, versionNo:{}", dto.getTemplateId(), dto.getVersionNo());
            return false;
        }
        dto.setId(entity.getId());
        return true;
    }

    @Override
    public Paging<TemplateVersionDTO> getPage(final TemplateVersionPageParam param) {
        LambdaQueryWrapper<DevTemplateVersion> wrapper = queryWrapper();
        // 列表不下发正文快照，避免大字段
        wrapper.select(DevTemplateVersion.class, field -> !"content".equals(field.getProperty()));
        wrapper.eq(DevTemplateVersion::getTemplateId, param.getTemplateId());
        wrapper.orderByDesc(DevTemplateVersion::getVersionNo);
        return MybatisPage.map(page(pageInfo(param), wrapper), TemplateVersionConvert.INSTANCE::toDTO);
    }

    @Override
    public TemplateVersionDTO getDtoById(final Serializable id) {
        if (id == null) {
            return null;
        }
        return TemplateVersionConvert.INSTANCE.toDTO(getBaseMapper().selectById(id));
    }

    @Override
    public int nextVersionNo(final Serializable templateId) {
        LambdaQueryWrapper<DevTemplateVersion> wrapper = queryWrapper();
        wrapper.select(DevTemplateVersion::getVersionNo);
        wrapper.eq(DevTemplateVersion::getTemplateId, templateId);
        wrapper.orderByDesc(DevTemplateVersion::getVersionNo);
        Page<DevTemplateVersion> page = new Page<>(1, 1, false);
        List<DevTemplateVersion> list = getBaseMapper().selectList(page, wrapper);
        return list.isEmpty() || ObjUtil.isNull(list.getFirst().getVersionNo())
            ? 1
            : list.getFirst().getVersionNo() + 1;
    }

    @Override
    public List<Long> listExpiredVersionIds(final Serializable templateId, final int keepCount) {
        LambdaQueryWrapper<DevTemplateVersion> wrapper = queryWrapper();
        wrapper.select(DevTemplateVersion::getId);
        wrapper.eq(DevTemplateVersion::getTemplateId, templateId);
        wrapper.orderByDesc(DevTemplateVersion::getVersionNo);
        List<DevTemplateVersion> all = getBaseMapper().selectList(wrapper);
        if (all.size() <= keepCount) {
            return List.of();
        }
        return all.subList(keepCount, all.size()).stream().map(DevTemplateVersion::getId).toList();
    }

    @Override
    public boolean delete(final Collection<Serializable> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return removeByIds(ids);
    }
}
