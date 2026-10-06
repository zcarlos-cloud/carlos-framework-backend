package ${project.packageName}.service;

import ${project.packageName}.manager.${table.classPrefix}Manager;
import ${project.packageName}.pojo.dto.${table.classPrefix}DTO;
import ${project.packageName}.service.${table.classPrefix}Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;


/**
 * <p>
 * ${table.comment} 业务
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ${table.classPrefix}Service {

private final ${table.classPrefix}Manager ${table.classMainPrefix}Manager;

    /**
     * 新增${table.comment}
     *
     * @param dto ${table.comment}数据
     * @author  ${project.author}
     * @date    ${.now}
     */
    public void add${table.classPrefix}(${table.classPrefix}DTO dto){
        boolean success = ${table.classMainPrefix}Manager.add(dto);
        if (!success) {
            // 保存失败的应对措施
            return;
        }
        Serializable id = dto.getId();
        log.info("Insert '${table.classPrefix}' data: id:{}", id);
        // 保存完成的后续业务
    }

    /**
     * 删除${table.comment}
     *
     * @param ids ${table.comment}id
     * @author  ${project.author}
     * @date    ${.now}
     */
    public void delete${table.classPrefix}(Set<Serializable> ids){
        for (Serializable id : ids) {
        boolean success = ${table.classMainPrefix}Manager.delete(id);
        if (!success) {
            // 删除失败的措施
            continue;
        }
            // 删除成功的后续业务
        }
    }

    /**
     * 修改${table.comment}信息
     *
     * @param dto 对象信息
     * @author  ${project.author}
     * @date    ${.now}
     */
    public void update${table.classPrefix}(${table.classPrefix}DTO dto){
        boolean success = ${table.classMainPrefix}Manager.modify(dto);
        if (!success) {
            // 修改失败操作
            return;
        }
        // 修改成功的后续操作
        log.info("Update '${table.classPrefix}' data: id:{}", dto.getId());
    }

}
