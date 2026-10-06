package com.carlos.system.news.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.system.news.convert.SysNewsConvert;
import com.carlos.system.news.manager.SysNewsManager;
import com.carlos.system.news.mapper.SysNewsMapper;
import com.carlos.system.news.pojo.dto.SysNewsDTO;
import com.carlos.system.news.pojo.entity.SysNews;
import com.carlos.system.news.pojo.enums.SysNewsStatusEnum;
import com.carlos.system.news.pojo.param.SysNewsPageParam;
import com.carlos.system.news.pojo.vo.SysNewsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 公告/站内通知 查询封装实现类
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SysNewsManagerImpl extends ServiceImpl<SysNewsMapper, SysNews> implements SysNewsManager {

    @Override
    public boolean add(SysNewsDTO dto) {
        SysNews entity = SysNewsConvert.INSTANCE.toDO(dto);
        boolean success = this.save(entity);
        if (!success) {
            log.warn("Insert 'SysNews' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        return true;
    }

    @Override
    public boolean delete(Set<Long> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public boolean modify(SysNewsDTO dto) {
        SysNews entity = SysNewsConvert.INSTANCE.toDO(dto);
        return this.updateById(entity);
    }

    @Override
    public SysNewsDTO getDtoById(Serializable id) {
        SysNews entity = this.getById(id);
        return entity == null ? null : SysNewsConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public IPage<SysNewsVO> getPage(SysNewsPageParam param) {
        PageInfo<SysNews> page = new PageInfo<>(param);
        LambdaQueryWrapper<SysNews> wrapper = new LambdaQueryWrapper<>();
        // 列表页不取 content 大字段
        wrapper.select(SysNews.class, field -> !"content".equals(field.getColumn()));
        wrapper.like(StrUtil.isNotBlank(param.getTitle()), SysNews::getTitle, param.getTitle());
        wrapper.eq(param.getNewsType() != null, SysNews::getNewsType, param.getNewsType());
        wrapper.eq(param.getStatus() != null, SysNews::getStatus, param.getStatus());
        // 稳定排序：置顶优先，其次发布时间/创建时间 + 主键兜底
        wrapper.orderByDesc(SysNews::getIsTop);
        wrapper.orderByDesc(SysNews::getPublishTime);
        wrapper.orderByDesc(SysNews::getId);
        page(page, wrapper);
        return page.copyWithRecords(
            page.getRecords().stream()
                .map(e -> SysNewsConvert.INSTANCE.toVO(SysNewsConvert.INSTANCE.toDTO(e)))
                .collect(Collectors.toList())
        );
    }

    @Override
    public List<SysNewsDTO> listPublishedActive() {
        LambdaQueryWrapper<SysNews> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysNews.class, field -> !"content".equals(field.getColumn()));
        wrapper.eq(SysNews::getStatus, SysNewsStatusEnum.PUBLISHED);
        wrapper.and(w -> w.isNull(SysNews::getExpireTime).or().gt(SysNews::getExpireTime, LocalDateTime.now()));
        wrapper.orderByDesc(SysNews::getIsTop);
        wrapper.orderByDesc(SysNews::getPublishTime);
        wrapper.orderByDesc(SysNews::getId);
        return this.list(wrapper).stream()
            .map(SysNewsConvert.INSTANCE::toDTO)
            .collect(Collectors.toList());
    }

}
