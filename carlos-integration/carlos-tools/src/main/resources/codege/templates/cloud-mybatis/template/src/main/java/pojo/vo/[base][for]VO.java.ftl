package ${project.packageName}.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
<#list table.imports as import>
import ${import};
</#list>

/**
 * <p>
 * ${table.comment} 显示层对象，向页面传输的对象
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ${table.classPrefix}VO implements Serializable {
private static final long serialVersionUID = 1L;
<#list table.columns as column>
    <#if !column.logicField && !column.versionField>
        @Schema(description = "${column.columnComment}")
    private ${column.javaType} ${column.propertyName};
    </#if>
</#list>

}
