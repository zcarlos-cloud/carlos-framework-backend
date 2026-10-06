package ${project.packageName}.infrastructure.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import ${project.packageName}.infrastructure.persistence.${table.classPrefix}PO;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * ${table.comment} 数据访问层接口
 * </p>
 * <p>基于 MyBatis-Plus + MyBatis-Plus-Join 实现，仅由 Repository 调用</p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Mapper
public interface ${table.classPrefix}Mapper extends MPJBaseMapper<${table.classPrefix}PO> {

}
