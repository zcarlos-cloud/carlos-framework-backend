package com.carlos.org.position.convert;

import com.carlos.org.position.pojo.dto.OrgPositionCategoryDTO;
import com.carlos.org.position.pojo.entity.OrgPositionCategory;
import com.carlos.org.position.pojo.param.OrgPositionCategoryCreateParam;
import com.carlos.org.position.pojo.param.OrgPositionCategoryUpdateParam;
import com.carlos.org.position.pojo.vo.OrgPositionCategoryVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 岗位类别表（职系） 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Mapper(uses = {CommonConvert.class})
public interface OrgPositionCategoryConvert {

    OrgPositionCategoryConvert INSTANCE = Mappers.getMapper(OrgPositionCategoryConvert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionCategoryDTO toDTO(OrgPositionCategoryCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionCategoryDTO toDTO(OrgPositionCategoryUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    List<OrgPositionCategoryDTO> toDTO(List<OrgPositionCategory> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionCategoryDTO toDTO(OrgPositionCategory entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionCategory toDO(OrgPositionCategoryDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionCategoryVO toVO(OrgPositionCategoryDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    List<OrgPositionCategoryVO> toVO(List<OrgPositionCategory> dos);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param entity 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionCategoryVO toVO(OrgPositionCategory entity);
}
