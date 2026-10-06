package ${project.packageName}.repository;


import ${project.packageName}.pojo.entity.${table.classPrefix};
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;


import java.util.Map;

/**
 * <p>
 * ${table.comment} 查询接口
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Repository
public interface ${table.classPrefix}Repository extends ElasticsearchRepository<${table.classPrefix}, String> {


}
