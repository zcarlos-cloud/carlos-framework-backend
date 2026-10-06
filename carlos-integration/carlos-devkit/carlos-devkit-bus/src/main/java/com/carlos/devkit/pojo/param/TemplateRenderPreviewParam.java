package com.carlos.devkit.pojo.param;

import com.carlos.devkit.pojo.enums.FieldNameTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 单个模板渲染预览参数（用一张真实表的元数据渲染某个模板，用于在线编辑时即时看效果）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@Schema(description = "模板渲染预览参数")
public class TemplateRenderPreviewParam {

    @NotNull(message = "模板文件ID不能为空")
    @Schema(description = "模板文件ID")
    private Long templateId;

    @NotNull(message = "数据源ID不能为空")
    @Schema(description = "数据源ID")
    private Long dsId;

    @Schema(description = "库（MySQL）/ schema（PostgreSQL）")
    private String schema;

    @NotBlank(message = "样例表不能为空")
    @Schema(description = "样例表名")
    private String tableName;

    @Schema(description = "包名，默认 com.carlos.demo")
    private String packageName = "com.carlos.demo";

    @Schema(description = "作者，默认 Carlos")
    private String author = "Carlos";

    @Schema(description = "是否保留表名前缀，默认 true")
    private Boolean useTablePrefix = Boolean.TRUE;

    @Size(max = 64, message = "命名方式长度不能超过64")
    @Schema(description = "字段命名方式，默认 NOT_PREFIX_AND_CAMEL")
    private FieldNameTypeEnum nameType = FieldNameTypeEnum.NOT_PREFIX_AND_CAMEL;
}
