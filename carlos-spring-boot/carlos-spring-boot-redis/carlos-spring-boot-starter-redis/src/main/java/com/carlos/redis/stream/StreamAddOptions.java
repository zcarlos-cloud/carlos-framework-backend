package com.carlos.redis.stream;

/**
 * 写入选项（不可变）
 *
 * @param maxLength       Stream 长度上限（{@code MAXLEN}）；为 null 表示<b>使用全局配置</b>
 * @param approximateTrim 是否近似修剪（{@code ~}）
 * @author Carlos
 * @since 3.0.0
 */
public record StreamAddOptions(Long maxLength, boolean approximateTrim) {

    /**
     * 按长度上限构造（默认近似修剪）
     *
     * @param maxLength 长度上限
     * @return 选项实例
     */
    public static StreamAddOptions maxLen(long maxLength) {
        return new StreamAddOptions(maxLength, true);
    }

    /**
     * 按长度上限与修剪方式构造
     *
     * @param maxLength       长度上限
     * @param approximateTrim 是否近似修剪
     * @return 选项实例
     */
    public static StreamAddOptions of(long maxLength, boolean approximateTrim) {
        return new StreamAddOptions(maxLength, approximateTrim);
    }
}
