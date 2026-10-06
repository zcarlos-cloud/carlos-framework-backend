package com.carlos.gateway.util;

import java.nio.charset.StandardCharsets;

/**
 * <p>
 * MurmurHash3 哈希工具类（32位版本）
 * </p>
 * <p>
 * 特点：速度快、分布均匀、无加密安全性要求场景的首选。
 * 比 MD5/SHA-256 快 5-10 倍，适合缓存键生成、一致性哈希、灰度分流等场景。
 * </p>
 *
 * @author carlos
 * @date 2026/4/10
 * @see <a href="https://en.wikipedia.org/wiki/MurmurHash">MurmurHash</a>
 */
public final class MurmurHash3Util {

    private MurmurHash3Util() {
        // 工具类禁止实例化
    }

    // 默认随机种子（与 GrayReleaseFilter / ResponseCacheFilter 历史实现保持一致）
    private static final int DEFAULT_SEED = 0x9747b28c;

    private static final int C1 = 0xcc9e2d51;
    private static final int C2 = 0x1b873593;
    private static final int R1 = 15;
    private static final int R2 = 13;
    private static final int M = 5;
    private static final int N = 0xe6546b64;

    /**
     * 计算字符串的 MurmurHash3（32位）哈希值，使用默认种子
     *
     * @param key 输入字符串
     * @return 32位哈希值
     */
    public static int hash32(String key) {
        return hash32(key, DEFAULT_SEED);
    }

    /**
     * 计算字符串的 MurmurHash3（32位）哈希值
     *
     * @param key  输入字符串
     * @param seed 随机种子
     * @return 32位哈希值
     */
    public static int hash32(String key, int seed) {
        if (key == null) {
            return 0;
        }
        byte[] data = key.getBytes(StandardCharsets.UTF_8);
        return hash32(data, seed);
    }

    /**
     * 计算字节数组的 MurmurHash3（32位）哈希值
     *
     * @param data 输入字节数组
     * @param seed 随机种子
     * @return 32位哈希值
     */
    public static int hash32(byte[] data, int seed) {
        int length = data.length;
        int hash = seed;

        // 每次处理 4 字节
        int i = 0;
        for (; i + 4 <= length; i += 4) {
            int k = (data[i] & 0xff)
                | ((data[i + 1] & 0xff) << 8)
                | ((data[i + 2] & 0xff) << 16)
                | ((data[i + 3] & 0xff) << 24);

            k *= C1;
            k = Integer.rotateLeft(k, R1);
            k *= C2;

            hash ^= k;
            hash = Integer.rotateLeft(hash, R2);
            hash = hash * M + N;
        }

        // 处理剩余字节
        int k1 = 0;
        switch (length - i) {
            case 3:
                k1 ^= (data[i + 2] & 0xff) << 16;
            case 2:
                k1 ^= (data[i + 1] & 0xff) << 8;
            case 1:
                k1 ^= (data[i] & 0xff);
                k1 *= C1;
                k1 = Integer.rotateLeft(k1, R1);
                k1 *= C2;
                hash ^= k1;
        }

        // 最终化
        hash ^= length;
        hash ^= (hash >>> 16);
        hash *= 0x85ebca6b;
        hash ^= (hash >>> 13);
        hash *= 0xc2b2ae35;
        hash ^= (hash >>> 16);

        return hash;
    }
}
