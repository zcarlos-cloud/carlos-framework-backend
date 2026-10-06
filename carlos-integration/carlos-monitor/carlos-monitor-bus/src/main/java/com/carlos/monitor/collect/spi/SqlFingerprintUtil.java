package com.carlos.monitor.collect.spi;

import cn.hutool.core.util.StrUtil;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * <p>
 * SQL 指纹工具：Druid SQL 归一化（参数占位化）→ MurmurHash → 指纹
 * 采集侧与查询侧共用，保证同一指纹可聚合
 * </p>
 *
 * @author Carlos
 * @date 2026年10月2日
 */
@Slf4j
public final class SqlFingerprintUtil {

    /**
     * 样例 SQL 截断长度（对齐 CK 注释：样例 SQL 参数脱敏，截断 4000）
     */
    private static final int SAMPLE_SQL_MAX_LENGTH = 4000;

    private SqlFingerprintUtil() {
    }

    /**
     * 计算 SQL 指纹（归一化失败时降级为原文哈希）
     *
     * @param sql    原始 SQL
     * @param dbType Druid 数据库类型（可为 null，按通用方言归一化）
     * @return 指纹（MurmurHash128 hex）
     */
    public static String fingerprint(String sql, DbType dbType) {
        if (StrUtil.isBlank(sql)) {
            return "";
        }
        String normalized = normalize(sql, dbType);
        return Hashing.murmur3_128().hashString(normalized, StandardCharsets.UTF_8).toString();
    }

    /**
     * 归一化 SQL（参数占位化）
     *
     * @param sql    原始 SQL
     * @param dbType Druid 数据库类型（可为 null）
     * @return 归一化后的 SQL
     */
    public static String normalize(String sql, DbType dbType) {
        if (StrUtil.isBlank(sql)) {
            return "";
        }
        try {
            return dbType == null ? SQLUtils.normalize(sql) : SQLUtils.normalize(sql, dbType);
        } catch (Exception e) {
            log.debug("SQL normalize fail, use raw sql, error:{}", e.getMessage());
            return sql;
        }
    }

    /**
     * 样例 SQL（参数占位化 + 截断 4000）
     *
     * @param sql    原始 SQL
     * @param dbType Druid 数据库类型（可为 null）
     * @return 可入库的样例 SQL
     */
    public static String sampleSql(String sql, DbType dbType) {
        return StrUtil.maxLength(normalize(sql, dbType), SAMPLE_SQL_MAX_LENGTH);
    }
}
