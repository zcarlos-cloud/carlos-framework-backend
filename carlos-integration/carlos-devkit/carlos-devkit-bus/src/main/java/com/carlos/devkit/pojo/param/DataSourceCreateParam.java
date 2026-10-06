package com.carlos.devkit.pojo.param;

import com.carlos.devkit.pojo.enums.DbTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 数据源新增参数
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@Schema(description = "数据源新增参数")
public class DataSourceCreateParam {

    @NotBlank(message = "数据源名称不能为空")
    @Size(max = 128, message = "数据源名称长度不能超过128")
    @Schema(description = "数据源名称")
    private String dsName;

    @Size(max = 64, message = "数据源编码长度不能超过64")
    @Schema(description = "数据源编码")
    private String dsCode;

    @NotNull(message = "数据库类型不能为空")
    @Schema(description = "数据库类型：MYSQL/POSTGRESQL")
    private DbTypeEnum dbType;

    @NotBlank(message = "主机地址不能为空")
    @Size(max = 128, message = "主机地址长度不能超过128")
    @Schema(description = "主机地址")
    private String host;

    @NotNull(message = "端口不能为空")
    @Min(value = 1, message = "端口范围 1-65535")
    @Max(value = 65535, message = "端口范围 1-65535")
    @Schema(description = "端口")
    private Integer port;

    @Size(max = 128, message = "库名长度不能超过128")
    @Schema(description = "库名")
    private String databaseName;

    @Size(max = 128, message = "schema 长度不能超过128")
    @Schema(description = "默认 schema（PostgreSQL 专用）")
    private String schemaName;

    @Size(max = 128, message = "用户名长度不能超过128")
    @Schema(description = "用户名")
    private String username;

    @Size(max = 256, message = "密码长度不能超过256")
    @Schema(description = "密码（明文入参，后端 SM4 加密后落库；任何接口都不会回发）")
    private String password;

    @Size(max = 1024, message = "JDBC 参数长度不能超过1024")
    @Schema(description = "额外 JDBC 参数，如 useSSL=false")
    private String jdbcParams;

    @Schema(description = "是否只读数据源，默认 true")
    private Boolean readOnly = Boolean.TRUE;

    @Size(max = 512, message = "备注长度不能超过512")
    @Schema(description = "备注")
    private String remark;
}
