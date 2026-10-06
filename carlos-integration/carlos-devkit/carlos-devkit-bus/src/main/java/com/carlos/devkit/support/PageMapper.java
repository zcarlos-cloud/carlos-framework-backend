package com.carlos.devkit.support;

import com.carlos.core.pagination.Paging;

import java.util.List;
import java.util.function.Function;

/**
 * <p>
 * 分页结果转换器
 * </p>
 * <p>
 * 把 {@code Paging<DTO>} 映射为 {@code Paging<VO>}，保留分页元信息（Controller 层专项使用）。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
public final class PageMapper {

    private PageMapper() {
    }

    /**
     * 分页结果映射
     *
     * @param source 源分页（承载 DTO）
     * @param mapper 元素映射函数
     * @param <D>    源元素类型
     * @param <V>    目标元素类型
     * @return 目标分页（承载 VO）
     */
    public static <D, V> Paging<V> map(final Paging<D> source, final Function<D, V> mapper) {
        Paging<V> target = new Paging<>();
        if (source == null) {
            target.setRecords(List.of());
            return target;
        }
        target.setTotal(source.getTotal());
        target.setCurrent(source.getCurrent());
        target.setSize(source.getSize());
        target.setPages(source.getPages());
        List<D> records = source.getRecords();
        target.setRecords(records == null ? List.of() : records.stream().map(mapper).toList());
        return target;
    }
}
