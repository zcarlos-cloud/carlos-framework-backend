package ${project.packageName}.api.pojo.ao;

import lombok.Data;

import java.io.Serializable;
<#list table.imports as import>
    import ${import};
</#list>

/**
 * <p>
 * ${table.comment} API提供的对象(API Object)
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
public class ${table.classPrefix}AO implements Serializable{
<#list table.columns as column>
    <#if !column.logicField>
        /** ${column.columnComment} */
        private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
