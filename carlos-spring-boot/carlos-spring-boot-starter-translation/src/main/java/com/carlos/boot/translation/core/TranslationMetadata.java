package com.carlos.boot.translation.core;

import com.carlos.boot.translation.annotation.*;
import com.carlos.boot.translation.field.*;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * 字段翻译元数据（缓存反射信息，提升性能）
 * </p>
 *
 * @author carlos
 * @date 2025/01/20
 */
@Slf4j
public class TranslationMetadata {

    /**
     * 类级别的元数据缓存
     */
    private static final ConcurrentHashMap<Class<?>, TranslationMetadata> CACHE =
        new ConcurrentHashMap<>();

    private final List<TranslatableField> fields;
    private final boolean hasNested;

    private TranslationMetadata(Class<?> clazz) {
        this.fields = parseFields(clazz);
        this.hasNested = fields.stream().anyMatch(f -> f instanceof NestedField);
    }

    /**
     * 获取类的翻译元数据
     *
     * @param clazz 类
     * @return 翻译元数据
     */
    public static TranslationMetadata of(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, TranslationMetadata::new);
    }

    /**
     * 清除缓存
     */
    public static void clearCache() {
        CACHE.clear();
    }

    private List<TranslatableField> parseFields(Class<?> clazz) {
        List<TranslatableField> list = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            // 跳过静态和 final 字段
            if (Modifier.isStatic(field.getModifiers()) ||
                Modifier.isFinal(field.getModifiers())) {
                continue;
            }

            field.setAccessible(true);

            // 解析各种注解
            TransUser userAnno = field.getAnnotation(TransUser.class);
            if (userAnno != null) {
                list.add(new UserField(field, userAnno));
                continue;
            }

            TransDict dictAnno = field.getAnnotation(TransDict.class);
            if (dictAnno != null) {
                list.add(new DictField(field, dictAnno));
                continue;
            }

            TransDept deptAnno = field.getAnnotation(TransDept.class);
            if (deptAnno != null) {
                list.add(new DeptField(field, deptAnno));
                continue;
            }

            TransRegion regionAnno = field.getAnnotation(TransRegion.class);
            if (regionAnno != null) {
                list.add(new RegionField(field, regionAnno));
                continue;
            }

            TransEnum enumAnno = field.getAnnotation(TransEnum.class);
            if (enumAnno != null) {
                list.add(new EnumField(field, enumAnno));
                continue;
            }

            TransNested nestedAnno = field.getAnnotation(TransNested.class);
            if (nestedAnno != null) {
                list.add(new NestedField(field, nestedAnno));
            }
        }

        return List.copyOf(list);
    }

    /**
     * 收集需要翻译的ID
     *
     * @param obj   对象
     * @param batch 批次
     */
    public void collect(Object obj, TranslationBatch batch) {
        if (obj == null) {
            return;
        }

        for (TranslatableField field : fields) {
            try {
                field.collect(obj, batch);
            } catch (Exception e) {
                log.warn("Failed to collect translation id from field: {}", field.getName(), e);
            }
        }
    }

    /**
     * 填充翻译结果
     *
     * @param obj  对象
     * @param data 翻译数据
     */
    public void fill(Object obj, TranslationData data) {
        if (obj == null) {
            return;
        }

        for (TranslatableField field : fields) {
            try {
                field.fill(obj, data);
            } catch (Exception e) {
                log.warn("Failed to fill translation to field: {}", field.getName(), e);
            }
        }
    }

    /**
     * 是否包含嵌套字段
     *
     * @return true/false
     */
    public boolean hasNested() {
        return hasNested;
    }

    /**
     * 获取可翻译字段列表
     *
     * @return 字段列表
     */
    public List<TranslatableField> getFields() {
        return fields;
    }

}
