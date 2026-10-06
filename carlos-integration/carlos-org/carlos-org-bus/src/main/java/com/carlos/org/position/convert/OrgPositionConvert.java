package com.carlos.org.position.convert;

import com.carlos.org.position.api.pojo.ao.OrgPositionAO;
import com.carlos.org.position.pojo.dto.OrgPositionDTO;
import com.carlos.org.position.pojo.entity.OrgPosition;
import com.carlos.org.position.pojo.param.OrgPositionCreateParam;
import com.carlos.org.position.pojo.param.OrgPositionUpdateParam;
import com.carlos.org.position.pojo.vo.OrgPositionVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 岗位表 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Mapper(uses = {CommonConvert.class})
public interface OrgPositionConvert {

    OrgPositionConvert INSTANCE = Mappers.getMapper(OrgPositionConvert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionDTO toDTO(OrgPositionCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionDTO toDTO(OrgPositionUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    List<OrgPositionDTO> toDTO(List<OrgPosition> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionDTO toDTO(OrgPosition entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPosition toDO(OrgPositionDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionVO toVO(OrgPositionDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    List<OrgPositionVO> toVO(List<OrgPosition> dos);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param entity 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgPositionVO toVO(OrgPosition entity);

    /**
     * 数据显示对象转 API 对象（岗位分页接口返回的是 VO，接口契约要 AO）
     *
     * <p>方法名刻意不叫 {@code toAO}：与 {@code toAO(OrgPositionDTO)} 重载时
     * {@code toAOList} 的两个重载会因泛型擦除而签名相同（都是 {@code List}），编译不过。</p>
     *
     * @param vo 数据显示对象
     * @return API 对象
     */
    @Mapping(target = "version", ignore = true)
    OrgPositionAO toAOFromVo(OrgPositionVO vo);

    /**
     * 数据显示对象列表转 API 对象列表
     *
     * @param voList 数据显示对象列表
     * @return API 对象列表
     */
    List<OrgPositionAO> toAOListFromVo(List<OrgPositionVO> voList);

    /**
     * 数据传输对象转 API 对象（批量按 id 查询岗位时用）
     *
     * @param dto 数据传输对象
     * @return API 对象
     */
    OrgPositionAO toAO(OrgPositionDTO dto);

    /**
     * 数据传输对象列表转 API 对象列表
     *
     * @param dtoList 数据传输对象列表
     * @return API 对象列表
     */
    List<OrgPositionAO> toAOList(List<OrgPositionDTO> dtoList);
}
