package com.carlos.fx.codege.enums;

/**
 * <p>
 * 数据库类型枚举
 * </p>
 *
 * @author Carlos
 * @date 2019/12/25 15:19
 * @ModificationHistory Who  When  What ---------     -------------   --------------------------------------
 */

public enum DbTypeEnum {
    /**
     * MySQL
     */
    MYSQL("mysql", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://[ip]:[port]/[databaseName]?useUnicode=true" +
            "&characterEncoding=UTF8", "org.hibernate.dialect.MySQL5InnoDBDialect", new String[]{
            "information_schema",
            "mysql", "performance_schema", "sys"
    }),

    /**
     * PostgreSQL
     */
    POSTGRESQL("postgresql", "org.postgresql.Driver", "jdbc:postgresql://[ip]:[port]/[databaseName]?currentSchema=public" +
        "&stringtype=unspecified", "org.hibernate.dialect.PostgreSQLDialect", new String[]{
        "information_schema",
        "pg_catalog", "pg_toast"
    });


    /**
     * 描述
     */
    private final String describe;
    /**
     * 驱动名称
     */
    private final String driver;
    /**
     * 连接地址
     */
    private final String url;
    /**
     * 方言
     */
    private final String dialect;
    /**
     * 系统自带表格
     */
    private final String[] sysTable;

    DbTypeEnum(String describe, String driver, String url, String dialect, String[] sysTable) {
        this.describe = describe;
        this.driver = driver;
        this.url = url;
        this.dialect = dialect;
        this.sysTable = sysTable;
    }

    public String getDriver() {
        return driver;
    }

    public String getUrl() {
        return url;
    }

    public String getDialect() {
        return dialect;
    }

    public String getDescribe() {
        return describe;
    }

    public String[] getSysTable() {
        return sysTable;
    }
}
