package ${project.packageName}.domain.aggregate;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
<#list table.imports as import>
import ${import};
</#list>

/**
 * <p>
 * ${table.comment} 领域实体（聚合根）
 * </p>
 * <p>封装业务规则与领域行为，属于充血模型</p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Data
@Accessors(chain = true)
public class ${table.classPrefix} implements Serializable {

    private static final long serialVersionUID = 1L;

<#list table.columns as column>
    <#if !column.logicField>
    /**
     * ${column.columnComment}
     */
    private ${column.javaType} ${column.propertyName};
    </#if>
</#list>

    /**
     * 创建新${table.comment}领域实体
     *
     * @return ${table.classPrefix}
     */
    public static ${table.classPrefix} create() {
        return new ${table.classPrefix}();
    }

    /**
     * 执行业务更新操作
     * TODO: 根据实际业务补充领域行为
     */
    public void update() {
        // 领域业务规则校验与状态变更
    }

    /**
     * 逻辑删除标记
     */
    public void remove() {
        // TODO: 执行领域层面的删除前校验与清理
    }
}
