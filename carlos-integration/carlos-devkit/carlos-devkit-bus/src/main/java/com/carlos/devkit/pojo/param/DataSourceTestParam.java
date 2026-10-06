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
 * 数据源连接测试参数
 * </p>
 * <p>
 * 二选一：
 * <ul>
 *   <li>传 {@code id}：使用库中已存的凭据（编辑场景，密码留空时按原值测试）；</li>
 *   <li>不传 {@code id}：使用表单里的临时凭据（新增场景，尚未落库）。</li>
 * </ul>
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@Schema(description = "数据源连接测试参数")
public class DataSourceTestParam {

    @Schema(description = "数据源ID（编辑/已有数据源测试时传，密码留空则使用库中已存密码）")
    private Long id;

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
    @Schema(description = "密码明文（可空，传 id 时留空表示用库中已存密码）")
    private String password;

    @Size(max = 1024, message = "JDBC 参数长度不能超过1024")
    @Schema(description = "额外 JDBC 参数")
    private String jdbcParams;
}
