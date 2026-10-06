package com.carlos.mongodb.tool;

import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamPage;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;
import java.util.Optional;

/**
 * <p>
 * MongoDB 工具服务类 — 基于 MongoTemplate 的高级操作封装
 * </p>
 *
 * <p><strong>与 mongo-plus 的关系：</strong>mongo-plus 的
 * {@code BaseMapper}/{@code IService} 已提供日常 CRUD 操作。
 * 本工具类用于 mongo-plus 不直接覆盖的高级场景，如：</p>
 * <ul>
 *   <li>聚合管道（Aggregation Pipeline）</li>
 *   <li>地理空间查询（Geospatial Queries）</li>
 *   <li>细粒度的更新操作（$inc、$push 等）</li>
 *   <li>批量写入（Bulk Operations）</li>
 *   <li>GridFS 文件操作</li>
 * </ul>
 *
 * @author carlos
 * @date 2024/01/15
 */
@RequiredArgsConstructor
public class MongoServiceTool {

    private final MongoTemplate mongoTemplate;

    /**
     * 保存文档
     */
    public <T> T save(T entity) {
        return mongoTemplate.save(entity);
    }

    /**
     * 批量插入
     */
    public <T> List<T> saveAll(List<T> entities) {
        return (List<T>) mongoTemplate.insertAll(entities);
    }

    /**
     * 根据 ID 查询
     */
    public <T, ID> Optional<T> findById(ID id, Class<T> clazz) {
        return Optional.ofNullable(mongoTemplate.findById(id, clazz));
    }

    /**
     * 根据条件查询单条
     */
    public <T> Optional<T> findOne(Query query, Class<T> clazz) {
        return Optional.ofNullable(mongoTemplate.findOne(query, clazz));
    }

    /**
     * 根据条件查询列表
     */
    public <T> List<T> find(Query query, Class<T> clazz) {
        return mongoTemplate.find(query, clazz);
    }

    /**
     * 查询所有
     */
    public <T> List<T> findAll(Class<T> clazz) {
        return mongoTemplate.findAll(clazz);
    }

    /**
     * 查询所有（带排序）
     */
    public <T> List<T> findAll(Class<T> clazz, Sort sort) {
        Query query = new Query().with(sort);
        return mongoTemplate.find(query, clazz);
    }

    /**
     * 分页查询
     */
    public <T> Paging<T> findPage(Query query, ParamPage param, Class<T> clazz) {
        long total = mongoTemplate.count(query, clazz);
        Pageable pageable = PageRequest.of(param.getCurrent() - 1, param.getSize());
        query.with(pageable);
        List<T> records = mongoTemplate.find(query, clazz);

        Paging<T> paging = new Paging<>();
        paging.setCurrent(param.getCurrent());
        paging.setSize(param.getSize());
        paging.setTotal((int) total);
        paging.setRecords(records);
        paging.setPages((int) Math.ceil((double) total / param.getSize()));
        return paging;
    }

    /**
     * 更新多条匹配的记录
     */
    public UpdateResult update(Query query, Update update, Class<?> clazz) {
        return mongoTemplate.updateMulti(query, update, clazz);
    }

    /**
     * 更新第一条匹配的记录
     */
    public UpdateResult updateFirst(Query query, Update update, Class<?> clazz) {
        return mongoTemplate.updateFirst(query, update, clazz);
    }

    /**
     * 根据 ID 更新
     */
    public UpdateResult updateById(Object id, Update update, Class<?> clazz) {
        Query query = Query.query(Criteria.where("_id").is(id));
        return mongoTemplate.updateFirst(query, update, clazz);
    }

    /**
     * 删除操作
     */
    public DeleteResult remove(Query query, Class<?> clazz) {
        return mongoTemplate.remove(query, clazz);
    }

    /**
     * 根据 ID 删除
     */
    public DeleteResult removeById(Object id, Class<?> clazz) {
        Query query = Query.query(Criteria.where("_id").is(id));
        return mongoTemplate.remove(query, clazz);
    }

    /**
     * 删除实体
     */
    public DeleteResult remove(Object entity) {
        return mongoTemplate.remove(entity);
    }

    /**
     * 统计数量（带条件）
     */
    public long count(Query query, Class<?> clazz) {
        return mongoTemplate.count(query, clazz);
    }

    /**
     * 统计所有
     */
    public long count(Class<?> clazz) {
        return mongoTemplate.count(new Query(), clazz);
    }

    /**
     * 判断是否存在
     */
    public boolean exists(Query query, Class<?> clazz) {
        return mongoTemplate.exists(query, clazz);
    }

    // ==================== 聚合管道 ====================

    /**
     * 聚合管道查询（通过输入类型确定集合名）
     *
     * @param aggregation 聚合定义（$match / $group / $project / $sort / $limit 等）
     * @param inputClass  输入集合对应的实体类
     * @param outputClass 输出结果类型
     * @return 聚合结果列表
     */
    public <T> List<T> aggregate(Aggregation aggregation, Class<?> inputClass, Class<T> outputClass) {
        return mongoTemplate.aggregate(aggregation, inputClass, outputClass)
            .getMappedResults();
    }

    /**
     * 聚合管道查询（指定集合名）
     *
     * @param aggregation   聚合定义
     * @param collectionName 集合名称
     * @param outputClass   输出结果类型
     * @return 聚合结果列表
     */
    public <T> List<T> aggregate(Aggregation aggregation, String collectionName, Class<T> outputClass) {
        return mongoTemplate.aggregate(aggregation, collectionName, outputClass)
            .getMappedResults();
    }

    /**
     * 聚合管道查询（返回完整 AggregationResults，包含原始 Document）
     *
     * @param aggregation 聚合定义
     * @param inputClass  输入集合对应的实体类
     * @param outputClass 输出结果类型
     * @return AggregationResults（含 mappedResults + rawResults）
     */
    public <T> AggregationResults<T> aggregateRaw(Aggregation aggregation, Class<?> inputClass, Class<T> outputClass) {
        return mongoTemplate.aggregate(aggregation, inputClass, outputClass);
    }

    /**
     * 获取 MongoTemplate
     */
    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }
}
