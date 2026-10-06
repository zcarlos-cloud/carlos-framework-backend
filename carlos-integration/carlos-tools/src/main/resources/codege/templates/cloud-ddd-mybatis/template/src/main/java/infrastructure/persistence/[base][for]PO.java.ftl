package ${project.packageName}.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
<#list table.imports as import>
import ${import};
</#list>

/**
 * <p>
 * ${table.comment} 持久化对象（Persistence Object）
 * </p>
 * <p>与数据库表结构一一对应，仅用于基础设施层数据持久化</p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("${table.name}")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ${table.classPrefix}PO extends Model<${table.classPrefix}PO> implements Serializable {

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
