package com.carlos.datasource.typehandler;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;

/**
 * <p>
 * Serializable 通用类型处理器
 * </p>
 *
 * <p>
 * 支持多种类型的自动转换：
 * - 数值类型：Long、Integer、Short、BigInteger、BigDecimal
 * - 字符串类型：String
 * - 时间戳类型：Long 类型的时间戳
 * </p>
 *
 * @author Carlos
 * @date 2023/5/27 11:34
 */
@Slf4j
@MappedTypes(Serializable.class)
@MappedJdbcTypes({JdbcType.VARCHAR, JdbcType.BIGINT, JdbcType.INTEGER, JdbcType.NUMERIC})
public class SerializableTypeHandler extends BaseTypeHandler<Serializable> {

    /**
     * 设置非空参数
     *
     * @param ps        PreparedStatement
     * @param i         参数索引
     * @param parameter 参数值
     * @param jdbcType  JDBC类型
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Serializable parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.VARCHAR);
            return;
        }

        try {
            // 根据参数类型选择合适的设置方法
            if (parameter instanceof Long) {
                ps.setLong(i, (Long) parameter);
            } else if (parameter instanceof String) {
                ps.setString(i, (String) parameter);
            } else if (parameter instanceof Integer) {
                ps.setInt(i, (Integer) parameter);
            } else if (parameter instanceof BigInteger) {
                ps.setBigDecimal(i, new BigDecimal((BigInteger) parameter));
            } else if (parameter instanceof BigDecimal) {
                ps.setBigDecimal(i, (BigDecimal) parameter);
            } else if (parameter instanceof Short) {
                ps.setShort(i, (Short) parameter);
            } else if (parameter instanceof java.util.Date) {
                ps.setTimestamp(i, new Timestamp(((java.util.Date) parameter).getTime()));
            } else {
                // 默认转换为字符串
                ps.setString(i, parameter.toString());
            }

            log.debug("设置参数: index={}, value={}, type={}", i, parameter, parameter.getClass().getSimpleName());
        } catch (SQLException e) {
            log.error("设置参数失败: index={}, value={}", i, parameter, e);
            throw e;
        }
    }

    /**
     * 获取可空结果（通过列名）
     *
     * @param rs         结果集
     * @param columnName 列名
     * @return Serializable 值
     */
    @Override
    public Serializable getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        return convertToSerializable(value, columnName);
    }

    /**
     * 获取可空结果（通过列索引）
     *
     * @param rs          结果集
     * @param columnIndex 列索引
     * @return Serializable 值
     */
    @Override
    public Serializable getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Object value = rs.getObject(columnIndex);
        return convertToSerializable(value, String.valueOf(columnIndex));
    }

    /**
     * 获取可空结果（通过存储过程）
     *
     * @param cs          CallableStatement
     * @param columnIndex 列索引
     * @return Serializable 值
     */
    @Override
    public Serializable getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Object value = cs.getObject(columnIndex);
        return convertToSerializable(value, String.valueOf(columnIndex));
    }

    /**
     * 将数据库值转换为 Serializable
     *
     * @param value      数据库值
     * @param columnName 列名（用于日志）
     * @return Serializable 值
     */
    private Serializable convertToSerializable(Object value, String columnName) {
        if (value == null) {
            return null;
        }

        // 如果已经是 Serializable 类型，直接返回
        if (value instanceof Serializable) {
            return (Serializable) value;
        }

        // 转换特殊类型
        if (value instanceof Timestamp) {
            return ((Timestamp) value).getTime();
        }

        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).getTime();
        }

        if (value instanceof java.sql.Time) {
            return ((java.sql.Time) value).getTime();
        }

        // 其他类型转换为字符串
        String stringValue = value.toString();
        log.debug("转换类型: column={}, originalType={}, value={}",
            columnName, value.getClass().getSimpleName(), stringValue);

        return stringValue;
    }
}
