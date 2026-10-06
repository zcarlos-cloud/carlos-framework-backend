package com.carlos.boot.translation.core;

import com.carlos.core.enums.BaseEnum;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 翻译请求批次（收集所有需要翻译的ID）
 * </p>
 *
 * @author carlos
 * @date 2025/01/20
 */
public class TranslationBatch {

    /**
     * 用户ID集合
     */
    private final Set<Serializable> userIds = new HashSet<>();

    /**
     * 字典请求集合 (type -> codes)
     */
    private final Map<String, Set<String>> dictCodes = new HashMap<>();

    /**
     * 部门ID集合
     */
    private final Set<Serializable> deptIds = new HashSet<>();

    /**
     * 区域编码集合
     */
    private final Set<String> regionCodes = new HashSet<>();

    /**
     * 枚举值集合 (enumClass -> codes)
     */
    private final Map<Class<? extends BaseEnum<?>>, Set<Object>> enumValues = new HashMap<>();

    public void addUserId(Serializable id) {
        if (id != null) {
            userIds.add(id);
        }
    }

    public void addDictCode(String type, String code) {
        if (type != null && code != null) {
            dictCodes.computeIfAbsent(type, k -> new HashSet<>()).add(code);
        }
    }

    public void addDeptId(Serializable id) {
        if (id != null) {
            deptIds.add(id);
        }
    }

    public void addRegionCode(String code) {
        if (code != null) {
            regionCodes.add(code);
        }
    }

    public <T extends BaseEnum<?>> void addEnumValue(Class<T> enumClass, Object code) {
        if (enumClass != null && code != null) {
            enumValues.computeIfAbsent(enumClass, k -> new HashSet<>()).add(code);
        }
    }

    public boolean isEmpty() {
        return userIds.isEmpty()
            && dictCodes.isEmpty()
            && deptIds.isEmpty()
            && regionCodes.isEmpty()
            && enumValues.isEmpty();
    }

    public Set<Serializable> getUserIds() {
        return Set.copyOf(userIds);
    }

    public Map<String, Set<String>> getDictCodes() {
        Map<String, Set<String>> result = new HashMap<>();
        dictCodes.forEach((k, v) -> result.put(k, Set.copyOf(v)));
        return Map.copyOf(result);
    }

    public Set<Serializable> getDeptIds() {
        return Set.copyOf(deptIds);
    }

    public Set<String> getRegionCodes() {
        return Set.copyOf(regionCodes);
    }

    public Map<Class<? extends BaseEnum<?>>, Set<Object>> getEnumValues() {
        Map<Class<? extends BaseEnum<?>>, Set<Object>> result = new HashMap<>();
        enumValues.forEach((k, v) -> result.put(k, Set.copyOf(v)));
        return Map.copyOf(result);
    }

    @Override
    public String toString() {
        return "TranslationBatch{" +
            "userIds=" + userIds.size() +
            ", dictCodes=" + dictCodes.size() +
            ", deptIds=" + deptIds.size() +
            ", regionCodes=" + regionCodes.size() +
            ", enumValues=" + enumValues.size() +
            '}';
    }
}
