package com.carlos.system.dict.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.system.dict.convert.SysDictItemConvert;
import com.carlos.system.dict.manager.SysDictCacheManager;
import com.carlos.system.dict.manager.SysDictItemManager;
import com.carlos.system.dict.manager.SysDictManager;
import com.carlos.system.dict.pojo.dto.SysDictDTO;
import com.carlos.system.dict.pojo.dto.SysDictItemDTO;
import com.carlos.system.dict.pojo.entity.SysDictItem;
import com.carlos.system.enums.SystemErrorCode;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;


/**
 * <p>
 * 系统字典详情 业务接口实现类
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictItemService {

    private final SysDictItemManager dictItemManager;

    private final SysDictManager dictManager;

    private final SysDictCacheManager cacheManager;


    /**
     * 新增字典选项
     *
     * @param dictId 字典id
     * @param items  字典选项
     * @author carlos
     * @date 2021/12/2 10:22
     */
    @Transactional(rollbackFor = Exception.class)
    public void addDictItem(Serializable dictId, List<SysDictItemDTO> items) {
        if (dictId == null) {
            throw CommonErrorCode.PARAM_MISSING.exception("字典id不能为空");
        }
        SysDictDTO dict = dictManager.getDictById(dictId);
        if (dict == null) {
            throw SystemErrorCode.SYS_DICT_NOT_FOUND.exception();
        }
        if (CollUtil.isEmpty(items)) {
            return;
        }
        for (SysDictItemDTO dto : items) {
            dto.setDictId(Convert.toLong(dictId));
            if (dictItemManager.count(dictId, null, null, dto.getItemCode()) > 0) {
                throw SystemErrorCode.SYS_DICT_ITEM_CODE_ALREADY_EXISTS.exception("字典选项编码已存在");
            }
            if (dictItemManager.count(dictId, null, dto.getItemName(), null) > 0) {
                throw CommonErrorCode.BUSINESS_ERROR.exception("字典选项名称已存在");
            }
            dto.setEnable(true);
            if (dto.getOrderNum() == null) {
                dto.setOrderNum(getNewSort(dictId));
            }
            dictItemManager.add(dto);
        }
        // 添加字典选项缓存
        cacheManager.addItemCache(dict, items);
    }


    /**
     * 保存或修改字典选项
     *
     * @param dict 字典
     * @param items 字典选项
     * @author carlos
     * @date 2021/12/2 13:52
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(SysDictDTO dict, List<SysDictItemDTO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        // 有可能新增或者修改，针对新增，需要补充字段内容
        for (SysDictItemDTO item : items) {
            if (ObjUtil.isEmpty(item.getId())) {
                item.setDictId(dict.getId());
                item.setEnable(true);
                if (item.getOrderNum() == null) {
                    item.setOrderNum(getNewSort(dict.getId()));
                }
                // 添加字典选项
                addDictItem(dict.getId(), List.of(item));
            } else {
                // 修改字典选项
                updateDictItem(item);

            }
        }
        cacheManager.updateItemCache(dict, items);
    }

    /**
     * 修改字典选项信息
     *
     * @param dto 字典选项
     * @return boolean
     * @author carlos
     * @date 2021/12/2 13:52
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDictItem(final SysDictItemDTO dto) {
        final Serializable dictId = dictItemManager.getDictIdById(dto.getId());
        if (dictItemManager.count(dictId, dto.getId(), dto.getItemName(), null) > 0) {
            throw CommonErrorCode.BUSINESS_ERROR.exception("字典选项名称已存在");
        }
        final boolean b = dictItemManager.updateById(SysDictItemConvert.INSTANCE.toDO(dto));
        if (b) {
            cacheManager.updateItemCache(dto);
        }
        return false;
    }


    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDictItem(Set<Long> ids) {
        List<SysDictItemDTO> items = dictItemManager.listByItemIds(ids);
        if (CollUtil.isEmpty(items)) {
            return false;
        }
        Map<Serializable, List<SysDictItemDTO>> map = items.stream().collect(Collectors.groupingBy(SysDictItemDTO::getDictId));
        for (Map.Entry<Serializable, List<SysDictItemDTO>> entry : map.entrySet()) {
            final Serializable dictId = entry.getKey();
            SysDictDTO dict = dictManager.getDictById(dictId);
            final List<SysDictItemDTO> value = entry.getValue();
            deleteDictItem(dict, value.stream().map(SysDictItemDTO::getId).collect(Collectors.toSet()));
        }
        return true;
    }

    /**
     * 删除字典选项
     * @param dict 字典
     * @param ids 字典选项id
     * @return boolean
     * @author carlos
     * @date 2021/12/2 13:50
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDictItem(SysDictDTO dict, Set<Long> ids) {
        dictItemManager.removeByIds(ids);
        cacheManager.deleteItems(dict, ids);
        return true;
    }

    /**
     * 删除字典选项
     *
     * @param dict 字典
     * @author carlos
     * @date 2021/12/2 13:50
     */
    public void deleteByDict(SysDictDTO dict) {
        List<SysDictItemDTO> items = dictItemManager.listItems(Sets.newHashSet(dict.getId()), null, false);
        Set<Long> itemIds = items.stream().map(SysDictItemDTO::getId).collect(Collectors.toSet());
        deleteDictItem(dict, itemIds);
    }

    /**
     * 获取新增字典选项的序号
     *
     * @param dictId 字典id
     * @return java.lang.Long
     * @author carlos
     * @date 2021/11/25 17:21
     */
    public Integer getNewSort(final Serializable dictId) {
        long count = dictItemManager.count(Wrappers.lambdaQuery(SysDictItem.class).eq(SysDictItem::getDictId,
            dictId));
        return Math.toIntExact(++count);
    }


    /**
     * 获取字典所有选项
     *
     * @param code 字典code
     * @param name 选项名称
     * @return com.carlos.sys.pojo.dto.DictItemDTO
     * @author carlos
     * @date 2022/1/6 18:06
     */
    public List<SysDictItemDTO> getItemsByDictCode(final String code, final String name) {
        if (StrUtil.isBlank(name)) {
            List<SysDictItemDTO> itemCache = getItemCache(code);
            // 过滤不可用的
            return itemCache.stream().filter(SysDictItemDTO::getEnable).collect(Collectors.toList());
        }
        final Serializable dictId = dictManager.getIdByCode(code);
        if (dictId == null) {
            throw SystemErrorCode.SYS_DICT_NOT_FOUND.exception("字典不存在:code=" + code);
        }

        // 过滤已关闭的选项
        return dictItemManager.listItems(Sets.newHashSet(dictId), name, true);
    }

    /**
     * 获取字典选项
     *
     * @param dictCode 参数0
     * @return java.util.List<com.carlos.system.dict.pojo.dto.DictItemDTO>
     * @author Carlos
     * @date 2023/4/16 0:46
     */
    public List<SysDictItemDTO> getItemCache(String dictCode) {
        if (StrUtil.isBlank(dictCode)) {
            return Collections.emptyList();
        }
        List<SysDictItemDTO> items = cacheManager.getDictItems(dictCode);
        // 过滤不可用的
        items = items.stream().filter(SysDictItemDTO::getEnable).collect(Collectors.toList());
        return sortDictItems(items);
    }

    /**
     * 字典选项排序
     *
     * @param items 参数0
     * @return java.util.List<com.carlos.system.dict.pojo.dto.SysDictItemDTO>
     * @author Carlos
     * @date 2025-12-15 16:32
     */
    private List<SysDictItemDTO> sortDictItems(List<SysDictItemDTO> items) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        return items.stream().sorted(Comparator.comparingLong(SysDictItemDTO::getOrderNum)).collect(Collectors.toList());
    }

}
