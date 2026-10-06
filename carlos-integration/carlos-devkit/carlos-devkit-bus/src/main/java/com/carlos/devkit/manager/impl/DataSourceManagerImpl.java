package com.carlos.devkit.manager.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.devkit.convert.DataSourceConvert;
import com.carlos.devkit.manager.DataSourceManager;
import com.carlos.devkit.mapper.DevDataSourceMapper;
import com.carlos.devkit.pojo.dto.DataSourceDTO;
import com.carlos.devkit.pojo.entity.DevDataSource;
import com.carlos.devkit.pojo.param.DataSourcePageParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 受管数据源 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class DataSourceManagerImpl extends BaseServiceImpl<DevDataSourceMapper, DevDataSource>
    implements DataSourceManager {

    @Override
    public boolean add(final DataSourceDTO dto) {
        DevDataSource entity = DataSourceConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("新增数据源失败, dsName:{}", dto.getDsName());
            return false;
        }
        dto.setId(entity.getId());
        return true;
    }

    @Override
    public boolean modify(final DataSourceDTO dto) {
        boolean success = updateById(DataSourceConvert.INSTANCE.toDO(dto));
        if (!success) {
            log.warn("更新数据源失败, id:{}", dto.getId());
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
    public DataSourceDTO getDtoById(final Serializable id) {
        if (id == null) {
            return null;
        }
        return DataSourceConvert.INSTANCE.toDTO(getBaseMapper().selectById(id));
    }

    @Override
    public Paging<DataSourceDTO> getPage(final DataSourcePageParam param) {
        LambdaQueryWrapper<DevDataSource> wrapper = queryWrapper();
        // 列表不下发密码密文
        wrapper.select(DevDataSource.class, field -> !"password_cipher".equals(field.getColumn()));
        wrapper.like(StrUtil.isNotBlank(param.getDsName()), DevDataSource::getDsName, param.getDsName());
        wrapper.eq(StrUtil.isNotBlank(param.getDbType()), DevDataSource::getDbType, param.getDbType());
        wrapper.eq(ObjUtil.isNotNull(param.getStatus()), DevDataSource::getStatus, param.getStatus());
        wrapper.orderByDesc(DevDataSource::getCreateTime);
        return MybatisPage.map(page(pageInfo(param), wrapper), DataSourceConvert.INSTANCE::toDTO);
    }

    @Override
    public List<DataSourceDTO> listEnabled() {
        LambdaQueryWrapper<DevDataSource> wrapper = queryWrapper();
        wrapper.select(DevDataSource.class, field -> !"password_cipher".equals(field.getColumn()));
        wrapper.eq(DevDataSource::getStatus, 1);
        wrapper.orderByAsc(DevDataSource::getDsName);
        return DataSourceConvert.INSTANCE.toDTO(getBaseMapper().selectList(wrapper));
    }

    @Override
    public Set<Long> listIdsWithPassword(final Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        LambdaQueryWrapper<DevDataSource> wrapper = queryWrapper();
        // 只取主键，绝不把密文带出来
        wrapper.select(DevDataSource::getId);
        wrapper.in(DevDataSource::getId, ids);
        wrapper.isNotNull(DevDataSource::getPasswordCipher);
        wrapper.ne(DevDataSource::getPasswordCipher, "");
        return getBaseMapper().selectList(wrapper).stream()
            .map(DevDataSource::getId)
            .collect(Collectors.toSet());
    }

    @Override
    public long countByName(final String dsName, final Serializable excludeId) {
        LambdaQueryWrapper<DevDataSource> wrapper = queryWrapper();
        wrapper.eq(DevDataSource::getDsName, dsName);
        wrapper.ne(ObjUtil.isNotEmpty(excludeId), DevDataSource::getId, excludeId);
        return count(wrapper);
    }
}
