package com.carlos.workflow.convert;

import com.carlos.core.pagination.Paging;

import java.util.List;
import java.util.function.Function;

/**
 * <p>
 * 服务层分页结果到 API 分页结果的转换：只换记录，分页元信息（total/current/size/pages）原样保留。
 * </p>
 * <p>
 * {@code Paging} 不是 MyBatis-Plus 的 {@code IPage}，因此复用不了 {@code MybatisPage#convert}；
 * 这一小段转换在多个端点上完全一致，集中在此避免每个 Controller 各写一遍。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
public final class WorkflowPagingConvert {

    private WorkflowPagingConvert() {
    }

    /**
     * 分页记录转换
     *
     * @param source    服务层分页结果
     * @param converter 记录列表转换函数
     * @param <D>       源记录类型
     * @param <A>       目标记录类型
     * @return Paging&lt;A&gt;
     */
    public static <D, A> Paging<A> convert(Paging<D> source, Function<List<D>, List<A>> converter) {
        Paging<A> target = new Paging<>();
        if (source == null) {
            target.setTotal(0);
            target.setCurrent(1);
            target.setSize(10);
            target.setPages(0);
            target.setRecords(List.of());
            return target;
        }
        target.setTotal(source.getTotal());
        target.setCurrent(source.getCurrent());
        target.setSize(source.getSize());
        target.setPages(source.getPages());
        List<D> records = source.getRecords();
        target.setRecords(records == null ? List.of() : converter.apply(records));
        return target;
    }

}
