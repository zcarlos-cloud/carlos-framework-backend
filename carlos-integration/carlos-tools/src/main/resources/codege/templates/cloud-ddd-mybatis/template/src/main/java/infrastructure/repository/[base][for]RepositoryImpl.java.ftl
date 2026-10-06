package ${project.packageName}.infrastructure.repository;

import ${project.packageName}.domain.aggregate.${table.classPrefix};
import ${project.packageName}.domain.repository.${table.classPrefix}Repository;
import ${project.packageName}.infrastructure.convert.${table.classPrefix}Convert;
import ${project.packageName}.infrastructure.mapper.${table.classPrefix}Mapper;
import ${project.packageName}.infrastructure.persistence.${table.classPrefix}PO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

/**
 * <p>
 * ${table.comment} 仓储实现
 * </p>
 * <p>基于 MyBatis-Plus 实现领域对象的持久化，隔离 ORM 细节</p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ${table.classPrefix}RepositoryImpl implements ${table.classPrefix}Repository {

    private final ${table.classPrefix}Mapper ${table.classMainPrefix}Mapper;

    @Override
    public ${table.classPrefix} findById(Serializable id) {
        if (id == null) {
            return null;
        }
        ${table.classPrefix}PO po = ${table.classMainPrefix}Mapper.selectById(id);
        return ${table.classPrefix}Convert.INSTANCE.toDomain(po);
    }

    @Override
    public void save(${table.classPrefix} entity) {
        ${table.classPrefix}PO po = ${table.classPrefix}Convert.INSTANCE.toPO(entity);
        if (po.getId() == null) {
            ${table.classMainPrefix}Mapper.insert(po);
            entity.setId(po.getId());
        } else {
            ${table.classMainPrefix}Mapper.updateById(po);
        }
    }

    @Override
    public void remove(${table.classPrefix} entity) {
        if (entity == null || entity.getId() == null) {
            return;
        }
        ${table.classMainPrefix}Mapper.deleteById(entity.getId());
    }
}
