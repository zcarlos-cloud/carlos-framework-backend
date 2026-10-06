package com.carlos.system.news.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carlos.system.news.manager.SysNewsReadManager;
import com.carlos.system.news.mapper.SysNewsReadMapper;
import com.carlos.system.news.pojo.entity.SysNewsRead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 公告已读记录 查询封装实现类
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SysNewsReadManagerImpl extends ServiceImpl<SysNewsReadMapper, SysNewsRead> implements SysNewsReadManager {

    @Override
    public void markRead(SysNewsRead read) {
        try {
            this.save(read);
        } catch (DuplicateKeyException e) {
            // 幂等：uk_sys_news_read(news_id, user_id) 唯一索引兜底并发重复标记
            log.debug("公告已读记录已存在，忽略重复标记: newsId={}, userId={}", read.getNewsId(), read.getUserId());
        }
    }

    @Override
    public Set<Long> getReadNewsIds(Long userId, Collection<Long> newsIds) {
        if (userId == null || newsIds == null || newsIds.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<SysNewsRead> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysNewsRead::getNewsId);
        wrapper.eq(SysNewsRead::getUserId, userId);
        wrapper.in(SysNewsRead::getNewsId, newsIds);
        List<SysNewsRead> reads = this.list(wrapper);
        return reads.stream().map(SysNewsRead::getNewsId).collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public void deleteByNewsIds(Collection<Long> newsIds) {
        if (newsIds == null || newsIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SysNewsRead> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysNewsRead::getNewsId, newsIds);
        this.remove(wrapper);
    }

}
