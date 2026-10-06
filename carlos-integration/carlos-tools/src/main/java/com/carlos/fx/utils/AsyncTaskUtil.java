package com.carlos.fx.utils;

import javafx.application.Platform;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Slf4j
public class AsyncTaskUtil {

    /**
     * 线程池
     * 使用 CachedThreadPool，根据需要创建新线程，空闲线程会被回收
     * 所有线程都是守护线程（daemon），不会阻止 JVM 退出
     */
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 异步执行任务
     * 在后台线程执行 Task，成功或失败后在主线程执行回调
     *
     * <p>执行流程：</p>
     * <ol>
     *   <li>将 Task 提交到线程池，在后台线程执行 call() 方法</li>
     *   <li>如果执行成功，在主线程调用 onSuccess 回调</li>
     *   <li>如果执行失败，在主线程调用 onError 回调</li>
     * </ol>
     *
     * <p>注意事项：</p>
     * <ul>
     *   <li>Task 的 call() 方法在后台线程执行，不能直接更新 UI</li>
     *   <li>回调方法在主线程执行，可以安全地更新 UI</li>
     *   <li>如果 Task 抛出异常，会触发 onError 回调</li>
     * </ul>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * Task<String> task = new Task<>() {
     *     @Override
     *     protected String call() throws Exception {
     *         // 模拟耗时操作
     *         Thread.sleep(2000);
     *         return "操作完成";
     *     }
     * };
     *
     * AsyncTaskUtil.execute(task,
     *     result -> {
     *         // 成功：更新 UI
     *         label.setText(result);
     *     },
     *     error -> {
     *         // 失败：显示错误
     *         DialogUtil.showError("错误", "操作失败", error);
     *     }
     * );
     * }</pre>
     *
     * @param <T>       Task 返回值的类型
     * @param task      要执行的任务
     * @param onSuccess 成功回调，参数是 Task 的返回值，在主线程执行
     * @param onError   失败回调，参数是异常对象，在主线程执行
     */
    public static <T> void execute(
            Task<T> task,
            Consumer<T> onSuccess,
            Consumer<Throwable> onError) {

        // 设置成功回调
        // setOnSucceeded 在 Task 成功完成后触发
        task.setOnSucceeded(event -> {
            if (onSuccess != null) {
                // Platform.runLater 确保回调在主线程执行
                // task.getValue() 获取 Task 的返回值
                Platform.runLater(() -> onSuccess.accept(task.getValue()));
            }
        });

        // 设置失败回调
        // setOnFailed 在 Task 执行失败（抛出异常）后触发
        task.setOnFailed(event -> {
            if (onError != null) {
                // Platform.runLater 确保回调在主线程执行
                // task.getException() 获取 Task 抛出的异常
                Platform.runLater(() -> onError.accept(task.getException()));
            }
        });

        // 将 Task 提交到线程池执行
        executor.submit(task);
    }

    /**
     * 异步执行任务（只提供成功回调）
     * 失败时使用默认的错误处理：打印堆栈跟踪并显示错误对话框
     *
     * <p>这是 execute 方法的简化版本，适用于不需要自定义错误处理的场景。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * Task<List<Data>> task = new Task<>() {
     *     @Override
     *     protected List<Data> call() throws Exception {
     *         return dataService.loadData();
     *     }
     * };
     *
     * // 只提供成功回调，失败时自动显示错误对话框
     * AsyncTaskUtil.execute(task, result -> {
     *     dataList.addAll(result);
     * });
     * }</pre>
     *
     * @param <T>       Task 返回值的类型
     * @param task      要执行的任务
     * @param onSuccess 成功回调，参数是 Task 的返回值，在主线程执行
     */
    public static <T> void execute(Task<T> task, Consumer<T> onSuccess) {
        // 调用完整版本的 execute 方法，提供默认的错误处理
        execute(task, onSuccess, throwable -> {
            // 默认错误处理：打印堆栈跟踪
            log.error("异步任务执行失败", throwable);
            // 显示错误对话框
            DialogUtil.showError("错误", "操作失败", throwable);
        });
    }

    /**
     * 异步执行 Runnable
     * 在后台线程执行 Runnable，不返回结果，不提供回调
     *
     * <p>适用于不需要返回值和回调的简单后台任务。</p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * // 在后台线程执行日志记录
     * AsyncTaskUtil.executeAsync(() -> {
     *     logger.info("用户执行了操作");
     * });
     * }</pre>
     *
     * @param runnable 要执行的任务
     */
    public static void executeAsync(Runnable runnable) {
        executor.submit(runnable);
    }

    /**
     * 在 JavaFX 主线程执行 Runnable
     * 如果当前已经在主线程，直接执行；否则使用 Platform.runLater 切换到主线程
     *
     * <p>JavaFX 的 UI 更新必须在主线程执行。该方法确保 Runnable 在主线程执行，
     * 无论调用时在哪个线程。</p>
     *
     * <p>使用场景：</p>
     * <ul>
     *   <li>在后台线程中需要更新 UI</li>
     *   <li>不确定当前线程是否为主线程</li>
     * </ul>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * // 在后台线程中
     * new Thread(() -> {
     *     String result = doSomething();
     *
     *     // 切换到主线程更新 UI
     *     AsyncTaskUtil.runOnFxThread(() -> {
     *         label.setText(result);
     *     });
     * }).start();
     * }</pre>
     *
     * @param runnable 要在主线程执行的任务
     */
    public static void runOnFxThread(Runnable runnable) {
        // 检查当前是否在 JavaFX 主线程
        if (Platform.isFxApplicationThread()) {
            // 已经在主线程，直接执行
            runnable.run();
        } else {
            // 不在主线程，使用 Platform.runLater 切换到主线程
            Platform.runLater(runnable);
        }
    }

    /**
     * 关闭线程池
     * 停止接受新任务，等待已提交的任务执行完成
     *
     * <p>注意：通常不需要手动调用此方法，因为线程池中的线程都是守护线程，
     * 会在 JVM 退出时自动终止。</p>
     *
     * <p>使用场景：应用程序退出前，需要确保所有后台任务完成。</p>
     */
    public static void shutdown() {
        executor.shutdown();
    }
}
