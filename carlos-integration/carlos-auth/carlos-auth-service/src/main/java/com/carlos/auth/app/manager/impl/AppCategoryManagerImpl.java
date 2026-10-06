package com.carlos.auth.app.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.auth.app.convert.AppCategoryConvert;
import com.carlos.auth.app.manager.AppCategoryManager;
import com.carlos.auth.app.mapper.AppCategoryMapper;
import com.carlos.auth.app.pojo.dto.AppCategoryDTO;
import com.carlos.auth.app.pojo.entity.AppCategory;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;

/**
 * 应用分类 查询封装实现类
 *
 * @author Carlos
 * @date 2026-07-02
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AppCategoryManagerImpl extends BaseServiceImpl<AppCategoryMapper, AppCategory> implements AppCategoryManager {

    private static final String CACHE_KEY = "auth:app:category:all";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Override
    public boolean add(AppCategoryDTO dto) {
        AppCategory entity = AppCategoryConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (success) {
            dto.setId(entity.getId());
            clearCache();
            log.debug("Insert 'AppCategory' data: id:{}", entity.getId());
        }
        return success;
    }

    @Override
    public boolean delete(Serializable id) {
        boolean success = removeById(id);
        if (success) {
            clearCache();
            log.debug("Remove 'AppCategory' data by id:{}", id);
        }
        return success;
    }

    @Override
    public boolean modify(AppCategoryDTO dto) {
        AppCategory entity = AppCategoryConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (success) {
            clearCache();
            log.debug("Update 'AppCategory' data by id:{}", dto.getId());
        }
        return success;
    }

    @Override
    public List<AppCategoryDTO> listAll() {
        List<AppCategoryDTO> cached = RedisUtil.getValue(CACHE_KEY);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        LambdaQueryWrapper<AppCategory> wrapper = queryWrapper();
        wrapper.orderByAsc(AppCategory::getSortOrder);
        List<AppCategory> list = list(wrapper);
        List<AppCategoryDTO> dtoList = AppCategoryConvert.INSTANCE.toDTO(list);
        if (!dtoList.isEmpty()) {
            RedisUtil.setValue(CACHE_KEY, dtoList, CACHE_TTL);
        }
        return dtoList;
    }

    private void clearCache() {
        RedisUtil.delete(CACHE_KEY);
    }
}
