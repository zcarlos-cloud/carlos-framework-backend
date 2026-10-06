package com.carlos.datasource.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.core.exception.DaoException;
import com.carlos.datasource.config.MybatisCommonField;
import jakarta.annotation.Resource;

import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * <p>
 * 公共 Service 实现类（增强版）
 * </p>
 *
 * <p>
 * 继承 BatchServiceImpl，提供完整的 CRUD 和批量操作能力
 * </p>
 *
 * @author carlos
 * @date 2020/4/11 22:59
 */
public abstract class BaseServiceImpl<M extends BaseMapper<T>, T>
    extends BatchServiceImpl<M, T>
    implements BaseService<T> {

    @Resource
    protected MybatisCommonField commonField;


    /**
     * 获取主键值（子类可实现自定义逻辑）
     *
     * 默认通过反射获取 id 字段值，子类可覆盖此方法
     *
     * @param entity 实体
     * @return 主键值
     */
    @Override
    protected Serializable getId(T entity) {
        try {
            // 尝试获取 id 字段的值
            Field idField = getIdField(entity.getClass());
            if (idField != null) {
                idField.setAccessible(true);
                Object id = idField.get(entity);
                return (Serializable) id;
            }
            throw new DaoException("实体 " + entity.getClass().getName() + " 没有 id 字段");
        } catch (IllegalAccessException e) {
            throw new DaoException("无法访问实体 id 字段", e);
        }
    }

    /**
     * 获取 ID 字段（递归查找父类）
     *
     * @param clazz 类
     * @return ID 字段
     */
    private Field getIdField(Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            return null;
        }

        try {
            return clazz.getDeclaredField("id");
        } catch (NoSuchFieldException e) {
            // 尝试父类
            return getIdField(clazz.getSuperclass());
        }
    }
}
