package ${project.packageName}.api.pojo.param;


import lombok.Data;
import lombok.experimental.Accessors;
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
@Accessors(chain = true)
public class Api${table.classPrefix}Param implements Serializable{
<#list table.columns as column>
    <#if !column.logicField>
    /** ${column.columnComment} */
    private ${column.javaType} ${column.propertyName};
    </#if>
</#list>
}
