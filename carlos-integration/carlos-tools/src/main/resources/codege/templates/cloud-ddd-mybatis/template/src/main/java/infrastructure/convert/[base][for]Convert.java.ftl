package ${project.packageName}.infrastructure.convert;

import ${project.packageName}.application.dto.${table.classPrefix}DTO;
import ${project.packageName}.application.param.${table.classPrefix}CreateParam;
import ${project.packageName}.application.param.${table.classPrefix}UpdateParam;
import ${project.packageName}.application.vo.${table.classPrefix}VO;
import ${project.packageName}.domain.aggregate.${table.classPrefix};
import ${project.packageName}.infrastructure.persistence.${table.classPrefix}PO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * ${table.comment} 对象转换器
 * </p>
 * <p>负责 Param/DTO/Domain/PO/VO 之间的类型转换</p>
 *
 * @author ${project.author}
 * @date ${.now}
 */
@Mapper
public interface ${table.classPrefix}Convert {

    ${table.classPrefix}Convert INSTANCE = Mappers.getMapper(${table.classPrefix}Convert.class);

    // Param -> DTO
    ${table.classPrefix}DTO toDTO(${table.classPrefix}CreateParam param);

    ${table.classPrefix}DTO toDTO(${table.classPrefix}UpdateParam param);

    // PO -> Domain
    ${table.classPrefix} toDomain(${table.classPrefix}PO po);

    // Domain -> PO
    ${table.classPrefix}PO toPO(${table.classPrefix} domain);

    // DTO -> VO
    ${table.classPrefix}VO toVO(${table.classPrefix}DTO dto);

    // Domain -> DTO
    ${table.classPrefix}DTO toDTO(${table.classPrefix} domain);

    // PO -> VO
    ${table.classPrefix}VO toVO(${table.classPrefix}PO po);

    List<${table.classPrefix}VO> toVO(List<${table.classPrefix}PO> pos);

    List<${table.classPrefix}DTO> toDTO(List<${table.classPrefix}PO> pos);
}
