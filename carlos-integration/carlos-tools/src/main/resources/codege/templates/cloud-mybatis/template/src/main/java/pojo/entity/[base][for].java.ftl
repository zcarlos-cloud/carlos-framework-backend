package ${project.packageName}.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

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
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("${table.name}")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ${table.classPrefix} extends Model<${table.classPrefix}> implements Serializable{
private static final long serialVersionUID = 1L;
<#list table.columns as column>
    /**
    * ${column.columnComment}
    */
    <#if column.logicField>
    @TableLogic
    </#if>
    <#if column.versionField>
    @Version
    </#if>
    <#if column.primaryKey>
    @TableId(type = IdType.ASSIGN_ID, value = "${column.columnName}")
    <#else>
    @TableField(value = "${column.columnName}")
    </#if>
    private ${column.javaType} ${column.propertyName};
</#list>

}
