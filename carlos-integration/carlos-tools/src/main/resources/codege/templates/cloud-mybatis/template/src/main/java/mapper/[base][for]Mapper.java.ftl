package ${project.packageName}.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import ${project.packageName}.pojo.entity.${table.classPrefix};
import org.apache.ibatis.annotations.Mapper;


/**
 * <p>
 * ${table.comment} 查询接口
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Mapper
public interface ${table.classPrefix}Mapper extends MPJBaseMapper<${table.classPrefix}> {


}
