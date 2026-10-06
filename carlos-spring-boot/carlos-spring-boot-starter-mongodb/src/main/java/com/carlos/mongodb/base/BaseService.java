package com.carlos.mongodb.base;

import com.carlos.core.pagination.PageConvert;
import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamPage;
import com.mongoplus.conditions.query.LambdaQueryChainWrapper;
import com.mongoplus.conditions.update.LambdaUpdateChainWrapper;
import com.mongoplus.model.PageResult;
import com.mongoplus.service.IService;
import com.mongoplus.support.SFunction;

/**
 * <p>
 * MongoDB 公共 Service 接口，扩展自 mongo-plus 的 {@link IService}
 * </p>
 *
 * <p><strong>与 MyBatis-Plus 对标：</strong></p>
 * <ul>
 *   <li>{@link #lambdaQuery()} / {@link #queryWrapper()} — 等价于 MyBatis-Plus 的 LambdaQueryWrapper</li>
 *   <li>{@link #lambdaUpdate()} / {@link #updateWrapper()} — 等价于 MyBatis-Plus 的 LambdaUpdateWrapper</li>
 *   <li>{@link #pageInfo(ParamPage)} — 对标 MyBatis-Plus 的分页查询</li>
 *   <li>{@link #field(SFunction)} — Lambda 方式获取 MongoDB 字段名</li>
 * </ul>
 *
 * <p><strong>使用示例：</strong></p>
 * <pre>{@code
 * // Lambda 查询
 * List<User> users = userService.lambdaQuery()
 *     .eq(User::getName, "张三")
 *     .gt(User::getAge, 18)
 *     .list();
 *
 * // 分页查询
 * ParamPage param = new ParamPage();
 * param.setCurrent(1);
 * param.setSize(10);
 * Paging<UserVO> page = userService.pageInfo(param)
 *     .eq(User::getStatus, 1)
 *     .page(voClass);
 * }</pre>
 *
 * @param <T> 实体类型
 * @author Carlos
 * @date 2021/12/22 18:58
 */
public interface BaseService<T> extends IService<T> {

    /**
     * 获取 Lambda 查询构造器（别名，与 queryWrapper 等价）
     *
     * @return LambdaQueryChainWrapper
     */
    @Override
    LambdaQueryChainWrapper<T> lambdaQuery();

    /**
     * 获取 Lambda 更新构造器（别名，与 updateWrapper 等价）
     *
     * @return LambdaUpdateChainWrapper
     */
    @Override
    LambdaUpdateChainWrapper<T> lambdaUpdate();

    /**
     * 获取实体类型
     *
     * @return 实体 Class
     */
    Class<T> getEntityClass();

    /**
     * 获取字段名（根据 Lambda 表达式）
     *
     * @param lambda 属性 Lambda，如 {@code User::getName}
     * @return MongoDB 字段名（驼峰转下划线，并识别 @Field/@ID 注解）
     */
    default String field(SFunction<T, ?> lambda) {
        return lambda.getFieldNameLine();
    }

    // ==================== 分页支持 ====================

    /**
     * 创建分页查询构造器（无条件）
     *
     * @param param 分页参数
     * @return Paging 分页结果
     */
    Paging<T> pageInfo(ParamPage param);

    /**
     * 创建分页查询构造器（带条件）
     *
     * @param param         分页参数
     * @param queryChainWrapper 查询条件
     * @return Paging 分页结果
     */
    Paging<T> pageInfo(ParamPage param, LambdaQueryChainWrapper<T> queryChainWrapper);

    /**
     * 分页查询并转换（无条件）
     *
     * @param param   分页参数
     * @param convert 转换器
     * @param <V>     目标类型
     * @return Paging 分页结果
     */
    <V> Paging<V> pageInfoConvert(ParamPage param, PageConvert<V, T> convert);

    /**
     * 分页查询并转换（带条件）
     *
     * @param param             分页参数
     * @param queryChainWrapper 查询条件
     * @param convert           转换器
     * @param <V>               目标类型
     * @return Paging 分页结果
     */
    <V> Paging<V> pageInfoConvert(ParamPage param, LambdaQueryChainWrapper<T> queryChainWrapper,
                                  PageConvert<V, T> convert);

    /**
     * 将 mongo-plus PageResult 转为 Carlos Paging
     *
     * @param pageResult mongo-plus 分页结果
     * @return Carlos Paging
     */
    default Paging<T> convertPageResult(PageResult<T> pageResult) {
        Paging<T> paging = new Paging<>();
        paging.setTotal((int) pageResult.getTotalSize());
        paging.setRecords(pageResult.getContentData());
        paging.setCurrent((int) pageResult.getPageNum());
        paging.setSize((int) pageResult.getPageSize());
        paging.setPages((int) pageResult.getTotalPages());
        return paging;
    }

    /**
     * 将 mongo-plus PageResult 转为 Carlos Paging（带类型转换）
     *
     * @param pageResult mongo-plus 分页结果
     * @param convert    转换器
     * @param <V>        目标类型
     * @return Carlos Paging
     */
    default <V> Paging<V> convertPageResult(PageResult<T> pageResult, PageConvert<V, T> convert) {
        Paging<V> paging = new Paging<>();
        paging.setTotal((int) pageResult.getTotalSize());
        paging.setRecords(convert.convert(pageResult.getContentData()));
        paging.setCurrent((int) pageResult.getPageNum());
        paging.setSize((int) pageResult.getPageSize());
        paging.setPages((int) pageResult.getTotalPages());
        return paging;
    }
}
