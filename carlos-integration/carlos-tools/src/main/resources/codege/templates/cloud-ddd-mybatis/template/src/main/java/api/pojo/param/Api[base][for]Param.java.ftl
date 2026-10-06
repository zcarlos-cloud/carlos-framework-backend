package ${project.packageName}.api.pojo.param;

import lombok.Data;

import java.io.Serializable;
<#list table.imports as import>
    import ${import};
</#list>

/**
 * <p>
 * ${table.comment} API请求参数
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
public class Api${table.classPrefix}Param implements Serializable{
<#list table.columns as column>
    <#if !column.logicField>
        /** ${column.columnComment} */
        private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
