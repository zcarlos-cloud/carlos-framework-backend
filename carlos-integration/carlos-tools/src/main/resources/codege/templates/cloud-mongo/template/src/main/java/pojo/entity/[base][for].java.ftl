package ${project.packageName}.pojo.entity;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
<#list table.imports as import>
    import ${import};
</#list>

/**
* <p>
    * ${table.comment} 数据源对象
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@Data
@Accessors(chain = true)
@Document(collection = "${table.name}")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ${table.classPrefix} implements Serializable{
private static final long serialVersionUID = 1L;
<#list table.columns as column>
    /**
    * ${column.columnComment}
    */
    <#if column.primaryKey>
        @Id
    <#else>
        @Field(value = "${column.columnName}")
    </#if>
    private ${column.javaType} ${column.propertyName};
</#list>

}
