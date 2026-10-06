package ${project.packageName}.application;

import com.carlos.core.pagination.Paging;
import ${project.packageName}.application.dto.${table.classPrefix}DTO;
import ${project.packageName}.application.param.${table.classPrefix}PageParam;
import ${project.packageName}.application.vo.${table.classPrefix}VO;
import ${project.packageName}.domain.aggregate.${table.classPrefix};
import ${project.packageName}.domain.repository.${table.classPrefix}Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Set;

/**
 * <p>
 * ${table.comment} 应用服务层
 * </p>
 * <p>负责业务流程编排与事务控制，不包含领域业务规则</p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ${table.classPrefix}AppService {

    private final ${table.classPrefix}Repository ${table.classMainPrefix}Repository;

    /**
     * 新增${table.comment}
     *
     * @param dto ${table.comment}数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void add${table.classPrefix}(${table.classPrefix}DTO dto) {
        ${table.classPrefix} entity = new ${table.classPrefix}();
        // TODO: 将 DTO 属性映射到领域实体
        ${table.classMainPrefix}Repository.save(entity);
        log.info("新增${table.comment}成功，id:{}", entity.getId());
    }

    /**
     * 删除${table.comment}
     *
     * @param ids ${table.comment}id集合
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete${table.classPrefix}(Set<Serializable> ids) {
        for (Serializable id : ids) {
            ${table.classPrefix} entity = ${table.classMainPrefix}Repository.findById(id);
            if (entity != null) {
                ${table.classMainPrefix}Repository.remove(entity);
            }
        }
        log.info("删除${table.comment}成功，ids:{}", ids);
    }

    /**
     * 修改${table.comment}信息
     *
     * @param dto 对象信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void update${table.classPrefix}(${table.classPrefix}DTO dto) {
        ${table.classPrefix} entity = ${table.classMainPrefix}Repository.findById(dto.getId());
        if (entity == null) {
            log.warn("${table.comment}不存在，id:{}", dto.getId());
            return;
        }
        // TODO: 将 DTO 属性映射到领域实体并执行业务操作
        ${table.classMainPrefix}Repository.save(entity);
        log.info("修改${table.comment}成功，id:{}", dto.getId());
    }

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return ${table.classPrefix}DTO
     */
    public ${table.classPrefix}DTO getDtoById(Serializable id) {
        ${table.classPrefix} entity = ${table.classMainPrefix}Repository.findById(id);
        if (entity == null) {
            return null;
        }
        ${table.classPrefix}DTO dto = new ${table.classPrefix}DTO();
        // TODO: 将领域实体属性映射到 DTO
        return dto;
    }

    /**
     * 分页列表
     *
     * @param param 分页参数
     * @return 分页数据
     */
    public Paging<${table.classPrefix}VO> getPage(${table.classPrefix}PageParam param) {
        // TODO: 调用 Repository 或领域服务实现分页查询
        return null;
    }
}
