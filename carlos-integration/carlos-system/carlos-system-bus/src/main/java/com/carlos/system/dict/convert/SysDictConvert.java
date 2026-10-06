package com.carlos.system.dict.convert;

import com.carlos.system.dict.pojo.dto.SysDictDTO;
import com.carlos.system.dict.pojo.entity.SysDict;
import com.carlos.system.dict.pojo.param.SysDictCreateParam;
import com.carlos.system.dict.pojo.param.SysDictUpdateParam;
import com.carlos.system.dict.pojo.vo.SysDictListVO;
import com.carlos.system.dict.pojo.vo.SysDictVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 系统字典 转换器
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@Mapper
public interface SysDictConvert {

    SysDictConvert INSTANCE = Mappers.getMapper(SysDictConvert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author carlos
     * @date 2021/12/2 14:55
     */
    SysDictDTO toDTO(SysDictCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author carlos
     * @date 2021/12/2 14:55
     */
    SysDictDTO toDTO(SysDictUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author carlos
     * @date 2021/12/2 15:13
     */
    List<SysDictDTO> toDTO(List<SysDict> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author carlos
     * @date 2021/12/2 15:13
     */
    SysDictDTO toDTO(SysDict entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author carlos
     * @date 2021/12/2 14:56
     */
    SysDict toDO(SysDictDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author carlos
     * @date 2021/12/2 15:04
     */
    SysDictVO toVO(SysDictDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dtos 数据传输对象
     * @return 数据显示对象
     * @author carlos
     * @date 2021/12/2 15:04
     */
    List<SysDictListVO> toVO(List<SysDictDTO> dtos);
}
