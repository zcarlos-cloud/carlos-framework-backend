package com.carlos.devkit.manager.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.devkit.convert.GenerateRecordConvert;
import com.carlos.devkit.manager.GenerateRecordManager;
import com.carlos.devkit.mapper.DevGenerateRecordMapper;
import com.carlos.devkit.pojo.dto.GenerateRecordDTO;
import com.carlos.devkit.pojo.entity.DevGenerateRecord;
import com.carlos.devkit.pojo.param.GenerateRecordPageParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;

/**
 * <p>
 * 生成记录 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class GenerateRecordManagerImpl extends BaseServiceImpl<DevGenerateRecordMapper, DevGenerateRecord>
    implements GenerateRecordManager {

    @Override
    public boolean add(final GenerateRecordDTO dto) {
        DevGenerateRecord entity = GenerateRecordConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("新增生成记录失败, projectName:{}", dto.getProjectName());
            return false;
        }
        dto.setId(entity.getId());
        return true;
    }

    @Override
    public Paging<GenerateRecordDTO> getPage(final GenerateRecordPageParam param) {
        LambdaQueryWrapper<DevGenerateRecord> wrapper = queryWrapper();
        // 列表不下发参数快照
        wrapper.select(DevGenerateRecord.class, field -> !"gen_params".equals(field.getProperty()));
        wrapper.eq(ObjUtil.isNotNull(param.getBizType()), DevGenerateRecord::getBizType, param.getBizType());
        wrapper.eq(StrUtil.isNotBlank(param.getTemplateGroupCode()),
            DevGenerateRecord::getTemplateGroupCode, param.getTemplateGroupCode());
        wrapper.like(StrUtil.isNotBlank(param.getProjectName()),
            DevGenerateRecord::getProjectName, param.getProjectName());
        wrapper.eq(ObjUtil.isNotNull(param.getStatus()), DevGenerateRecord::getStatus, param.getStatus());
        wrapper.orderByDesc(DevGenerateRecord::getCreateTime);
        return MybatisPage.map(page(pageInfo(param), wrapper), GenerateRecordConvert.INSTANCE::toDTO);
    }

    @Override
    public boolean delete(final Collection<Serializable> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return removeByIds(ids);
    }
}
