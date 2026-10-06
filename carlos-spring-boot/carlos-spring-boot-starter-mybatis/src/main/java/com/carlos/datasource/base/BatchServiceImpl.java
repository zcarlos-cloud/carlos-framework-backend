package com.carlos.datasource.base;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carlos.core.exception.DaoException;
import com.carlos.datasource.config.MybatisProperties;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * <p>
 * 批量操作服务实现类
 * </p>
 *
 * @author carlos
 * @date 2025/01/15
 * @param <M> Mapper类型
 * @param <T> 实体类型
 */
@Slf4j
public abstract class BatchServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T>
    implements BatchService<T> {

    @Autowired
    protected MybatisProperties mybatisProperties;

    /**
     * 获取主键值
     *
     * @param entity 实体
     * @return 主键值
     */
    protected abstract Serializable getId(T entity);

    /**
     * 判断实体是否为新记录（无主键或主键为空）
     *
     * @param entity 实体
     * @return 是否为新记录
     */
    protected boolean isNewRecord(T entity) {
        Serializable id = getId(entity);
        if (id == null) {
            return true;
        }
        if (id instanceof Number num) {
            return num.longValue() == 0;
        }
        if (id instanceof String str) {
            return str.isEmpty();
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatchOptimized(List<T> entityList) {
        if (CollectionUtils.isEmpty(entityList)) {
            return true;
        }

        int batchSize = mybatisProperties.getBatch().getBatchSize();
        boolean asyncEnabled = mybatisProperties.getBatch().isAsyncEnabled();
        int asyncThreshold = mybatisProperties.getBatch().getAsyncThreshold();

        // 大数据量使用异步
        if (asyncEnabled && entityList.size() > asyncThreshold) {
            log.info("数据量 {} 超过阈值 {}，使用异步批量插入", entityList.size(), asyncThreshold);
            return asyncSaveBatch(entityList, batchSize).join();
        }

        return saveBatchPartitioned(entityList, batchSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatchPartitioned(List<T> entityList, int partitionSize) {
        if (CollectionUtils.isEmpty(entityList)) {
            return true;
        }

        log.debug("开始批量插入，总条数: {}, 批次大小: {}", entityList.size(), partitionSize);

        // 使用 Guava 的 Lists.partition 进行分批
        List<List<T>> partitions = Lists.partition(entityList, partitionSize);
        int totalSuccess = 0;

        for (int i = 0; i < partitions.size(); i++) {
            List<T> partition = partitions.get(i);
            try {
                boolean success = super.saveBatch(partition, partition.size());
                if (success) {
                    totalSuccess += partition.size();
                    log.debug("第 {}/{} 批插入成功，本批条数: {}", i + 1, partitions.size(), partition.size());
                } else {
                    log.error("第 {}/{} 批插入失败", i + 1, partitions.size());
                    throw new DaoException("批量插入失败");
                }
            } catch (Exception e) {
                log.error("第 {}/{} 批插入异常", i + 1, partitions.size(), e);
                throw new DaoException("批量插入异常", e);
            }
        }

        log.info("批量插入完成，成功条数: {}/{}, 批次: {}", totalSuccess, entityList.size(), partitions.size());
        return totalSuccess == entityList.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatchByIdSelective(List<T> entityList) {
        return updateBatchByIdSelective(entityList, getDefaultBatchSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatchByIdSelective(List<T> entityList, int partitionSize) {
        if (CollectionUtils.isEmpty(entityList)) {
            return true;
        }

        log.debug("开始批量更新（非空字段），总条数: {}, 批次大小: {}", entityList.size(), partitionSize);

        List<List<T>> partitions = Lists.partition(entityList, partitionSize);
        int totalSuccess = 0;

        for (int i = 0; i < partitions.size(); i++) {
            List<T> partition = partitions.get(i);
            try {
                for (T entity : partition) {
                    // 使用 updateById 只更新非空字段
                    int rows = baseMapper.updateById(entity);
                    if (rows > 0) {
                        totalSuccess++;
                    }
                }
                log.debug("第 {}/{} 批更新成功", i + 1, partitions.size());
            } catch (Exception e) {
                log.error("第 {}/{} 批更新异常", i + 1, partitions.size(), e);
                throw new DaoException("批量更新异常", e);
            }
        }

        log.info("批量更新完成，成功条数: {}/{}", totalSuccess, entityList.size());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatch(List<T> entityList, Wrapper<T> wrapper) {
        if (CollectionUtils.isEmpty(entityList)) {
            return true;
        }
        if (wrapper == null) {
            throw new DaoException("updateBatch wrapper must not be null, otherwise it will update all table records");
        }

        log.warn("批量更新（根据条件）可能性能较低，建议谨慎使用，数据量: {}", entityList.size());

        try {
            for (T entity : entityList) {
                baseMapper.update(entity, wrapper);
            }
            return true;
        } catch (Exception e) {
            log.error("批量更新异常", e);
            throw new DaoException("批量更新异常", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean upsertBatch(List<T> entityList) {
        return upsertBatch(entityList, getDefaultBatchSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean upsertBatch(List<T> entityList, int partitionSize) {
        if (CollectionUtils.isEmpty(entityList)) {
            return true;
        }

        log.debug("开始批量 Upsert，总条数: {}, 批次大小: {}", entityList.size(), partitionSize);

        // 分离插入和更新
        List<T> insertList = new ArrayList<>();
        List<T> updateList = new ArrayList<>();

        for (T entity : entityList) {
            if (isNewRecord(entity)) {
                insertList.add(entity);
            } else {
                updateList.add(entity);
            }
        }

        boolean insertSuccess = true;
        boolean updateSuccess = true;

        // 执行插入
        if (!insertList.isEmpty()) {
            insertSuccess = saveBatchPartitioned(insertList, partitionSize);
            log.debug("插入数量: {}, 结果: {}", insertList.size(), insertSuccess);
        }

        // 执行更新
        if (!updateList.isEmpty()) {
            updateSuccess = updateBatchByIdSelective(updateList, partitionSize);
            log.debug("更新数量: {}", updateList.size());
        }

        log.info("批量 Upsert 完成，插入: {}, 更新: {}", insertList.size(), updateList.size());
        return insertSuccess && updateSuccess;
    }

    @Override
    @Async("taskExecutor")
    public CompletableFuture<Boolean> asyncSaveBatch(List<T> entityList) {
        return asyncSaveBatch(entityList, getDefaultBatchSize());
    }

    @Override
    @Async("taskExecutor")
    public CompletableFuture<Boolean> asyncSaveBatch(List<T> entityList, int partitionSize) {
        log.info("开始异步批量插入，数据量: {}", entityList == null ? 0 : entityList.size());
        try {
            boolean result = saveBatchPartitioned(entityList, partitionSize);
            log.info("异步批量插入完成，结果: {}", result);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("异步批量插入异常", e);
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Override
    public <R> List<R> processInPartitions(List<T> dataList, int partitionSize,
                                           Function<List<T>, List<R>> processor) {
        if (CollectionUtils.isEmpty(dataList)) {
            return new ArrayList<>();
        }

        List<List<T>> partitions = Lists.partition(dataList, partitionSize);
        List<R> results = new ArrayList<>();

        for (int i = 0; i < partitions.size(); i++) {
            List<T> partition = partitions.get(i);
            try {
                List<R> partitionResult = processor.apply(partition);
                if (partitionResult != null) {
                    results.addAll(partitionResult);
                }
                log.debug("第 {}/{} 批处理完成，本批条数: {}", i + 1, partitions.size(), partition.size());
            } catch (Exception e) {
                log.error("第 {}/{} 批处理异常", i + 1, partitions.size(), e);
                throw new DaoException("分批处理异常", e);
            }
        }

        return results;
    }

    @Override
    public int getDefaultBatchSize() {
        return mybatisProperties != null ?
            mybatisProperties.getBatch().getBatchSize() : 500;
    }

    @Override
    public int getLargeBatchThreshold() {
        return mybatisProperties != null ?
            mybatisProperties.getBatch().getAsyncThreshold() : 5000;
    }
}
