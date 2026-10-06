package com.carlos.system.favorite.manager;

import com.carlos.system.favorite.pojo.entity.SysFavorite;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 通用用户收藏 查询封装接口
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
public interface SysFavoriteManager {

    /**
     * 查询某用户某类型的全部收藏（按 sort_no 升序，主键兜底稳定排序）
     *
     * @param userId     用户ID
     * @param targetType 业务类型
     * @return 收藏列表
     */
    List<SysFavorite> listByType(Long userId, String targetType);

    /**
     * 查询某用户的全部收藏（跨类型，按类型 + sort_no 升序，主键兜底稳定排序；收藏管理用）
     *
     * @param userId 用户ID
     * @return 收藏列表
     */
    List<SysFavorite> listByUser(Long userId);

    /**
     * 查询给定目标中已被该用户收藏的 targetId 集合（批量校验用）
     *
     * @param userId     用户ID
     * @param targetType 业务类型
     * @param targetIds  目标标识集合
     * @return 已收藏的目标标识集合
     */
    Set<String> listFavoritedIds(Long userId, String targetType, Collection<String> targetIds);

    /**
     * 添加收藏（幂等，uk_sys_favorite 唯一索引兜底并发重复添加）
     *
     * @param favorite 收藏记录（user_id/tenant_id/target_type/target_id 已由 Service 组装）
     */
    void addFavorite(SysFavorite favorite);

    /**
     * 取消收藏（物理删除，记录不存在时静默返回）
     *
     * @param userId     用户ID
     * @param targetType 业务类型
     * @param targetId   目标标识
     */
    void removeFavorite(Long userId, String targetType, String targetId);

    /**
     * 覆盖保存某类型的全部收藏（物理删除该用户该类型全部记录后按顺序批量插入，sort_no 从 1 开始）
     *
     * @param userId     用户ID
     * @param tenantId   租户ID
     * @param targetType 业务类型
     * @param targetIds  目标标识列表（已去重保序，按期望顺序整体覆盖）
     */
    void replaceFavorites(Long userId, Long tenantId, String targetType, List<String> targetIds);

}
