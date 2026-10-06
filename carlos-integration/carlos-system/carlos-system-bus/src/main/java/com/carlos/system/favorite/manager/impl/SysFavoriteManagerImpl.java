package com.carlos.system.favorite.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carlos.system.favorite.manager.SysFavoriteManager;
import com.carlos.system.favorite.mapper.SysFavoriteMapper;
import com.carlos.system.favorite.pojo.entity.SysFavorite;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 通用用户收藏 查询封装实现类
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SysFavoriteManagerImpl extends ServiceImpl<SysFavoriteMapper, SysFavorite> implements SysFavoriteManager {

    @Override
    public List<SysFavorite> listByType(Long userId, String targetType) {
        LambdaQueryWrapper<SysFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFavorite::getUserId, userId);
        wrapper.eq(SysFavorite::getTargetType, targetType);
        wrapper.orderByAsc(SysFavorite::getSortNo);
        wrapper.orderByAsc(SysFavorite::getId);
        return this.list(wrapper);
    }

    @Override
    public List<SysFavorite> listByUser(Long userId) {
        LambdaQueryWrapper<SysFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFavorite::getUserId, userId);
        wrapper.orderByAsc(SysFavorite::getTargetType);
        wrapper.orderByAsc(SysFavorite::getSortNo);
        wrapper.orderByAsc(SysFavorite::getId);
        return this.list(wrapper);
    }

    @Override
    public Set<String> listFavoritedIds(Long userId, String targetType, Collection<String> targetIds) {
        if (userId == null || targetIds == null || targetIds.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<SysFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysFavorite::getTargetId);
        wrapper.eq(SysFavorite::getUserId, userId);
        wrapper.eq(SysFavorite::getTargetType, targetType);
        wrapper.in(SysFavorite::getTargetId, targetIds);
        List<SysFavorite> favorites = this.list(wrapper);
        return favorites.stream().map(SysFavorite::getTargetId).collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public void addFavorite(SysFavorite favorite) {
        try {
            this.save(favorite);
        } catch (DuplicateKeyException e) {
            // 幂等：uk_sys_favorite(user_id, target_type, target_id) 唯一索引兜底并发重复添加
            log.debug("收藏已存在，忽略重复添加: userId={}, targetType={}, targetId={}",
                favorite.getUserId(), favorite.getTargetType(), favorite.getTargetId());
        }
    }

    @Override
    public void removeFavorite(Long userId, String targetType, String targetId) {
        LambdaQueryWrapper<SysFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFavorite::getUserId, userId);
        wrapper.eq(SysFavorite::getTargetType, targetType);
        wrapper.eq(SysFavorite::getTargetId, targetId);
        this.remove(wrapper);
    }

    @Override
    public void replaceFavorites(Long userId, Long tenantId, String targetType, List<String> targetIds) {
        LambdaQueryWrapper<SysFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFavorite::getUserId, userId);
        wrapper.eq(SysFavorite::getTargetType, targetType);
        this.remove(wrapper);
        if (targetIds == null || targetIds.isEmpty()) {
            return;
        }
        List<SysFavorite> favorites = new ArrayList<>(targetIds.size());
        for (int i = 0; i < targetIds.size(); i++) {
            favorites.add(new SysFavorite()
                .setUserId(userId)
                .setTenantId(tenantId)
                .setTargetType(targetType)
                .setTargetId(targetIds.get(i))
                .setSortNo(i + 1));
        }
        this.saveBatch(favorites);
    }

}
