package com.carlos.boot.translation.service;

import cn.hutool.extra.spring.SpringUtil;
import com.carlos.boot.translation.cache.CacheKeys;
import com.carlos.boot.translation.cache.TranslationCache;
import com.carlos.boot.translation.core.TranslationBatch;
import com.carlos.boot.translation.core.TranslationContext;
import com.carlos.boot.translation.core.TranslationData;
import com.carlos.core.base.DepartmentInfo;
import com.carlos.core.base.Dict;
import com.carlos.core.base.RegionInfo;
import com.carlos.core.base.UserInfo;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumInfo;
import com.carlos.core.interfaces.ApplicationExtendBatch;
import com.carlos.core.util.EnumUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 翻译服务实现（带多级缓存）
 * </p>
 *
 * @author carlos
 * @date 2025/01/20
 */
@Slf4j
@RequiredArgsConstructor
public class CachedTranslationService implements TranslationService {


    private final TranslationCache cacheManager;

    @Override
    public TranslationData translate(TranslationBatch batch) {
        TranslationContext context = TranslationContext.get();
        TranslationData result = new TranslationData();

        // 1. 翻译用户
        if (!batch.getUserIds().isEmpty()) {
            result.setUsers(translateUsers(batch.getUserIds(), context));
        }

        // 2. 翻译字典
        if (MapUtils.isNotEmpty(batch.getDictCodes())) {
            result.setDicts(translateDicts(batch.getDictCodes(), context));
        }

        // 3. 翻译部门
        if (!batch.getDeptIds().isEmpty()) {
            result.setDepts(translateDepts(batch.getDeptIds(), context));
        }

        // 4. 翻译区域
        if (!batch.getRegionCodes().isEmpty()) {
            result.setRegions(translateRegions(batch.getRegionCodes(), context));
        }

        // 5. 翻译枚举（枚举通常从内存获取，不需要远程查询和缓存）
        if (MapUtils.isNotEmpty(batch.getEnumValues())) {
            result.setEnums(translateEnums(batch.getEnumValues()));
        }

        return result;
    }

    /**
     * 翻译用户信息
     *
     * @param ids     用户ID集合
     * @param context 翻译上下文
     * @return 用户数据
     */
    private Map<Serializable, UserInfo> translateUsers(Set<Serializable> ids, TranslationContext context) {
        Map<Serializable, UserInfo> result = new HashMap<>(ids.size());

        // 如果禁用缓存，直接查询数据库
        if (!context.isCacheEnabled()) {
            log.debug("Cache disabled, query users directly: {}", ids);
            Map<Serializable, UserInfo> dbData = getApplicationExtend().getUserByIds(ids);
            result.putAll(dbData);
            return result;
        }

        Set<Serializable> missIds = new HashSet<>();

        // L1 + L2 缓存查询
        for (Serializable id : ids) {
            String key = CacheKeys.userKey(id);
            UserInfo cached = cacheManager.get(key, UserInfo.class);

            if (cached != null) {
                result.put(id, cached);
            } else {
                missIds.add(id);
            }
        }

        // 批量查询缺失数据
        if (CollectionUtils.isNotEmpty(missIds)) {
            log.debug("Batch query users: {}", missIds);
            Map<Serializable, UserInfo> dbData = getApplicationExtend().getUserByIds(missIds);

            // 回填结果并写入缓存（使用注解配置的缓存时间）
            long timeout = context.getCacheMinutes();
            dbData.forEach((id, user) -> {
                result.put(id, user);
                cacheManager.put(CacheKeys.userKey(id), user, timeout, TimeUnit.MINUTES);
            });
        }

        return result;
    }

    private ApplicationExtendBatch getApplicationExtend() {
        return SpringUtil.getBean(ApplicationExtendBatch.class);
    }

    /**
     * 翻译字典信息
     *
     * @param typeCodes 字典类型和编码映射
     * @param context   翻译上下文
     * @return 字典数据
     */
    private Map<String, Dict> translateDicts(Map<String, Set<String>> typeCodes, TranslationContext context) {
        Map<String, Dict> result = new HashMap<>();

        // 如果禁用缓存，直接查询数据库
        if (!context.isCacheEnabled()) {
            log.debug("Cache disabled, query dicts directly: {}", typeCodes);
            for (Map.Entry<String, Set<String>> entry : typeCodes.entrySet()) {
                String type = entry.getKey();
                Map<String, Dict> dbData = getApplicationExtend().getDictVos(type, entry.getValue());
                dbData.forEach((code, dict) -> result.put(type + ":" + code, dict));
            }
            return result;
        }

        Map<String, Set<String>> missMap = new HashMap<>();

        // 缓存查询
        for (Map.Entry<String, Set<String>> entry : typeCodes.entrySet()) {
            String type = entry.getKey();
            for (String code : entry.getValue()) {
                String key = CacheKeys.dictKey(type, code);
                Dict cached = cacheManager.get(key, Dict.class);

                if (cached != null) {
                    result.put(type + ":" + code, cached);
                } else {
                    missMap.computeIfAbsent(type, k -> new HashSet<>()).add(code);
                }
            }
        }

        // 批量查询
        if (MapUtils.isNotEmpty(missMap)) {
            log.debug("Batch query dicts: {}", missMap);
            long timeout = context.getCacheMinutes();
            for (Map.Entry<String, Set<String>> entry : missMap.entrySet()) {
                String type = entry.getKey();
                Map<String, Dict> dbData = getApplicationExtend().getDictVos(type, entry.getValue());

                dbData.forEach((code, dict) -> {
                    String compositeKey = type + ":" + code;
                    result.put(compositeKey, dict);
                    cacheManager.put(CacheKeys.dictKey(type, code), dict, timeout, TimeUnit.MINUTES);
                });
            }
        }

        return result;
    }

