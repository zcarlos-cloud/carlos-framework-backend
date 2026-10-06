package ${project.packageName}.pojo.entity;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

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
@Document(indexName = "${table.name}", createIndex = true, shards = 5, replicas = 1)
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
    @Field(value = "${column.columnName}", type = FieldType.Auto)
    </#if>
    private ${column.javaType} ${column.propertyName};
</#list>

}
