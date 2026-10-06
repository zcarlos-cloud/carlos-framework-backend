package com.carlos.boot.translation.service;

import com.carlos.boot.translation.core.TranslationBatch;
import com.carlos.boot.translation.core.TranslationContext;
import com.carlos.boot.translation.core.TranslationData;
import com.carlos.boot.translation.core.TranslationMetadata;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

/**
 * <p>
 * 翻译服务核心接口
 * </p>
 *
 * @author carlos
 * @date 2025/01/20
 */
public interface TranslationService {

    /**
     * 批量翻译
     *
     * @param batch 翻译批次
     * @return 翻译数据
     */
    TranslationData translate(TranslationBatch batch);

    /**
     * 单对象翻译
     *
     * @param obj 对象
     */
    default void translate(Object obj) {
        translate(obj, TranslationContext.get());
    }

    /**
     * 单对象翻译（带上下文）
     *
     * @param obj     对象
     * @param context 翻译上下文
     */
    default void translate(Object obj, TranslationContext context) {
        if (obj == null) {
            return;
        }

        TranslationMetadata metadata = TranslationMetadata.of(obj.getClass());
        TranslationBatch batch = new TranslationBatch();
        metadata.collect(obj, batch);

        if (!batch.isEmpty()) {
            TranslationData data = translate(batch);
            metadata.fill(obj, data);
        }
    }

    /**
     * 集合翻译
     *
     * @param collection 集合
     */
    default void translateCollection(Collection<?> collection) {
        translateCollection(collection, TranslationContext.get());
    }

    /**
     * 集合翻译（带上下文）
     *
     * @param collection 集合
     * @param context    翻译上下文
     */
    default void translateCollection(Collection<?> collection, TranslationContext context) {
        if (CollectionUtils.isEmpty(collection)) {
            return;
        }

        // 按类型分组，同类型批量处理
        Map<Class<?>, List<Object>> groups = new HashMap<>();
        for (Object obj : collection) {
            if (obj != null) {
                groups.computeIfAbsent(obj.getClass(), k -> new ArrayList<>()).add(obj);
            }
        }

        for (Map.Entry<Class<?>, List<Object>> entry : groups.entrySet()) {
            Class<?> clazz = entry.getKey();
            List<Object> list = entry.getValue();

            TranslationMetadata metadata = TranslationMetadata.of(clazz);
            TranslationBatch batch = new TranslationBatch();

            // 收集所有对象的翻译ID
            for (Object obj : list) {
                metadata.collect(obj, batch);
            }

            // 批量查询
            if (!batch.isEmpty()) {
                TranslationData data = translate(batch);

                // 填充结果
                for (Object obj : list) {
                    metadata.fill(obj, data);
                }
            }
        }
    }
}
