package com.carlos.system.news.manager;

import com.carlos.system.news.pojo.entity.SysNewsRead;

import java.util.Collection;
import java.util.Set;

/**
 * <p>
 * 公告已读记录 查询封装接口
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
public interface SysNewsReadManager {

    /**
     * 标记已读（幂等，唯一索引兜底并发，重复标记不报错）
     *
     * @param read 已读记录
     */
    void markRead(SysNewsRead read);

    /**
     * 查询用户在给定公告集合中已读的公告ID
     *
     * @param userId  用户ID
     * @param newsIds 公告ID集合
     * @return 已读的公告ID集合
     */
    Set<Long> getReadNewsIds(Long userId, Collection<Long> newsIds);

    /**
     * 物理删除指定公告的全部已读记录（公告删除时级联清理）
     *
     * @param newsIds 公告ID集合
     */
    void deleteByNewsIds(Collection<Long> newsIds);

}
