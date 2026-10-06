package com.carlos.devkit.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 数据源连接测试结果 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "数据源连接测试结果")
public class TestConnectionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否连通")
    private Boolean success;

    @Schema(description = "数据库产品名")
    private String databaseName;

    @Schema(description = "数据库产品版本")
    private String databaseVersion;

    @Schema(description = "响应耗时（毫秒）")
    private Long responseTimeMs;

    @Schema(description = "失败原因")
    private String errorMessage;
}
