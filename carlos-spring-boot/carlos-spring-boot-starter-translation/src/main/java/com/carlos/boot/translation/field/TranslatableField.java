package com.carlos.boot.translation.field;

import com.carlos.boot.translation.core.TranslationBatch;
import com.carlos.boot.translation.core.TranslationData;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.Field;

/**
 * 可翻译字段抽象
 */
@Getter
@AllArgsConstructor
public abstract class TranslatableField {
    protected final Field field;
    protected final String name;

    /**
     * 收集ID
     *
     * @param obj   对象
     * @param batch 批次
     * @throws Exception 异常
     */
    public abstract void collect(Object obj, TranslationBatch batch) throws Exception;

    /**
     * 填充翻译结果
     *
     * @param obj  对象
     * @param data 数据
     * @throws Exception 异常
     */
    public abstract void fill(Object obj, TranslationData data) throws Exception;
}
