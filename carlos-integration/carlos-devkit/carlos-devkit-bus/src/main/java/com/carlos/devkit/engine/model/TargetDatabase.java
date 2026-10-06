package com.carlos.devkit.engine.model;

import com.carlos.devkit.pojo.enums.DbTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * <p>
 * 目标数据库连接信息（引擎视角，明文口令只在连接建立瞬间使用）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Accessors(chain = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TargetDatabase {

    /**
     * 数据库类型
     */
    private DbTypeEnum dbType;

    /**
     * 主机
     */
    private String host;

    /**
     * 端口
     */
    private String port;

    /**
     * 库名
     */
    private String databaseName;

    /**
     * 默认 schema（PostgreSQL 专用）
     */
    private String schemaName;

    /**
     * 用户名
     */
    private String username;

    /**
     * 明文口令
     */
    private String password;

    /**
     * 额外 JDBC 参数
     */
    private String jdbcParams;
}
