package com.carlos.datasource.pagination;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.carlos.core.pagination.PageConvert;
import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamPage;
import com.carlos.datasource.param.ParamPageOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 增强版分页参数封装
 * </p>
 *
 * <p>
 * 特性：
 * 1. 对 MybatisPlus 的分页参数进行二次封装
 * 2. 集成排序字段和查询参数的封装
 * 3. 支持大数据量分页优化
 * 4. 便捷的 VO 转换方法
 * </p>
 *
 * @author carlos
 * @date 2020/4/14 14:30
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class PageInfo<T> extends Page<T> {

    private static final long serialVersionUID = -2211095086394170578L;

    /**
     * 分页字段
     */
    private ParamPage paramPage;

    /**
     * 排序字段映射
     */
    private OrderMapping orderMapping;

    /**
     * 是否优化大页码
     */
    private boolean optimizeLargeOffset = false;

    /**
     * 大页码阈值（默认 10万）
     */
    private long largeOffsetThreshold = 100000;

    /**
     * 最大页大小限制（防止大数据量查询）
     */
    private long maxSize = 10000;

    // region----------------------  构造方法 start  ------------------------

    /**
     * 传入分页参数
     */
    public PageInfo(ParamPage paramPage) {
        this(paramPage, null);
    }

    /**
     * 传入分页参数，排序参数，排序字段映射
     *
     * @param paramPage    分页参数
     * @param orderMapping 字段映射规则
     */
    public PageInfo(ParamPage paramPage, OrderMapping orderMapping) {
        this.paramPage = paramPage;
        this.orderMapping = orderMapping;

        if (paramPage != null) {
            // 校验并设置页大小
            long size = paramPage.getSize();
            if (size > maxSize) {
                log.warn("页大小 {} 超过最大限制 {}，已调整为最大限制", size, maxSize);
                size = maxSize;
            }
            super.setSize(size);
            super.setCurrent(paramPage.getCurrent());
        }

        // 如果不为空则进行字段映射
        if (orderMapping == null) {
            this.orderMapping = new OrderMapping(true);
        }

        // 处理排序
        handleOrderBy(paramPage);
    }

    /**
     * 基础构造方法
     */
    public PageInfo(long current, long size) {
        super(current, size, 0);
        validateSize(size);
    }

    public PageInfo(long current, long size, long total) {
        super(current, size, total, true);
        validateSize(size);
    }

    public PageInfo(long current, long size, boolean isSearchCount) {
        super(current, size, isSearchCount);
        validateSize(size);
    }

    public PageInfo(long current, long size, long total, boolean isSearchCount) {
        super(current, size, total, isSearchCount);
        validateSize(size);
    }
    // endregion----------------------   构造方法 end   ------------------------

    // region----------------------  排序处理 start  ------------------------

    /**
     * 处理排序逻辑
     *
     * @param paramPage 分页参数
     */
    private void handleOrderBy(ParamPage paramPage) {
        if (paramPage instanceof ParamPageOrder pageOrder) {
            if (CollectionUtil.isNotEmpty(pageOrder.getSorts())) {
                // 进行排序字段映射
                this.orderMapping.mappingOrderItems(pageOrder.getSorts());
                super.setOrders(pageOrder.getSorts());
                log.debug("排序字段映射完成: {}",
                    pageOrder.getSorts().stream()
                        .map(OrderItem::getColumn)
                        .collect(Collectors.joining(", ")));
            }
        }
    }

    /**
     * 添加升序排序
     *
     * @param column 排序字段
     * @return PageInfo
     */
    public PageInfo<T> asc(String column) {
        this.addOrder(OrderItem.asc(column));
        return this;
    }

    /**
     * 添加降序排序
     *
     * @param column 排序字段
     * @return PageInfo
     */
    public PageInfo<T> desc(String column) {
        this.addOrder(OrderItem.desc(column));
        return this;
    }

    /**
     * 添加排序字段（自动驼峰转下划线）
     *
     * @param property 属性名（驼峰）
     * @param isAsc    是否升序
     * @return PageInfo
     */
    public PageInfo<T> orderBy(String property, boolean isAsc) {
        if (this.orderMapping != null && this.orderMapping.isUnderLineMode()) {
            String column = property.replaceAll("([A-Z])", "_$1").toLowerCase();
            return isAsc ? asc(column) : desc(column);
        }
        return isAsc ? asc(property) : desc(property);
    }
    // endregion----------------------   排序处理 end   ------------------------

    // region----------------------  大数据量优化 start  ------------------------

    /**
     * 校验页大小
     *
     * @param size 页大小
     */
    private void validateSize(long size) {
        if (size > maxSize) {
            throw new IllegalArgumentException(
                String.format("页大小 %d 超过最大限制 %d", size, maxSize));
        }
    }

    /**
     * 启用大页码优化
     *
     * @return PageInfo
     */
    public PageInfo<T> optimize() {
        this.optimizeLargeOffset = true;
        long offset = (getCurrent() - 1) * getSize();

        if (offset > largeOffsetThreshold) {
            log.warn("当前偏移量 {} 超过阈值 {}，启用流式查询优化", offset, largeOffsetThreshold);
            // 对于超大偏移量，建议：
            // 1. 不统计总数
            // 2. 使用上次查询的最后一条记录的ID作为游标
            this.setSearchCount(false);
        }
        return this;
    }

    /**
     * 使用游标分页（推荐用于大数据量）
     *
     * @param lastId 上一页最后一条记录的ID
     * @param size   页大小
     * @param <ID>   ID类型
     * @return CursorPageInfo
     */
    @SuppressWarnings("unchecked")
    public static <ID extends Serializable, T> CursorPageInfo<T, ID> cursorPage(ID lastId, long size) {
        return new CursorPageInfo<>(lastId, size);
    }

    /**
     * 判断是否需要优化
     *
     * @return 是否需要优化
     */
    public boolean needsOptimization() {
        if (!optimizeLargeOffset) {
            return false;
        }
        long offset = (getCurrent() - 1) * getSize();
        return offset > largeOffsetThreshold;
    }
    // endregion----------------------   大数据量优化 end   ------------------------

    // region----------------------  VO 转换 start  ------------------------

    /**
     * 转换为 VO 分页（简化调用）
     *
     * @param converter 转换函数
     * @param <V>       VO 类型
     * @return Paging
     */
    public <V> Paging<V> toVoPage(Function<List<T>, List<V>> converter) {
        return MybatisPage.convertBy(this, converter);
    }

    /**
     * 转换为 VO 分页（使用 MapStruct 等转换器）
     *
     * @param pageConvert 转换器
     * @param <V>         VO 类型
     * @return Paging
     */
    public <V> Paging<V> toVoPage(PageConvert<V, T> pageConvert) {
        return MybatisPage.convert(this, pageConvert);
    }

    /**
     * 简单复制属性到新分页对象（不转换数据）
     *
     * @param records 新的记录列表
     * @param <E>     新的类型
     * @return IPage
     */
    public <E> IPage<E> copyWithRecords(List<E> records) {
        Page<E> page = new Page<>();
        page.setCurrent(getCurrent());
        page.setSize(getSize());
        page.setTotal(getTotal());
        page.setPages(getPages());
        page.setRecords(records);
        return page;
    }
    // endregion----------------------   VO 转换 end   ------------------------

    // region----------------------  静态工厂方法 start  ------------------------

    /**
     * 创建分页（默认配置）
     *
     * @param current 当前页
     * @param size    页大小
     * @param <T>     实体类型
     * @return PageInfo
     */
    public static <T> PageInfo<T> of(long current, long size) {
        return new PageInfo<>(current, size);
    }

    /**
     * 创建分页（带总数）
     *
     * @param current 当前页
     * @param size    页大小
     * @param total   总数
     * @param <T>     实体类型
     * @return PageInfo
     */
    public static <T> PageInfo<T> of(long current, long size, long total) {
        return new PageInfo<>(current, size, total);
    }

    /**
     * 从 ParamPage 创建
     *
     * @param paramPage 分页参数
     * @param <T>       实体类型
     * @return PageInfo
     */
    public static <T> PageInfo<T> of(ParamPage paramPage) {
        return new PageInfo<>(paramPage);
    }

    /**
     * 从 ParamPage 创建（带排序映射）
     *
     * @param paramPage    分页参数
     * @param orderMapping 排序映射
     * @param <T>          实体类型
     * @return PageInfo
     */
    public static <T> PageInfo<T> of(ParamPage paramPage, OrderMapping orderMapping) {
        return new PageInfo<>(paramPage, orderMapping);
    }

    /**
     * 创建空分页
     *
     * @param <T> 实体类型
     * @return PageInfo
     */
    public static <T> PageInfo<T> empty() {
        PageInfo<T> pageInfo = new PageInfo<>(1, 10, 0);
        pageInfo.setRecords(Collections.emptyList());
        return pageInfo;
    }
    // endregion----------------------   静态工厂方法 end   ------------------------

    // region----------------------  重写方法 start  ------------------------

    @Override
    public Page<T> setSize(long size) {
        validateSize(size);
        return super.setSize(size);
    }
    // endregion----------------------   重写方法 end   ------------------------
}
