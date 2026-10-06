package com.carlos.mongodb.page;

import com.carlos.core.pagination.PageConvert;
import com.carlos.core.pagination.Paging;
import com.mongoplus.model.PageResult;

/**
 * <p>
 * MongoDB 分页工具（适配 mongo-plus {@link PageResult}）
 * </p>
 *
 * <p><strong>使用示例：</strong></p>
 * <pre>{@code
 * // 在 Service 中使用
 * PageResult<User> mongoPage = userService.page(queryWrapper, 1, 10);
 * Paging<UserVO> paging = MongoPage.convert(mongoPage, UserVO::from);
 * }</pre>
 *
 * @author Carlos
 * @date 2021/12/17 17:21
 */
public class MongoPage {

    /**
     * 将 mongo-plus PageResult 转换为 Carlos Paging
     *
     * @param pageResult mongo-plus 分页结果
     * @param <T> 实体类型
     * @return Carlos Paging
     */
    public static <T> Paging<T> convert(PageResult<T> pageResult) {
        Paging<T> paging = new Paging<>();
        paging.setTotal((int) pageResult.getTotalSize());
        paging.setRecords(pageResult.getContentData());
        paging.setCurrent((int) pageResult.getPageNum());
        paging.setSize((int) pageResult.getPageSize());
        paging.setPages((int) pageResult.getTotalPages());
        return paging;
    }

    /**
     * 将 mongo-plus PageResult 转换为 Carlos Paging（带类型转换）
     *
     * @param pageResult mongo-plus 分页结果
     * @param convert    转换器
     * @param <V>        目标类型
     * @param <T>        源类型
     * @return Carlos Paging
     */
    public static <V, T> Paging<V> convert(PageResult<T> pageResult, PageConvert<V, T> convert) {
        Paging<V> paging = new Paging<>();
        paging.setTotal((int) pageResult.getTotalSize());
        paging.setRecords(convert.convert(pageResult.getContentData()));
        paging.setCurrent((int) pageResult.getPageNum());
        paging.setSize((int) pageResult.getPageSize());
        paging.setPages((int) pageResult.getTotalPages());
        return paging;
    }
}
