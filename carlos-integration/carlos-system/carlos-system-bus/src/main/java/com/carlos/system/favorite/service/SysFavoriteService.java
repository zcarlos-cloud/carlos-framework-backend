package com.carlos.system.favorite.service;

import com.carlos.security.auth.UserContext;
import com.carlos.security.util.SecurityUtils;
import com.carlos.system.enums.SystemErrorCode;
import com.carlos.system.favorite.manager.SysFavoriteManager;
import com.carlos.system.favorite.pojo.entity.SysFavorite;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 通用用户收藏 业务
 * </p>
 * <p>按 targetType + targetId 收藏任意业务内容，数据按 user_id 强隔离；
 * add/remove 幂等，replace 整体覆盖并维护排序。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysFavoriteService {

    private final SysFavoriteManager favoriteManager;

    /**
     * 我的收藏列表（按 sort_no 升序）
     *
     * @param targetType 业务类型
     * @return 收藏实体列表（Controller 转 VO）
     */
    public List<SysFavorite> listMy(String targetType) {
        return favoriteManager.listByType(currentUser().getUserId(), targetType);
    }

    /**
     * 我的全部收藏（跨类型，收藏管理页集中展示用）
     *
     * @return 收藏实体列表（Controller 转 VO）
     */
    public List<SysFavorite> listAll() {
        return favoriteManager.listByUser(currentUser().getUserId());
    }

    /**
     * 批量校验收藏状态（返回给定目标中已收藏的 targetId）
     *
     * @param targetType 业务类型
     * @param targetIds  目标标识集合
     * @return 已收藏的目标标识集合
     */
    public Set<String> checkFavorited(String targetType, Collection<String> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return Collections.emptySet();
        }
        return favoriteManager.listFavoritedIds(currentUser().getUserId(), targetType, targetIds);
    }

    /**
     * 覆盖保存某类型的全部收藏（物理删除后按顺序批量插入，sort_no 从 1 开始）
     *
     * @param targetType 业务类型
     * @param targetIds  目标标识列表（会去重保序）
     * @return true-成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean replace(String targetType, List<String> targetIds) {
        UserContext user = currentUser();
        List<String> distinctIds = targetIds.stream().distinct().collect(Collectors.toList());
        favoriteManager.replaceFavorites(user.getUserId(),
            user.getTenantId() == null ? 0L : user.getTenantId(), targetType, distinctIds);
        log.info("覆盖收藏成功：userId={}, targetType={}, count={}", user.getUserId(), targetType, distinctIds.size());
        return true;
    }

    /**
     * 添加收藏（幂等，重复添加静默成功）
     *
     * @param targetType 业务类型
     * @param targetId   目标标识
     * @return true-成功
     */
    public boolean add(String targetType, String targetId) {
        UserContext user = currentUser();
        SysFavorite favorite = new SysFavorite()
            .setUserId(user.getUserId())
            .setTenantId(user.getTenantId() == null ? 0L : user.getTenantId())
            .setTargetType(targetType)
            .setTargetId(targetId)
            .setSortNo(0);
        favoriteManager.addFavorite(favorite);
        return true;
    }

    /**
     * 取消收藏（幂等，记录不存在时静默成功）
     *
     * @param targetType 业务类型
     * @param targetId   目标标识
     * @return true-成功
     */
    public boolean remove(String targetType, String targetId) {
        favoriteManager.removeFavorite(currentUser().getUserId(), targetType, targetId);
        return true;
    }

    private UserContext currentUser() {
        UserContext user = SecurityUtils.getCurrentUser();
        if (user == null || user.getUserId() == null) {
            throw SystemErrorCode.SYS_SYSTEM_ERROR.exception("未获取到登录用户");
        }
        return user;
    }

    private Long currentTenantId() {
        UserContext user = SecurityUtils.getCurrentUser();
        return user != null && user.getTenantId() != null ? user.getTenantId() : 0L;
    }

}