    /**
     * 翻译部门信息
     *
     * @param ids     部门ID集合
     * @param context 翻译上下文
     * @return 部门数据
     */
    private Map<Serializable, DepartmentInfo> translateDepts(Set<Serializable> ids, TranslationContext context) {
        Map<Serializable, DepartmentInfo> result = new HashMap<>(ids.size());

        // 如果禁用缓存，直接查询数据库
        if (!context.isCacheEnabled()) {
            log.debug("Cache disabled, query departments directly: {}", ids);
            Map<Serializable, DepartmentInfo> dbData = getApplicationExtend().getDepartmentByIds(ids);
            result.putAll(dbData);
            return result;
        }

        Set<Serializable> missIds = new HashSet<>();

        // 缓存查询
        for (Serializable id : ids) {
            String key = CacheKeys.deptKey(id);
            DepartmentInfo cached = cacheManager.get(key, DepartmentInfo.class);

            if (cached != null) {
                result.put(id, cached);
            } else {
                missIds.add(id);
            }
        }

        // 批量查询
        if (CollectionUtils.isNotEmpty(missIds)) {
            log.debug("Batch query departments: {}", missIds);
            Map<Serializable, DepartmentInfo> dbData = getApplicationExtend().getDepartmentByIds(missIds);

            long timeout = context.getCacheMinutes();
            dbData.forEach((id, dept) -> {
                result.put(id, dept);
                cacheManager.put(CacheKeys.deptKey(id), dept, timeout, TimeUnit.MINUTES);
            });
        }

        return result;
    }

    /**
     * 翻译区域信息
     *
     * @param codes   区域编码集合
     * @param context 翻译上下文
     * @return 区域数据
     */
    private Map<String, RegionInfo> translateRegions(Set<String> codes, TranslationContext context) {
        Map<String, RegionInfo> result = new HashMap<>(codes.size());

        // 如果禁用缓存，直接查询数据库
        if (!context.isCacheEnabled()) {
            log.debug("Cache disabled, query regions directly: {}", codes);
            Map<String, RegionInfo> dbData = getApplicationExtend().getRegionByCodes(codes);
            result.putAll(dbData);
            return result;
        }

        Set<String> missCodes = new HashSet<>();

        // 缓存查询
        for (String code : codes) {
            String key = CacheKeys.regionKey(code);
            RegionInfo cached = cacheManager.get(key, RegionInfo.class);

            if (cached != null) {
                result.put(code, cached);
            } else {
                missCodes.add(code);
            }
        }

        // 批量查询
        if (CollectionUtils.isNotEmpty(missCodes)) {
            log.debug("Batch query regions: {}", missCodes);
            Map<String, RegionInfo> dbData = getApplicationExtend().getRegionByCodes(missCodes);

            long timeout = context.getCacheMinutes();
            dbData.forEach((code, region) -> {
                result.put(code, region);
                cacheManager.put(CacheKeys.regionKey(code), region, timeout, TimeUnit.MINUTES);
            });
        }

        return result;
    }

    /**
     * 翻译枚举值
     *
     * @param enumValues 枚举值集合
     * @return 枚举数据
     */
    private Map<String, EnumInfo> translateEnums(
        Map<Class<? extends BaseEnum<?>>, Set<Object>> enumValues) {
        Map<String, EnumInfo> result = new HashMap<>();

        for (Map.Entry<Class<? extends BaseEnum<?>>, Set<Object>> entry : enumValues.entrySet()) {
            Class<? extends BaseEnum<?>> enumClass = entry.getKey();
            Set<Object> codes = entry.getValue();

            for (Object code : codes) {
                try {
                    // 使用 EnumUtil 根据 code 获取枚举实例
                    BaseEnum<?> enumItem = EnumUtil.getByCode(enumClass, code);
                    if (enumItem != null) {
                        EnumInfo vo = enumItem.getEnumVo();
                        String key = enumClass.getName() + ":" + code;
                        result.put(key, vo);
                        log.debug("Translated enum: {} -> {}", key, vo);
                    } else {
                        log.warn("Enum not found: class={}, code={}", enumClass.getSimpleName(), code);
                    }
                } catch (Exception e) {
                    log.error("Error translating enum: class={}, code={}", enumClass.getSimpleName(), code, e);
                }
            }
        }

        log.info("Translated {} enum values from {} types",
            result.size(), enumValues.size());
        return result;
    }
}
