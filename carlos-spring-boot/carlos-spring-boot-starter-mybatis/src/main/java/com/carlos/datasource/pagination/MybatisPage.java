package com.carlos.datasource.pagination;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carlos.core.pagination.PageConvert;
import com.carlos.core.pagination.Paging;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * MyBatis 分页工具类
 * </p>
 *
 * <p>
 * 提供分页对象转换工具方法
 * </p>
 *
 * @author carlos
 * @date 2021/12/17 17:21
 */
public class MybatisPage {

    /**
     * 分页对象转换（使用 PageConvert 接口）
     *
     * @param page    原分页信息
     * @param convert 转换器
     * @param <V>     目标类型
     * @param <D>     源类型
     * @return Paging
     */
    public static <V, D> Paging<V> convert(IPage<D> page, PageConvert<V, D> convert) {
        if (page == null) {
            return emptyPaging();
        }

        Paging<V> pageInfo = new Paging<>();
        pageInfo.setTotal((int) page.getTotal());
        pageInfo.setCurrent((int) page.getCurrent());
        pageInfo.setSize((int) page.getSize());
        pageInfo.setPages((int) page.getPages());

        List<D> records = page.getRecords();
        if (records != null && !records.isEmpty()) {
            pageInfo.setRecords(convert.convert(records));
        } else {
            pageInfo.setRecords(Collections.emptyList());
        }

        return pageInfo;
    }

    /**
     * 分页对象转换（使用 Function 函数式接口）
     *
     * @param page      原分页信息
     * @param converter 转换函数
     * @param <V>       目标类型
     * @param <D>       源类型
     * @return Paging
     */
    public static <V, D> Paging<V> convertBy(IPage<D> page, Function<List<D>, List<V>> converter) {
        if (page == null) {
            return emptyPaging();
        }

        Paging<V> pageInfo = new Paging<>();
        pageInfo.setTotal((int) page.getTotal());
        pageInfo.setCurrent((int) page.getCurrent());
        pageInfo.setSize((int) page.getSize());
        pageInfo.setPages((int) page.getPages());

        List<D> records = page.getRecords();
        if (records != null && !records.isEmpty() && converter != null) {
            pageInfo.setRecords(converter.apply(records));
        } else {
            pageInfo.setRecords(Collections.emptyList());
        }

        return pageInfo;
    }

    /**
     * 分页对象转换（使用 Stream 映射）
     *
     * @param page   原分页信息
     * @param mapper 映射函数
     * @param <V>    目标类型
     * @param <D>    源类型
     * @return Paging
     */
    public static <V, D> Paging<V> map(IPage<D> page, Function<D, V> mapper) {
        if (page == null) {
            return emptyPaging();
        }

        return convertBy(page, records ->
            records.stream()
                .map(mapper)
                .collect(Collectors.toList())
        );
    }

    /**
     * 创建空分页
     *
     * @param <V> 类型
     * @return Paging
     */
    public static <V> Paging<V> emptyPaging() {
        Paging<V> pageInfo = new Paging<>();
        pageInfo.setTotal(0);
        pageInfo.setCurrent(1);
        pageInfo.setSize(10);
        pageInfo.setPages(0);
        pageInfo.setRecords(Collections.emptyList());
        return pageInfo;
    }

    /**
     * 将 Paging 转换为 PageInfo（简单包装）
     *
     * @param paging 分页信息
     * @param <T>    类型
     * @return PageInfo
     */
    public static <T> PageInfo<T> toPageInfo(Paging<T> paging) {
        if (paging == null) {
            return PageInfo.empty();
        }

        PageInfo<T> pageInfo = new PageInfo<>(
            paging.getCurrent(),
            paging.getSize(),
            paging.getTotal()
        );
        pageInfo.setRecords(paging.getRecords());
        return pageInfo;
    }
}
