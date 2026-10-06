package com.carlos.devkit.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 目标库列元数据
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "目标库列元数据")
public class ColumnMetaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "列名")
    private String columnName;

    @Schema(description = "数据库数据类型")
    private String columnDbType;

    @Schema(description = "Java 数据类型")
    private String javaType;

    @Schema(description = "列注释")
    private String columnComment;

    @Schema(description = "是否主键")
    private Boolean primaryKey;

    @Schema(description = "是否可空")
    private Boolean nullable;

    @Schema(description = "是否自增")
    private Boolean autoIncrement;

    @Schema(description = "是否逻辑删除字段")
    private Boolean logicField;

    @Schema(description = "是否乐观锁字段")
    private Boolean versionField;

    @Schema(description = "是否通用字段")
    private Boolean commonField;

    @Schema(description = "生成后的 Java 属性名")
    private String propertyName;
}
