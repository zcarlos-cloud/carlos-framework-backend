package com.carlos.devkit.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import com.carlos.core.response.CommonErrorCode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * <p>
 * 目标数据库类型枚举（{@code dev_datasource.db_type}）
 * </p>
 * <p>
 * 承载字段为 Java 枚举类型（库表 VARCHAR 存码值），JSON 下发常量名。
 * 驱动名与 URL 模板沿用 carlos-tools 原 {@code DbTypeEnum}，模板迁移零成本。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@AppEnum(code = "DevkitDbType")
@Getter
@AllArgsConstructor
public enum DbTypeEnum implements BaseEnum<String> {

    /**
     * MySQL
     */
    MYSQL("MYSQL", "MySQL", "com.mysql.cj.jdbc.Driver",
        "jdbc:mysql://[ip]:[port]/[databaseName]?useUnicode=true&characterEncoding=UTF8&useSSL=false"
            + "&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",
        EnumColor.PRIMARY,
        // 系统库（从库清单中剔除）
        new String[]{"information_schema", "mysql", "performance_schema", "sys"}),

    /**
     * PostgreSQL
     */
    POSTGRESQL("POSTGRESQL", "PostgreSQL", "org.postgresql.Driver",
        "jdbc:postgresql://[ip]:[port]/[databaseName]?stringtype=unspecified",
        EnumColor.SUCCESS,
        // 系统 schema（从 schema 清单中剔除）
        new String[]{"information_schema", "pg_catalog", "pg_toast"});

    @JsonValue
    @EnumValue
    private final String code;

    private final String desc;

    private final String driver;

    /**
     * JDBC URL 模板，含 [ip] / [port] / [databaseName] 占位符
     */
    private final String urlTemplate;

    private final EnumColor color;

    /**
     * 系统库 / 系统 schema 名单（清单展示时剔除）
     */
    private final String[] sysObjects;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    /**
     * 按码值或常量名解析（大小写不敏感）
     *
     * @param code 码值或常量名
     * @return 数据库类型枚举
     */
    @JsonCreator
    public static DbTypeEnum ofCode(final String code) {
        return Arrays.stream(values())
            .filter(item -> item.code.equalsIgnoreCase(code) || item.name().equalsIgnoreCase(code))
            .findFirst()
            .orElseThrow(() -> CommonErrorCode.NOT_FOUND.exception("不支持的数据库类型: " + code));
    }
}
