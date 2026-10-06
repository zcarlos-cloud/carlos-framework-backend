package com.carlos.mongodb.base;

import com.carlos.core.pagination.PageConvert;
import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamPage;
import com.mongoplus.conditions.query.LambdaQueryChainWrapper;
import com.mongoplus.model.PageResult;
import com.mongoplus.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * <p>
 * MongoDB 公共 Service 实现类，扩展自 mongo-plus 的 {@link ServiceImpl}
 * </p>
 *
 * <p><strong>对标 MyBatis-Plus BaseServiceImpl：</strong></p>
 * <ul>
 *   <li>继承 mongo-plus {@code ServiceImpl<T>}，获得完整 CRUD + Lambda 查询/更新能力</li>
 *   <li>通过泛型反射自动发现实体类型</li>
 *   <li>注入 {@link MongoTemplate} 用于高级操作（聚合管道、地理空间查询等）</li>
 *   <li>提供 {@link #pageInfo(ParamPage)} 分页查询，返回 Carlos 标准的 {@link Paging}</li>
 * </ul>
 *
 * <p><strong>使用示例：</strong></p>
 * <pre>{@code
 * @Service
 * public class UserServiceImpl extends BaseServiceImpl<User>
 *         implements UserService {
 *
 *     public UserServiceImpl(MongoTemplate mongoTemplate) {
 *         super(mongoTemplate);
 *     }
 *
 *     public List<User> findByName(String name) {
 *         return lambdaQuery()
 *             .eq(User::getUsername, name)
 *             .list();
 *     }
 *
 *     public Paging<User> pageUsers(ParamPage param) {
 *         return pageInfo(param, lambdaQuery().gt(User::getAge, 18));
 *     }
 * }
 * }</pre>
 *
 * @param <T> 实体类型
 * @author Carlos
 * @date 2021/12/22 19:09
 */
@Slf4j
public abstract class BaseServiceImpl<T> extends ServiceImpl<T>
    implements BaseService<T> {

    /**
     * MongoTemplate，用于复杂查询（聚合管道、地理空间查询等）
     */
    protected final MongoTemplate mongoTemplate;

    protected BaseServiceImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * 获取 MongoTemplate（用于复杂查询场景）
     *
     * @return MongoTemplate
     */
    protected MongoTemplate getMongoTemplate() {
        return this.mongoTemplate;
    }

    // ==================== 实体类型 ====================

    @Override
    public Class<T> getEntityClass() {
        return getGenericityClass();
    }

    // ==================== Lambda 构造器（由父类 ServiceImpl/RepositoryImpl 提供实现） ====================

    // ==================== 分页支持 ====================

    @Override
    public Paging<T> pageInfo(ParamPage param) {
        PageResult<T> pageResult = page(
            (int) param.getCurrent(),
            (int) param.getSize()
        );
        return convertPageResult(pageResult);
    }

    @Override
    public Paging<T> pageInfo(ParamPage param, LambdaQueryChainWrapper<T> queryChainWrapper) {
        PageResult<T> pageResult = page(
            queryChainWrapper,
            (int) param.getCurrent(),
            (int) param.getSize()
        );
        return convertPageResult(pageResult);
    }

    @Override
    public <V> Paging<V> pageInfoConvert(ParamPage param, PageConvert<V, T> convert) {
        Paging<T> paging = pageInfo(param);
        Paging<V> result = new Paging<>();
        result.setTotal(paging.getTotal());
        result.setRecords(convert.convert(paging.getRecords()));
        result.setCurrent(paging.getCurrent());
        result.setSize(paging.getSize());
        result.setPages(paging.getPages());
        return result;
    }

    @Override
    public <V> Paging<V> pageInfoConvert(ParamPage param, LambdaQueryChainWrapper<T> queryChainWrapper,
                                         PageConvert<V, T> convert) {
        Paging<T> paging = pageInfo(param, queryChainWrapper);
        Paging<V> result = new Paging<>();
        result.setTotal(paging.getTotal());
        result.setRecords(convert.convert(paging.getRecords()));
        result.setCurrent(paging.getCurrent());
        result.setSize(paging.getSize());
        result.setPages(paging.getPages());
        return result;
    }
}
