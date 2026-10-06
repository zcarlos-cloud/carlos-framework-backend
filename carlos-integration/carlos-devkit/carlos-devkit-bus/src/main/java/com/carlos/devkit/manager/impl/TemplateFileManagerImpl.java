package com.carlos.devkit.manager.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.devkit.convert.TemplateFileConvert;
import com.carlos.devkit.manager.TemplateFileManager;
import com.carlos.devkit.mapper.DevTemplateMapper;
import com.carlos.devkit.pojo.dto.TemplateFileDTO;
import com.carlos.devkit.pojo.entity.DevTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 模板文件 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class TemplateFileManagerImpl extends BaseServiceImpl<DevTemplateMapper, DevTemplate>
    implements TemplateFileManager {

    @Override
    public boolean add(final TemplateFileDTO dto) {
        DevTemplate entity = TemplateFileConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("新增模板文件失败, path:{}", dto.getFilePath());
            return false;
        }
        dto.setId(entity.getId());
        return true;
    }

    @Override
    public boolean modify(final TemplateFileDTO dto) {
        boolean success = updateById(TemplateFileConvert.INSTANCE.toDO(dto));
        if (!success) {
            log.warn("更新模板文件失败, id:{}", dto.getId());
        }
        return success;
    }

    @Override
    public boolean delete(final Collection<Serializable> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return removeByIds(ids);
    }

    @Override
    public TemplateFileDTO getDtoById(final Serializable id) {
        if (id == null) {
            return null;
        }
        return TemplateFileConvert.INSTANCE.toDTO(getBaseMapper().selectById(id));
    }

    @Override
    public List<TemplateFileDTO> listByGroupId(final Serializable groupId) {
        LambdaQueryWrapper<DevTemplate> wrapper = queryWrapper();
        // 树/列表场景不需要正文，避免把全部模板正文拉进内存
        wrapper.select(DevTemplate.class, field -> !"content".equals(field.getProperty()));
        wrapper.eq(DevTemplate::getGroupId, groupId);
        wrapper.orderByAsc(DevTemplate::getSortNum).orderByAsc(DevTemplate::getFilePath);
        return TemplateFileConvert.INSTANCE.toDTO(getBaseMapper().selectList(wrapper));
    }

    @Override
    public List<TemplateFileDTO> listEnabledByGroupId(final Serializable groupId) {
        LambdaQueryWrapper<DevTemplate> wrapper = queryWrapper();
        wrapper.eq(DevTemplate::getGroupId, groupId);
        wrapper.eq(DevTemplate::getStatus, 1);
        wrapper.orderByAsc(DevTemplate::getSortNum).orderByAsc(DevTemplate::getFilePath);
        return TemplateFileConvert.INSTANCE.toDTO(getBaseMapper().selectList(wrapper));
    }

    @Override
    public Map<Long, Integer> countByGroupIds(final Collection<Long> groupIds) {
        Map<Long, Integer> result = new HashMap<>();
        if (groupIds == null || groupIds.isEmpty()) {
            return result;
        }
        LambdaQueryWrapper<DevTemplate> wrapper = queryWrapper();
        wrapper.select(DevTemplate::getId, DevTemplate::getGroupId);
        wrapper.in(DevTemplate::getGroupId, groupIds);
        for (DevTemplate entity : getBaseMapper().selectList(wrapper)) {
            result.merge(entity.getGroupId(), 1, Integer::sum);
        }
        return result;
    }

    @Override
    public long countByPath(final Serializable groupId, final String filePath, final Serializable excludeId) {
        LambdaQueryWrapper<DevTemplate> wrapper = queryWrapper();
        wrapper.eq(DevTemplate::getGroupId, groupId);
        wrapper.eq(DevTemplate::getFilePath, filePath);
        wrapper.ne(ObjUtil.isNotEmpty(excludeId), DevTemplate::getId, excludeId);
        return count(wrapper);
    }

    @Override
    public TemplateFileDTO getByPath(final Serializable groupId, final String filePath) {
        if (ObjUtil.isEmpty(groupId) || StrUtil.isBlank(filePath)) {
            return null;
        }
        LambdaQueryWrapper<DevTemplate> wrapper = queryWrapper();
        wrapper.eq(DevTemplate::getGroupId, groupId);
        wrapper.eq(DevTemplate::getFilePath, filePath);
        Page<DevTemplate> page = new Page<>(1, 1, false);
        List<DevTemplate> list = getBaseMapper().selectList(page, wrapper);
        return list.isEmpty() ? null : TemplateFileConvert.INSTANCE.toDTO(list.getFirst());
    }

    @Override
    public boolean deleteByGroupId(final Serializable groupId) {
        LambdaQueryWrapper<DevTemplate> wrapper = queryWrapper();
        wrapper.eq(DevTemplate::getGroupId, groupId);
        return remove(wrapper);
    }
}
