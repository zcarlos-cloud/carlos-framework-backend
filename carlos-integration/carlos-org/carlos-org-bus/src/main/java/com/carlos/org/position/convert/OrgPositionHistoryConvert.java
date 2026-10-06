package com.carlos.org.position.convert;

import com.carlos.org.position.pojo.dto.OrgPositionHistoryDTO;
import com.carlos.org.position.pojo.entity.OrgPositionHistory;
import com.carlos.org.position.pojo.param.OrgPositionHistoryCreateParam;
import com.carlos.org.position.pojo.param.OrgPositionHistoryUpdateParam;
import com.carlos.org.position.pojo.vo.OrgPositionHistoryVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 岗位变更历史表 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Mapper(uses = {CommonConvert.class})
public interface OrgPositionHistoryConvert {

    OrgPositionHistoryConvert INSTANCE = Mappers.getMapper(OrgPositionHistoryConvert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionHistoryDTO toDTO(OrgPositionHistoryCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionHistoryDTO toDTO(OrgPositionHistoryUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    List<OrgPositionHistoryDTO> toDTO(List<OrgPositionHistory> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionHistoryDTO toDTO(OrgPositionHistory entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionHistory toDO(OrgPositionHistoryDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionHistoryVO toVO(OrgPositionHistoryDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    List<OrgPositionHistoryVO> toVO(List<OrgPositionHistory> dos);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param entity 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionHistoryVO toVO(OrgPositionHistory entity);
}
