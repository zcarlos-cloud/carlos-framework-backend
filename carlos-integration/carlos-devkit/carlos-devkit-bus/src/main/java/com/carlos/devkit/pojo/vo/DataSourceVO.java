package com.carlos.devkit.pojo.vo;

import com.carlos.devkit.pojo.enums.DbTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 数据源 显示层对象
 * </p>
 * <p>
 * <b>任何情况下都不下发密码</b>，只回 {@code hasPassword} 供前端提示「已配置密码」。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "数据源视图对象")
public class DataSourceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "数据源名称")
    private String dsName;

    @Schema(description = "数据源编码")
    private String dsCode;

    @Schema(description = "数据库类型")
    private DbTypeEnum dbType;

    @Schema(description = "主机地址")
    private String host;

    @Schema(description = "端口")
    private Integer port;

    @Schema(description = "库名")
    private String databaseName;

    @Schema(description = "默认 schema")
    private String schemaName;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "是否已配置密码（不返回密码本身）")
    private Boolean hasPassword;

    @Schema(description = "额外 JDBC 参数")
    private String jdbcParams;

    @Schema(description = "是否只读数据源")
    private Boolean readOnly;

    @Schema(description = "状态：1=启用 0=停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
