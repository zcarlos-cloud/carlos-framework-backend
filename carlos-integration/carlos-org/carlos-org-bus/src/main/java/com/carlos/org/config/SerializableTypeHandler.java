package com.carlos.org.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * <p>
 * MyBatis 提供了一个名为 Type Handlers 的功能，它允许开发人员将 Java 数据类型映射到特定于数据库的数据类型。 类型处理程序可用于将 Java 对象转换为可写入数据库的特定数据类型。它们还可用于将特定于数据库的数据类型转换为 Java 对象。
 * MyBatis中有两种类型的类型处理程序： 1. JdbcType 类型处理程序：这些类型处理程序用于在 Java 类型和 JDBC 数据库类型之间进行转换。 2.类型处理程序：这些类型处理程序用于在 Java 类型和特定于数据库的数据类型之间进行转换。 BaseTypeHandler 是
 * MyBatis 中的一个抽象类，您可以扩展它以创建自己的自定义类型处理程序。若要创建自定义类型处理程序，需要实现以下方法：
 * </p>
 *
 * @author Carlos
 * @date 2023/5/27 11:34
 */
public class SerializableTypeHandler extends BaseTypeHandler<Serializable> {

    /**
     * 在PreparedStatement上设置参数的值
     *
     * @param ps        PreparedStatement
     * @param i         参数坐标
     * @param parameter 参数java类型
     * @param jdbcType  对应的jdbc类型
     * @author Carlos
     * @date 2023/5/27 11:34
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Serializable parameter, JdbcType jdbcType) throws SQLException {


    }

    /**
     * 从查询结果获取数据
     *
     * @param rs         查询结果
     * @param columnName 列名称
     * @return java.io.Serializable
     * @author Carlos
     * @date 2023/5/27 11:34
     */
    @Override
    public Serializable getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    /**
     * 从查询结果获取数据
     *
     * @param rs          查询结果
     * @param columnIndex 列下标
     * @return java.io.Serializable
     * @author Carlos
     * @date 2023/5/27 11:34
     */
    @Override
    public Serializable getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return null;
    }


    /**
     * 从回调中获取结果
     *
     * @param cs          回调信息
     * @param columnIndex 参数下标
     * @return java.io.Serializable
     * @author Carlos
     * @date 2023/5/27 11:34
     */
    @Override
    public Serializable getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return null;
    }
}
