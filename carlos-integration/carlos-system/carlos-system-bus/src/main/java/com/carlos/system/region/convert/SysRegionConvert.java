package com.carlos.system.region.convert;

import com.carlos.system.config.CommonConvert;
import com.carlos.system.pojo.ao.SysRegionAO;
import com.carlos.system.pojo.param.ApiSysRegionAddParam;
import com.carlos.system.region.pojo.dto.SysRegionDTO;
import com.carlos.system.region.pojo.entity.SysRegion;
import com.carlos.system.region.pojo.excel.RegionExcel;
import com.carlos.system.region.pojo.param.SysRegionCreateParam;
import com.carlos.system.region.pojo.param.SysRegionUpdateParam;
import com.carlos.system.region.pojo.vo.SysRegionVO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 行政区域划分 转换器
 * </p>
 *
 * @author carlos
 * @date 2022-11-8 19:30:24
 */
@Mapper(uses = {CommonConvert.class})
public interface SysRegionConvert {

    SysRegionConvert INSTANCE = Mappers.getMapper(SysRegionConvert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author carlos
     * @date 2022-11-8 19:30:24
     */
    SysRegionDTO toDTO(SysRegionCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author carlos
     * @date 2022-11-8 19:30:24
     */
    SysRegionDTO toDTO(SysRegionUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author carlos
     * @date 2022-11-8 19:30:24
     */
    List<SysRegionDTO> toDTO(List<SysRegion> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author carlos
     * @date 2022-11-8 19:30:24
     */
    SysRegionDTO toDTO(SysRegion entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author carlos
     * @date 2022-11-8 19:30:24
     */
    SysRegion toDO(SysRegionDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author carlos
     * @date 2022-11-8 19:30:24
     */
    SysRegionVO toVO(SysRegionDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author carlos
     * @date 2022-11-8 19:30:24
     */
    List<SysRegionVO> toVO(List<SysRegion> dos);


    List<SysRegionVO> dto2vo(List<SysRegionDTO> dtos);

    /**
     * @Title: merge
     * @Description: 数据合并，将源对象dto中不为空的数据合并到目标对象target中
     * @Date: 2023/2/15 10:28
     * @Parameters: [dto, target]
     * @Return void
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void merge(SysRegionDTO dto, @MappingTarget SysRegionDTO target);

    /**
     * @Title: excelToDTOS
     * @Description: 数据导入对象转数据传输对象
     * @Date: 2023/2/21 18:37
     * @Parameters: [cachedDataList]
     * @Return java.util.List<com.carlos.common.dto.sys.SysRegionDTO>
     */
    List<SysRegionDTO> excelToDTOS(List<RegionExcel> cachedDataList);

    /**
     * @Title: toDOS
     * @Description: 数据传输对象转数据持久对象
     * @Date: 2023/2/21 18:53
     * @Parameters: [regions]
     * @Return java.util.List<com.carlos.system.region.pojo.entity.SysRegion>
     */
    List<SysRegion> toDOS(List<SysRegionDTO> regions);

    /**
     * @Title: dtoToexcel
     * @Description: 数据传输对象转数据导入导出对象
     * @Date: 2023/2/22 10:55
     * @Parameters: [list]
     * @Return java.util.List<com.carlos.system.region.pojo.excel.RegionExcel>
     */
    List<RegionExcel> dtoToexcel(List<SysRegionDTO> list);


    SysRegionDTO toDTO(ApiSysRegionAddParam region);

    List<SysRegionAO> toAOList(List<SysRegionDTO> regionTree);

    SysRegionAO toAO(SysRegionDTO dto);
    // List<SysRegionVO> toRecursionListVO(List<SysRegionDTO> tree);
}
