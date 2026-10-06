package ${project.packageName}.pojo.dto;


import lombok.Data;
import lombok.experimental.Accessors;

<#list table.imports as import>
import ${import};
</#list>

/**
 * <p>
 * ${table.comment} 数据传输对象，service和manager向外传输对象
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
