package com.carlos.system.dict.manager.impl;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.system.dict.convert.SysDictItemConvert;
import com.carlos.system.dict.manager.SysDictItemManager;
import com.carlos.system.dict.manager.SysDictManager;
import com.carlos.system.dict.mapper.SysDictItemMapper;
import com.carlos.system.dict.pojo.dto.SysDictItemDTO;
import com.carlos.system.dict.pojo.entity.SysDictItem;
import com.carlos.system.dict.pojo.param.SysDictItemPageParam;
import com.carlos.system.dict.pojo.vo.SysDictItemVO;
import com.carlos.system.enums.SystemErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 系统字典详情 查询封装实现类
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SysDictItemManagerImpl extends ServiceImpl<SysDictItemMapper, SysDictItem> implements SysDictItemManager {


    private final SysDictManager dictManager;


    @Override
    public boolean add(SysDictItemDTO dto) {
        SysDictItem entity = SysDictItemConvert.INSTANCE.toDO(dto);
        boolean success = this.save(entity);
        if (!success) {
            log.warn("Insert 'SysDictItem' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'SysDictItem' data: id:{}", entity.getId());
        return true;
    }


    @Override
    public long count(final Serializable dictId, final Serializable excludeId, final String name, String code) {
        if (ObjectUtil.isEmpty(dictId)) {
            throw CommonErrorCode.PARAM_MISSING.exception("字典ID不能为空");
        }
        if (StringUtils.isBlank(name) && StringUtils.isBlank(code)) {
            throw CommonErrorCode.PARAM_MISSING.exception("字典选项名称或字典选项编码不能为空");
        }
        return lambdaQuery().eq(SysDictItem::getDictId, dictId)
            .eq(StrUtil.isNotBlank(name), SysDictItem::getItemName, name)
            .eq(StrUtil.isNotBlank(code), SysDictItem::getItemCode, code)
            .ne(ObjectUtil.isNotEmpty(excludeId), SysDictItem::getId, excludeId).count();
    }

    @Override
    public SysDictItemDTO getItemById(Serializable id) {
        SysDictItem sysDict = getById(id);
        return SysDictItemConvert.INSTANCE.toDTO(sysDict);
    }

    @Override
    public IPage<SysDictItemVO> getPage(SysDictItemPageParam param) {
        PageInfo<SysDictItem> page = new PageInfo<>(param);
        LambdaQueryWrapper<SysDictItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(param.getItemName()), SysDictItem::getItemName, param.getItemName());
        wrapper.like(StrUtil.isNotBlank(param.getDescription()), SysDictItem::getDescription, param.getDescription());
        wrapper.eq(param.getDictId() != null, SysDictItem::getDictId, param.getDictId());
        wrapper.ge(param.getStart() != null, SysDictItem::getCreateTime, param.getStart());
        wrapper.le(param.getEnd() != null, SysDictItem::getCreateTime, param.getEnd());
        wrapper.orderByDesc(SysDictItem::getId).orderByAsc(SysDictItem::getOrderNum);
        page(page, wrapper);
        return page.copyWithRecords(
            page.getRecords().stream()
                .map(e -> SysDictItemConvert.INSTANCE.toVO(SysDictItemConvert.INSTANCE.toDTO(e)))
                .collect(Collectors.toList())
        );
    }

    @Override
    public Serializable getDictIdById(Serializable id) {
        SysDictItem entity = this.getOne(Wrappers.lambdaQuery(SysDictItem.class).eq(SysDictItem::getId, id).select(SysDictItem::getDictId));
        if (entity == null) {
            throw SystemErrorCode.SYS_DICT_ITEM_NOT_FOUND.exception();
        }
        return entity.getDictId();
    }

    @Override
    public List<SysDictItemDTO> listItems(Set<Serializable> dictIds, String name, boolean onlyEnable) {
        LambdaQueryWrapper<SysDictItem> wrapper = Wrappers.lambdaQuery(SysDictItem.class)
            .orderByAsc(SysDictItem::getOrderNum)
            .select(SysDictItem::getId,
                SysDictItem::getDictId,
                SysDictItem::getItemCode,
                SysDictItem::getItemName,
                SysDictItem::getEnable,
                SysDictItem::getOrderNum,
                SysDictItem::getDescription);

        wrapper.like(StringUtils.isNotBlank(name), SysDictItem::getItemName, name);
        wrapper.eq(onlyEnable, SysDictItem::getEnable, true);
        wrapper.in(CollUtil.isNotEmpty(dictIds), SysDictItem::getDictId, dictIds);
        List<SysDictItem> items = list(wrapper);
        return SysDictItemConvert.INSTANCE.toDTO(items);
    }


    @Override
    public List<SysDictItemDTO> listByDictCode(String dictCode) {
        Serializable dictId = dictManager.getIdByCode(dictCode);
        if (dictId == null) {
            return Collections.emptyList();
        }
        return listItems(CollUtil.newHashSet(dictId), null, true);
    }

    @Override
    public List<SysDictItemDTO> listByItemIds(Set<Long> ids) {
        List<SysDictItem> list = lambdaQuery().in(SysDictItem::getId, ids)
            .select(SysDictItem::getId,
                SysDictItem::getDictId,
                SysDictItem::getItemCode,
                SysDictItem::getItemName,
                SysDictItem::getEnable,
                SysDictItem::getOrderNum,
                SysDictItem::getDescription).list();

        return SysDictItemConvert.INSTANCE.toDTO(list);
    }
}
