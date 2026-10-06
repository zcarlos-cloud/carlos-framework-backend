package com.carlos.datasource.pagination;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 游标分页信息（用于大数据量分页优化）
 * </p>
 *
 * <p>
 * 使用游标（上次查询的最后一条记录ID）代替 OFFSET，避免大偏移量性能问题
 * </p>
 *
 * @author carlos
 * @date 2025/01/15
 * @param <T>  实体类型
 * @param <ID> ID类型
 */
@Data
public class CursorPageInfo<T, ID extends Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 游标（上一页最后一条记录的ID）
     */
    private ID cursor;

    /**
     * 页大小
     */
    private long size;

    /**
     * 是否还有更多数据
     */
    private boolean hasMore;

    /**
     * 当前页记录
     */
    private List<T> records;

    /**
     * 下一页游标
     */
    private ID nextCursor;

    public CursorPageInfo() {
        this(null, 10);
    }

    public CursorPageInfo(ID cursor, long size) {
        this.cursor = cursor;
        this.size = size;
        this.records = Collections.emptyList();
        this.hasMore = false;
    }

    /**
     * 创建下一页查询对象
     *
     * @return CursorPageInfo
     */
    public CursorPageInfo<T, ID> nextPage() {
        if (!hasMore || nextCursor == null) {
            return null;
        }
        return new CursorPageInfo<>(nextCursor, size);
    }

    /**
     * 判断是否为空
     *
     * @return 是否为空
     */
    public boolean isEmpty() {
        return records == null || records.isEmpty();
    }

    /**
     * 判断是否非空
     *
     * @return 是否非空
     */
    public boolean isNotEmpty() {
        return !isEmpty();
    }
}
