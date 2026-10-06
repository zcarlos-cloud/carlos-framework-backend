package com.carlos.org.convert;

import com.carlos.org.pojo.dto.OrgDockingMappingDTO;
import com.carlos.org.pojo.entity.OrgDockingMapping;
import com.carlos.org.pojo.vo.OrgDockingMappingVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 用户信息对接关联表 转换器
 * </p>
 *
 * @author Carlos
 * @date 2025-2-27 15:41:32
 */
@Mapper(uses = {CommonConvert.class})
public interface OrgDockingMappingConvert {

    OrgDockingMappingConvert INSTANCE = Mappers.getMapper(OrgDockingMappingConvert.class);


    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2025-2-27 15:41:32
     */
    List<OrgDockingMappingDTO> toDTO(List<OrgDockingMapping> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2025-2-27 15:41:32
     */
    OrgDockingMappingDTO toDTO(OrgDockingMapping entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author Carlos
     * @date 2025-2-27 15:41:32
     */
    OrgDockingMapping toDO(OrgDockingMappingDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2025-2-27 15:41:32
     */
    OrgDockingMappingVO toVO(OrgDockingMappingDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2025-2-27 15:41:32
     */
    List<OrgDockingMappingVO> toVO(List<OrgDockingMapping> dos);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param entity 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2025-2-27 15:41:32
     */
    OrgDockingMappingVO toVO(OrgDockingMapping entity);

    // OrgDockingMappingAO toAO(OrgDockingMappingDTO dockingMapping);
}
