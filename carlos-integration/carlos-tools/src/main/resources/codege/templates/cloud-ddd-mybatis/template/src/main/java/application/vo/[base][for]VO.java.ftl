package ${project.packageName}.application.vo;

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
 * ${table.comment} 视图对象，响应给前端的数据展示对象
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
