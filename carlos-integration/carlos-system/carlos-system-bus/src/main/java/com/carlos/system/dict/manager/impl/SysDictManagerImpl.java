package com.carlos.system.dict.manager.impl;


import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.redis.ICacheManager;
import com.carlos.system.dict.convert.SysDictConvert;
import com.carlos.system.dict.manager.SysDictManager;
import com.carlos.system.dict.mapper.SysDictMapper;
import com.carlos.system.dict.pojo.dto.SysDictDTO;
import com.carlos.system.dict.pojo.entity.SysDict;
import com.carlos.system.dict.pojo.param.SysDictPageParam;
import com.carlos.system.dict.pojo.vo.SysDictVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 系统字典 查询封装实现类
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SysDictManagerImpl extends ServiceImpl<SysDictMapper, SysDict> implements SysDictManager, ICacheManager<SysDictDTO> {

    private final SysDictMapper dictMapper;


    @Override
    public boolean add(SysDictDTO dto) {
        SysDict entity = SysDictConvert.INSTANCE.toDO(dto);
        boolean success = this.save(entity);
        if (!success) {
            log.warn("Insert 'SysDict' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'SysDict' data: id:{}", entity.getId());

        return true;
    }


    @Override
    public Long count(String code, String name, Serializable excludeId) {
        LambdaQueryChainWrapper<SysDict> wrapper = lambdaQuery();
        wrapper.eq(StrUtil.isNotBlank(code), SysDict::getDictCode, code);
        wrapper.eq(StrUtil.isNotBlank(name), SysDict::getDictName, name);
        wrapper.ne(ObjUtil.isNotEmpty(excludeId), SysDict::getId, excludeId);
        return wrapper.count();
    }

    @Override
    public SysDictDTO getDictById(Serializable id) {
        SysDict one = lambdaQuery()
            .select(SysDict::getId,
                SysDict::getDictCode,
                SysDict::getDictName,
                SysDict::getType,
                SysDict::getDescription)
            .eq(SysDict::getId, id)
            .one();
        return SysDictConvert.INSTANCE.toDTO(one);
    }

    @Override
    public IPage<SysDictVO> getPage(SysDictPageParam param) {
        PageInfo<SysDict> page = new PageInfo<>(param);
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(param.getDictName()), SysDict::getDictName, param.getDictName());
        wrapper.like(StrUtil.isNotBlank(param.getDescription()), SysDict::getDescription, param.getDescription());
        wrapper.eq(param.getType() != null, SysDict::getType, param.getType());
        wrapper.ge(param.getStart() != null, SysDict::getCreateTime, param.getStart());
        wrapper.le(param.getEnd() != null, SysDict::getCreateTime, param.getEnd());
        wrapper.orderByDesc(SysDict::getId);
        page(page, wrapper);
        return page.copyWithRecords(
            page.getRecords().stream()
                .map(e -> SysDictConvert.INSTANCE.toVO(SysDictConvert.INSTANCE.toDTO(e)))
                .collect(Collectors.toList())
        );
    }

    @Override
    public List<SysDictDTO> listDict(String name) {
        LambdaQueryWrapper<SysDict> wrapper = Wrappers
            .lambdaQuery(SysDict.class)
            .select(SysDict::getId,
                SysDict::getDictCode,
                SysDict::getDictName,
                SysDict::getType,
                SysDict::getDescription);
        if (StringUtils.isNotBlank(name)) {
            wrapper.like(SysDict::getDictName, name);
        }
        List<SysDict> dicts = dictMapper.selectList(wrapper);
        return SysDictConvert.INSTANCE.toDTO(dicts);
    }


    @Override
    public Serializable getIdByCode(String code) {
        LambdaQueryWrapper<SysDict> wrapper = Wrappers.lambdaQuery(SysDict.class).eq(SysDict::getDictCode, code);
        SysDict dict = getOne(wrapper);
        if (dict == null) {
            log.error("字典不存在, code:{}", code);
            return null;
        }
        return dict.getId();
    }

    @Override
    public List<SysDictDTO> listByDictIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<SysDict> wrapper = Wrappers
            .lambdaQuery(SysDict.class)
            .select(SysDict::getId,
                SysDict::getDictCode,
                SysDict::getDictName,
                SysDict::getType,
                SysDict::getDescription);
        wrapper.in(SysDict::getId, ids);
        List<SysDict> dicts = dictMapper.selectList(wrapper);
        return SysDictConvert.INSTANCE.toDTO(dicts);
    }
}
