package com.carlos.core.util;

import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjUtil;
import org.springframework.lang.Nullable;

import java.util.concurrent.*;

/**
 * <p>
 * 线程池工具
 * </p>
 *
 * @author carlos
 * @date 2021/10/18 23:37
 */
public class ExecutorUtil {

    public static final ThreadPoolExecutor POOL = ExecutorUtil.get(8, 15, "default-", 20, null);

    /**
     * 创建线程池
     *
     * @param coreNum    核心线程数
     * @param maxNum     最大线程数
     * @param namePrefix 线程名称前缀
     * @param queueSize  等待队列大小
     * @param handler    队列满时处理策略
     * @return java.util.concurrent.ThreadPoolExecutor
     * @author carlos
     * @date 2021/10/18 23:35
     */
    public static ThreadPoolExecutor get(int coreNum, int maxNum, String namePrefix, int queueSize, @Nullable RejectedExecutionHandler handler) {

        // ThreadPoolExecutor.AbortPolicy:丢弃任务并抛出RejectedExecutionException异常。
        // ThreadPoolExecutor.DiscardPolicy：也是丢弃任务，但是不抛出异常。
        // ThreadPoolExecutor.DiscardOldestPolicy：丢弃队列最前面的任务，然后重新尝试执行任务（重复此过程）
        // ThreadPoolExecutor.CallerRunsPolicy：由调用线程处理该任务
        handler = ObjUtil.defaultIfNull(handler, new ThreadPoolExecutor.CallerRunsPolicy());
        return ExecutorBuilder.create()
            .setCorePoolSize(coreNum)
            .setMaxPoolSize(maxNum)
            .setKeepAliveTime(10, TimeUnit.SECONDS)
            .setThreadFactory(ThreadUtil.createThreadFactoryBuilder().setNamePrefix(namePrefix).build())
            .setWorkQueue(new LinkedBlockingQueue<>(queueSize))
            .setHandler(handler)
            .build();

    }

    public static ThreadPoolExecutor defaultPool() {
        return POOL;
    }

    /**
     * 创建虚拟线程执行器，适用于 I/O 密集型场景。
     *
     * @return 虚拟线程执行器
     */
    public static ExecutorService newVirtualThreadPerTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
