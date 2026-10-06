package com.carlos.devkit.engine.support;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.devkit.config.DevkitProperties;
import com.carlos.devkit.engine.model.ConnectionTestResult;
import com.carlos.devkit.engine.model.TableBaseInfo;
import com.carlos.devkit.engine.model.TargetDatabase;
import com.carlos.devkit.enums.DevkitErrorCode;
import com.carlos.devkit.pojo.enums.DbTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * <p>
 * 目标数据库元数据读取
 * </p>
 * <p>
 * 迁移自 carlos-tools 原 {@code com.carlos.fx.utils.DatabaseUtil}：
 * <ul>
 *   <li>连接串由受管数据源（{@link TargetDatabase}）拼装，不再由前端随请求传入；</li>
 *   <li>补上连接 / 语句超时，避免坏网络把生成线程拖死；</li>
 *   <li><b>只读元数据</b>：仅调用 {@link DatabaseMetaData}，不执行任何用户 SQL。</li>
 * </ul>
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcMetaReader {

    private final DevkitProperties devkitProperties;

    /**
     * 测试目标库连通性
     *
     * @param target 目标数据库
     * @return 测试结果
     */
    public ConnectionTestResult testConnection(final TargetDatabase target) {
        long startTime = System.currentTimeMillis();
        try (Connection connection = openConnection(target)) {
            DatabaseMetaData metaData = connection.getMetaData();
            long cost = System.currentTimeMillis() - startTime;
            return ConnectionTestResult.success(
                metaData.getDatabaseProductName(), metaData.getDatabaseProductVersion(), cost);
        } catch (Exception e) {
            log.warn("数据源连接测试失败: {}:{} - {}", target.getHost(), target.getPort(), e.getMessage());
            return ConnectionTestResult.failure(massageError(e));
        }
    }

    /**
     * 列出目标实例下的库（MySQL）/ schema（PostgreSQL）
     *
     * @param target 目标数据库
     * @return 库 / schema 名称清单
     */
    public List<String> listSchemas(final TargetDatabase target) {
        try (Connection connection = openConnection(target)) {
            DatabaseMetaData metaData = connection.getMetaData();
            String[] sysObjects = target.getDbType().getSysObjects();
            List<String> result = new ArrayList<>();
            if (DbTypeEnum.MYSQL == target.getDbType()) {
                try (ResultSet rs = metaData.getCatalogs()) {
                    while (rs.next()) {
                        String name = rs.getString(1);
                        if (ArrayUtil.contains(sysObjects, name)) {
                            continue;
                        }
                        result.add(name);
                    }
                }
            } else {
                try (ResultSet rs = metaData.getSchemas()) {
                    while (rs.next()) {
                        String name = rs.getString("TABLE_SCHEM");
                        if (ArrayUtil.contains(sysObjects, name) || StrUtil.startWith(name, "pg_")) {
                            continue;
                        }
                        result.add(name);
                    }
                }
            }
            return result;
        } catch (SQLException e) {
            log.error("读取库/schema 清单失败: {}:{}", target.getHost(), target.getPort(), e);
            throw DevkitErrorCode.DATASOURCE_CONNECT_FAILED.exception(massageError(e));
        }
    }

    /**
     * 列出目标 schema 下的表
     *
     * @param target  目标数据库
     * @param schema  库（MySQL）/ schema（PostgreSQL）
     * @param keyword 表名关键字（可空，服务端过滤）
     * @return 表清单
     */
    public List<TableBaseInfo> listTables(final TargetDatabase target, final String schema, final String keyword) {
        try (Connection connection = openConnection(target)) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = DbTypeEnum.MYSQL == target.getDbType() ? schema : connection.getCatalog();
            String schemaArg = DbTypeEnum.MYSQL == target.getDbType() ? null : schema;
            List<TableBaseInfo> list = new ArrayList<>();
            try (ResultSet tables = metaData.getTables(catalog, schemaArg, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    if (StrUtil.containsAny(tableName, "=", "$")) {
                        continue;
                    }
                    if (StrUtil.isNotBlank(keyword)
                        && !StrUtil.containsIgnoreCase(tableName, keyword)) {
                        continue;
                    }
                    if (tableName.toUpperCase().equals(tableName)) {
                        tableName = tableName.toLowerCase();
                    }
                    list.add(new TableBaseInfo(tableName, tables.getString("REMARKS")));
                }
            }
            return list;
        } catch (SQLException e) {
            log.error("读取表清单失败: {}:{} / {}", target.getHost(), target.getPort(), schema, e);
            throw DevkitErrorCode.DATASOURCE_CONNECT_FAILED.exception(massageError(e));
        }
    }

    /**
     * 建立到目标库的连接（调用方负责关闭）
     *
     * @param target 目标数据库
     * @return 数据库连接
     * @throws SQLException 连接失败
     */
    public Connection openConnection(final TargetDatabase target) throws SQLException {
        Properties props = new Properties();
        if (StrUtil.isNotBlank(target.getUsername())) {
            props.setProperty("user", target.getUsername());
        }
        if (target.getPassword() != null) {
            props.setProperty("password", target.getPassword());
        }
        // 让元数据能取到注释
        props.setProperty("remarks", "true");
        props.setProperty("useInformationSchema", "true");
        int timeoutSec = devkitProperties.getMetaTimeoutSec();
        if (DbTypeEnum.MYSQL == target.getDbType()) {
            props.setProperty("connectTimeout", String.valueOf(timeoutSec * 1000L));
            props.setProperty("socketTimeout", String.valueOf(timeoutSec * 1000L));
        } else {
            props.setProperty("connectTimeout", String.valueOf(timeoutSec));
            props.setProperty("socketTimeout", String.valueOf(timeoutSec));
        }
        return DriverManager.getConnection(buildUrl(target), props);
    }

    /**
     * 拼装 JDBC 连接串
     *
     * @param target 目标数据库
     * @return 连接串
     */
    public String buildUrl(final TargetDatabase target) {
        String url = target.getDbType().getUrlTemplate()
            .replace("[ip]", StrUtil.nullToEmpty(target.getHost()))
            .replace("[port]", StrUtil.nullToEmpty(target.getPort()))
            .replace("[databaseName]", StrUtil.nullToEmpty(target.getDatabaseName()));
        if (StrUtil.isNotBlank(target.getJdbcParams())) {
            url = url + "&" + target.getJdbcParams();
        }
        return url;
    }

    /**
     * 释放连接（静默）
     *
     * @param connection 连接
     */
    public void release(final Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            log.warn("关闭数据库连接失败: {}", e.getMessage());
        }
    }

    /**
     * 裁剪异常信息，避免把驱动堆栈里的连接串（含口令）透给前端
     *
     * @param e 异常
     * @return 脱敏后的提示
     */
    private String massageError(final Exception e) {
        String message = e.getMessage();
        if (StrUtil.isBlank(message)) {
            return e.getClass().getSimpleName();
        }
        return message.replaceAll("(?i)password=[^&\\s]*", "password=***");
    }
}
