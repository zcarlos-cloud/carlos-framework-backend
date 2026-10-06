package ${project.packageName}.domain.repository;

import ${project.packageName}.domain.aggregate.${table.classPrefix};

import java.io.Serializable;

/**
 * <p>
 * ${table.comment} 仓储接口
 * </p>
 * <p>定义领域对象的持久化契约，由基础设施层实现</p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
public interface ${table.classPrefix}Repository {

    /**
     * 根据ID查询领域实体
     *
     * @param id 主键
     * @return ${table.classPrefix}
     */
    ${table.classPrefix} findById(Serializable id);

    /**
     * 保存领域实体（新增或更新）
     *
     * @param entity 领域实体
     */
    void save(${table.classPrefix} entity);

    /**
     * 移除领域实体
     *
     * @param entity 领域实体
     */
    void remove(${table.classPrefix} entity);
}
