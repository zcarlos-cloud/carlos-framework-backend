package com.carlos.org.position.convert;

import com.carlos.org.position.pojo.dto.OrgUserPositionDTO;
import com.carlos.org.position.pojo.entity.OrgUserPosition;
import com.carlos.org.position.pojo.param.OrgUserPositionAssignParam;
import com.carlos.org.position.pojo.param.OrgUserPositionCreateParam;
import com.carlos.org.position.pojo.param.OrgUserPositionUpdateParam;
import com.carlos.org.position.pojo.vo.OrgUserPositionVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 用户岗位职级关联表（核心任职信息） 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Mapper(uses = {CommonConvert.class})
public interface OrgUserPositionConvert {

    OrgUserPositionConvert INSTANCE = Mappers.getMapper(OrgUserPositionConvert.class);

    /**
     * 用户分配岗位参数对象转数据传输对象
     *
     * @param param 分配参数
     * @return 数据传输对象
     */
    OrgUserPositionDTO toDTO(OrgUserPositionAssignParam param);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgUserPositionDTO toDTO(OrgUserPositionCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgUserPositionDTO toDTO(OrgUserPositionUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    List<OrgUserPositionDTO> toDTO(List<OrgUserPosition> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgUserPositionDTO toDTO(OrgUserPosition entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgUserPosition toDO(OrgUserPositionDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgUserPositionVO toVO(OrgUserPositionDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    List<OrgUserPositionVO> toVO(List<OrgUserPosition> dos);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param entity 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgUserPositionVO toVO(OrgUserPosition entity);
}
