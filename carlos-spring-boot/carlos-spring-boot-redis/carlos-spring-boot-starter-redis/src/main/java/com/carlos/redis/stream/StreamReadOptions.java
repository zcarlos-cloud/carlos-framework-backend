package com.carlos.redis.stream;

import java.time.Duration;

/**
 * 读取选项（不可变）
 * <p>
 * 命名上与 Spring Data 的 {@code StreamReadOptions} 区分，构造时转换，
 * 以免调用方被框架细节污染。
 * </p>
 *
 * @param count        单次读取条数上限
 * @param blockTimeout 阻塞时长；为 null 表示不阻塞（立即返回）
 * @author Carlos
 * @since 3.0.0
 */
public record StreamReadOptions(long count, Duration blockTimeout) {

    /**
     * 按条数与阻塞时长构造
     *
     * @param count        条数
     * @param blockTimeout 阻塞时长
     * @return 选项实例
     */
    public static StreamReadOptions of(long count, Duration blockTimeout) {
        return new StreamReadOptions(count <= 0 ? 1 : count, blockTimeout);
    }

    /**
     * 非阻塞读取
     *
     * @param count 条数
     * @return 选项实例
     */
    public static StreamReadOptions nonBlocking(long count) {
        return of(count, null);
    }
}
