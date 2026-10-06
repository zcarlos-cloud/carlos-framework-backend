package com.carlos.datasource.base;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * <p>
 * 批量操作服务接口
 * </p>
 *
 * <p>
 * 提供高性能的批量数据操作方法，支持大数据量分批处理、异步执行等特性
 * </p>
 *
 * @author carlos
 * @date 2025/01/15
 * @param <T> 实体类型
 */
public interface BatchService<T> extends IService<T> {

    /**
     * 批量插入（优化版本）
     *
     * <p>
     * 针对大数据量批量插入进行优化：
     * 1. 自动分批处理
     * 2. 支持 rewriteBatchedStatements 优化
     * 3. 支持异步执行
     * </p>
     *
     * @param entityList 实体列表
     * @return 是否成功
     */
    boolean saveBatchOptimized(List<T> entityList);

    /**
     * 分批插入（指定批次大小）
     *
     * @param entityList    实体列表
     * @param partitionSize 每批大小
     * @return 是否成功
     */
    boolean saveBatchPartitioned(List<T> entityList, int partitionSize);

    /**
     * 批量更新（根据 ID，只更新非空字段）
     *
     * @param entityList 实体列表
     * @return 是否成功
     */
    boolean updateBatchByIdSelective(List<T> entityList);

    /**
     * 批量更新（根据 ID，只更新非空字段，指定批次大小）
     *
     * @param entityList    实体列表
     * @param partitionSize 每批大小
     * @return 是否成功
     */
    boolean updateBatchByIdSelective(List<T> entityList, int partitionSize);

    /**
     * 批量更新（根据条件）
     *
     * @param entityList 实体列表
     * @param wrapper    更新条件
     * @return 是否成功
     */
    boolean updateBatch(List<T> entityList, Wrapper<T> wrapper);

    /**
     * 批量 Upsert（插入或更新）
     *
     * <p>
     * 根据主键判断：
     * - 主键存在则更新
     * - 主键不存在则插入
     * </p>
     *
     * @param entityList 实体列表
     * @return 是否成功
     */
    boolean upsertBatch(List<T> entityList);

    /**
     * 批量 Upsert（指定批次大小）
     *
     * @param entityList    实体列表
     * @param partitionSize 每批大小
     * @return 是否成功
     */
    boolean upsertBatch(List<T> entityList, int partitionSize);

    /**
     * 异步批量插入
     *
     * @param entityList 实体列表
     * @return CompletableFuture
     */
    CompletableFuture<Boolean> asyncSaveBatch(List<T> entityList);

    /**
     * 异步批量插入（指定批次大小）
     *
     * @param entityList    实体列表
     * @param partitionSize 每批大小
     * @return CompletableFuture
     */
    CompletableFuture<Boolean> asyncSaveBatch(List<T> entityList, int partitionSize);

    /**
     * 分批处理大数据量列表
     *
     * <p>
     * 将大数据量列表分批处理，避免内存溢出
     * </p>
     *
     * @param dataList      数据列表
     * @param partitionSize 每批大小
     * @param processor     处理器
     * @param <R>           返回类型
     * @return 处理结果列表
     */
    <R> List<R> processInPartitions(List<T> dataList, int partitionSize, Function<List<T>, List<R>> processor);

    /**
     * 获取默认批次大小
     *
     * @return 批次大小
     */
    default int getDefaultBatchSize() {
        return 500;
    }

    /**
     * 获取大批量阈值
     *
     * @return 阈值
     */
    default int getLargeBatchThreshold() {
        return 5000;
    }
}
