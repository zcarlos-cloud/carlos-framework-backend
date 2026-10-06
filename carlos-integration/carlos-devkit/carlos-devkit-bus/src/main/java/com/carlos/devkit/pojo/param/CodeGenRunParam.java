package com.carlos.devkit.pojo.param;

import com.carlos.devkit.pojo.enums.FieldNameTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 代码生成参数（预览与下载共用）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@Schema(description = "代码生成参数")
public class CodeGenRunParam {

    @NotNull(message = "数据源ID不能为空")
    @Schema(description = "数据源ID")
    private Long dsId;

    @Schema(description = "库（MySQL）/ schema（PostgreSQL）")
    private String schema;

    @NotNull(message = "模板组ID不能为空")
    @Schema(description = "模板组ID")
    private Long groupId;

    @NotEmpty(message = "请至少选择一张数据表")
    @Schema(description = "选中的数据表名")
    private List<String> tableNames;

    @NotBlank(message = "项目名不能为空")
    @Size(max = 128, message = "项目名长度不能超过128")
    @Schema(description = "项目名（产物根目录名）")
    private String projectName;

    @NotBlank(message = "包名不能为空")
    @Size(max = 256, message = "包名长度不能超过256")
    @Schema(description = "包名，如 com.carlos.demo")
    private String packageName;

    @Size(max = 64, message = "作者长度不能超过64")
    @Schema(description = "作者，默认 Carlos")
    private String author = "Carlos";

    @Schema(description = "是否保留表名前缀，默认 true")
    private Boolean useTablePrefix = Boolean.TRUE;

    @Schema(description = "字段命名方式，默认 NOT_PREFIX_AND_CAMEL")
    private FieldNameTypeEnum nameType = FieldNameTypeEnum.NOT_PREFIX_AND_CAMEL;
}
