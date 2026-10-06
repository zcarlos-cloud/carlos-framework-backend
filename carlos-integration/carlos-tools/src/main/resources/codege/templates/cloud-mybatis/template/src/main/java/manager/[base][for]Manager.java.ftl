package ${project.packageName}.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import ${project.packageName}.pojo.vo.${table.classPrefix}VO;
import ${project.packageName}.pojo.entity.${table.classPrefix};
import ${project.packageName}.pojo.dto.${table.classPrefix}DTO;
import ${project.packageName}.pojo.param.${table.classPrefix}PageParam;

import java.io.Serializable;
/**
 * <p>
 * ${table.comment} 查询封装接口
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
public interface ${table.classPrefix}Manager extends BaseService<${table.classPrefix}>{

    /**
     * 新增${table.comment}
     *
     * @param dto ${table.comment}数据
     * @return boolean
     * @author  ${project.author}
     * @date    ${.now}
     */
    boolean add(${table.classPrefix}DTO dto);

    /**
     * 删除${table.comment}
     *
     * @param id ${table.comment}id
     * @return boolean
     * @author  ${project.author}
     * @date    ${.now}
     */
    boolean delete(Serializable id);

    /**
     * 修改${table.comment}信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author  ${project.author}
     * @date    ${.now}
     */
    boolean modify(${table.classPrefix}DTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
* @return ${project.packageName}.pojo.dto.${table.classPrefix}DTO
     * @author ${project.author}
     * @date   ${.now}
     */
    ${table.classPrefix}DTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param  param 分页参数
     * @author ${project.author}
     * @date   ${.now}
     */
    Paging<${table.classPrefix}VO> getPage(${table.classPrefix}PageParam param);
}
