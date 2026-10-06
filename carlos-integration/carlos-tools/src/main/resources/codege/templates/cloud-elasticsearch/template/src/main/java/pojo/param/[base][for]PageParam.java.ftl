package ${project.packageName}.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import com.carlos.core.param.ParamPage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

<#list table.imports as import>
    import ${import};
</#list>


/**
 * <p>
 * ${table.comment} 列表查询参数封装
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description =  "${table.comment}列表查询参数")
public class ${table.classPrefix}PageParam extends ParamPage {
<#list table.columns as column>
    <#if !column.commonField && !column.primaryKey && !column.logicField  && !column.versionField>
        @Schema(description = "${column.columnComment}")
    private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
@Schema(description = "开始时间")
    private LocalDateTime start;

@Schema(description = "结束时间")
    private LocalDateTime end;
}
