package com.carlos.devkit.manager.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.devkit.convert.TemplateGroupConvert;
import com.carlos.devkit.manager.TemplateGroupManager;
import com.carlos.devkit.mapper.DevTemplateGroupMapper;
import com.carlos.devkit.pojo.dto.TemplateGroupDTO;
import com.carlos.devkit.pojo.entity.DevTemplateGroup;
import com.carlos.devkit.pojo.param.TemplateGroupPageParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 模板组 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class TemplateGroupManagerImpl extends BaseServiceImpl<DevTemplateGroupMapper, DevTemplateGroup>
    implements TemplateGroupManager {

    @Override
    public boolean add(final TemplateGroupDTO dto) {
        DevTemplateGroup entity = TemplateGroupConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("新增模板组失败, groupCode:{}", dto.getGroupCode());
            return false;
        }
        dto.setId(entity.getId());
        return true;
    }

    @Override
    public boolean modify(final TemplateGroupDTO dto) {
        boolean success = updateById(TemplateGroupConvert.INSTANCE.toDO(dto));
        if (!success) {
            log.warn("更新模板组失败, id:{}", dto.getId());
        }
        return success;
    }

    @Override
    public boolean delete(final Serializable id) {
        if (id == null) {
            log.warn("模板组 id 不能为空");
            return false;
        }
        return removeById(id);
    }

    @Override
    public TemplateGroupDTO getDtoById(final Serializable id) {
        if (id == null) {
            return null;
        }
        return TemplateGroupConvert.INSTANCE.toDTO(getBaseMapper().selectById(id));
    }

    @Override
    public Paging<TemplateGroupDTO> getPage(final TemplateGroupPageParam param) {
        LambdaQueryWrapper<DevTemplateGroup> wrapper = queryWrapper();
        wrapper.like(StrUtil.isNotBlank(param.getGroupName()), DevTemplateGroup::getGroupName, param.getGroupName());
        wrapper.like(StrUtil.isNotBlank(param.getGroupCode()), DevTemplateGroup::getGroupCode, param.getGroupCode());
        wrapper.eq(ObjUtil.isNotNull(param.getTemplateType()),
            DevTemplateGroup::getTemplateType, param.getTemplateType());
        wrapper.eq(StrUtil.isNotBlank(param.getTechStack()), DevTemplateGroup::getTechStack, param.getTechStack());
        wrapper.eq(ObjUtil.isNotNull(param.getStatus()), DevTemplateGroup::getStatus, param.getStatus());
        wrapper.orderByAsc(DevTemplateGroup::getSortNum)
            .orderByDesc(DevTemplateGroup::getCreateTime);

        return MybatisPage.map(page(pageInfo(param), wrapper), TemplateGroupConvert.INSTANCE::toDTO);
    }

    @Override
    public List<TemplateGroupDTO> listByType(final Integer templateType) {
        LambdaQueryWrapper<DevTemplateGroup> wrapper = queryWrapper();
        wrapper.eq(ObjUtil.isNotNull(templateType), DevTemplateGroup::getTemplateType, templateType);
        wrapper.eq(DevTemplateGroup::getStatus, 1);
        wrapper.orderByAsc(DevTemplateGroup::getSortNum)
            .orderByAsc(DevTemplateGroup::getGroupName);
        return TemplateGroupConvert.INSTANCE.toDTO(getBaseMapper().selectList(wrapper));
    }

    @Override
    public long countByCode(final String groupCode, final Serializable excludeId) {
        LambdaQueryWrapper<DevTemplateGroup> wrapper = queryWrapper();
        wrapper.eq(DevTemplateGroup::getGroupCode, groupCode);
        wrapper.ne(ObjUtil.isNotEmpty(excludeId), DevTemplateGroup::getId, excludeId);
        return count(wrapper);
    }
}
