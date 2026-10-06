package ${project.packageName}.application.dto;

import lombok.Data;
import lombok.experimental.Accessors;

<#list table.imports as import>
import ${import};
</#list>

/**
 * <p>
 * ${table.comment} 数据传输对象，用于应用层与领域层之间的数据传递
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@Accessors(chain = true)
public class ${table.classPrefix}DTO {
<#list table.columns as column>
    <#if !column.logicField>
    /** ${column.columnComment} */
    private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
