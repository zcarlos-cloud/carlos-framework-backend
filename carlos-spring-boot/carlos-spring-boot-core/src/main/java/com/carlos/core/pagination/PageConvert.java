package com.carlos.core.pagination;

import java.util.List;

/**
 * <p>
 * 分页对象转换器
 * </p>
 *
 * @author carlos
 * @date 2021/12/17 16:55
 */
@FunctionalInterface
public interface PageConvert<V, D> {

    /**
     * 分页对象内容转换
     *
     * @param items 转换前分页对象
     * @return java.util.List<V> 转换后分页对象
     * @author carlos
     * @date 2021/12/17 16:55
     */
    List<V> convert(List<D> items);
}
