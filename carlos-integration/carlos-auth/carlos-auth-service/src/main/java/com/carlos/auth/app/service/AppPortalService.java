package com.carlos.auth.app.service;

import cn.hutool.core.collection.CollUtil;
import com.carlos.auth.app.convert.AppPortalConvert;
import com.carlos.auth.app.manager.AppCategoryManager;
import com.carlos.auth.app.manager.AppPortalManager;
import com.carlos.auth.app.manager.AppRoleAccessManager;
import com.carlos.auth.app.pojo.dto.AppCategoryDTO;
import com.carlos.auth.app.pojo.dto.AppPortalAppDTO;
import com.carlos.auth.app.pojo.vo.AppPortalCategoryVO;
import com.carlos.auth.app.pojo.vo.AppPortalGroupVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 应用门户服务
 *
 * @author Carlos
 * @date 2026-07-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppPortalService {

    private final AppPortalManager appPortalManager;
    private final AppCategoryManager appCategoryManager;
    private final AppRoleAccessManager appRoleAccessManager;

    /**
     * 获取当前用户可见的应用列表（按分类分组）
     *
     * @param userRoleCodes 用户角色编码列表
     * @return 按分类分组的应用列表
     */
    public List<AppPortalGroupVO> listMyApps(List<String> userRoleCodes) {
        // 1. 获取所有门户可见的应用
        List<AppPortalAppDTO> allVisibleApps = appPortalManager.listPortalVisibleApps();

        if (CollUtil.isEmpty(allVisibleApps)) {
            return Collections.emptyList();
        }

        // 2. 过滤用户角色权限
        List<AppPortalAppDTO> accessibleApps = filterByRoleAccess(allVisibleApps, userRoleCodes);

        // 3. 获取分类列表
        List<AppCategoryDTO> categories = appCategoryManager.listAll();
        Map<Long, AppCategoryDTO> categoryMap = categories.stream()
                .collect(Collectors.toMap(AppCategoryDTO::getId, c -> c));

        // 4. 按分类分组
        Map<Long, List<AppPortalAppDTO>> groupedApps = accessibleApps.stream()
                .collect(Collectors.groupingBy(
                        app -> app.getCategoryId() != null ? app.getCategoryId() : 0L,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 5. 构建响应
        List<AppPortalGroupVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<AppPortalAppDTO>> entry : groupedApps.entrySet()) {
            Long categoryId = entry.getKey();
            List<AppPortalAppDTO> apps = entry.getValue();

            // 排序
            apps.sort(Comparator.comparing(AppPortalAppDTO::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)));

            // 构建分类信息
            AppPortalCategoryVO categoryVO;
            AppCategoryDTO category = categoryMap.get(categoryId);
            if (category != null) {
                categoryVO = AppPortalConvert.INSTANCE.toCategoryVO(category);
            } else {
                categoryVO = new AppPortalCategoryVO()
                        .setId(0L)
                        .setCode("uncategorized")
                        .setName("未分类")
                        .setIcon("carbon:application");
            }

            result.add(new AppPortalGroupVO()
                    .setCategory(categoryVO)
                    .setApps(AppPortalConvert.INSTANCE.toAppVOList(apps)));
        }

        // 按分类 sortOrder 排序
        result.sort(Comparator.comparing(
                group -> {
                    AppCategoryDTO cat = categoryMap.get(group.getCategory().getId());
                    return cat != null ? cat.getSortOrder() : Integer.MAX_VALUE;
                }
        ));

        return result;
    }

    /**
     * 获取所有分类列表
     */
    public List<AppPortalCategoryVO> listCategories() {
        return AppPortalConvert.INSTANCE.toCategoryVOList(appCategoryManager.listAll());
    }

    /**
     * 根据角色权限过滤应用
     *
     * <p>规则：app_role_access 中无记录的应用 = 所有登录用户可见；
     * 有记录的应用 = 仅匹配角色的用户可见</p>
     */
    private List<AppPortalAppDTO> filterByRoleAccess(List<AppPortalAppDTO> apps, List<String> userRoleCodes) {
        // 获取所有有角色限制的应用ID
        Set<Long> allRestrictedAppIds = new HashSet<>(appRoleAccessManager.getAllRestrictedAppIds());

        // 获取用户角色能访问的受限应用
        Set<Long> userAccessibleRestrictedApps = CollUtil.isEmpty(userRoleCodes)
                ? Collections.emptySet()
                : new HashSet<>(appRoleAccessManager.getAccessibleAppIds(userRoleCodes));

        return apps.stream()
                .filter(app -> {
                    if (!allRestrictedAppIds.contains(app.getId())) {
                        // 没有角色限制 → 所有人可见
                        return true;
                    }
                    // 有角色限制 → 检查用户角色是否匹配
                    return userAccessibleRestrictedApps.contains(app.getId());
                })
                .collect(Collectors.toList());
    }
}
