package ${project.packageName}.application.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
<#list table.imports as import>
    import ${import};
</#list>

/**
 * <p>
 * ${table.comment} 新增参数封装
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@Accessors(chain = true)
@Schema(description =  "${table.comment}新增参数")
public class ${table.classPrefix}CreateParam {
<#list table.columns as column>
    <#if !column.commonField && !column.primaryKey && !column.logicField  && !column.versionField>
        <#if !column.nullable>
            <#if column.javaType == 'String'>
    @NotBlank(message = "${column.columnComment}不能为空")
            <#else>
    @NotNull(message = "${column.columnComment}不能为空")
            </#if>
        </#if>
        @Schema(description = "${column.columnComment}")
    private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
