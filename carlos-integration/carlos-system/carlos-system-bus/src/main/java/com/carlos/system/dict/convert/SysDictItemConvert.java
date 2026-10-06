package com.carlos.system.dict.convert;

import com.carlos.core.base.Dict;
import com.carlos.system.dict.pojo.dto.SysDictItemDTO;
import com.carlos.system.dict.pojo.entity.SysDictItem;
import com.carlos.system.dict.pojo.param.SysDictCreateParam;
import com.carlos.system.dict.pojo.param.SysDictItemCreateParam;
import com.carlos.system.dict.pojo.param.SysDictItemUpdateParam;
import com.carlos.system.dict.pojo.param.SysDictUpdateParam;
import com.carlos.system.dict.pojo.vo.SysDictItemListVO;
import com.carlos.system.dict.pojo.vo.SysDictItemVO;
import com.carlos.system.pojo.ao.DictItemAO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 系统字典详情 转换器
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@Mapper
public interface SysDictItemConvert {

    SysDictItemConvert INSTANCE = Mappers.getMapper(SysDictItemConvert.class);


    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author carlos
     * @date 2021/12/2 14:55
     */
    SysDictItemDTO toDTO(SysDictItemCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author carlos
     * @date 2021/12/2 14:55
     */
    SysDictItemDTO toDTO(SysDictItemUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author carlos
     * @date 2021/12/2 15:13
     */
    List<SysDictItemDTO> toDTO(List<SysDictItem> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author carlos
     * @date 2021/12/2 15:13
     */
    SysDictItemDTO toDTO(SysDictItem entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author carlos
     * @date 2021/12/2 14:56
     */
    SysDictItem toDO(SysDictItemDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author carlos
     * @date 2021/12/2 15:04
     */
    SysDictItemVO toVO(SysDictItemDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dtos 数据传输对象
     * @return 数据显示对象
     * @author carlos
     * @date 2021/12/2 15:04
     */

    List<SysDictItemListVO> toVO(List<SysDictItemDTO> dtos);

    @Mappings({
        @Mapping(source = "itemName", target = "name"),
        @Mapping(source = "itemCode", target = "code")
    })
    SysDictItemListVO toListVO(SysDictItemDTO dtos);


    /**
     * dto字段转换
     *
     * @param dto 原始dto
     * @return 目标dto
     * @author carlos
     * @date 2021/12/3 11:53
     */
    SysDictItemDTO toDTO(SysDictItemDTO dto);

    /**
     * 转字典通用对象
     *
     * @param dto 数据传输对象
     * @return com.carlos.common.core.base.DictDTO
     * @author carlos
     * @date 2021/12/30 12:33
     */
    @Mapping(source = "itemName", target = "name")
    @Mapping(source = "itemCode", target = "code")
    Dict toDictDTO(SysDictItemDTO dto);

    List<SysDictItemVO> toListVO(List<SysDictItemDTO> dtos);


    SysDictItemDTO toDTO(SysDictUpdateParam.Item item);

    List<SysDictItemDTO> create2dto(List<SysDictCreateParam.Item> item);

    List<SysDictItemDTO> update2dto(List<SysDictUpdateParam.Item> item);

    List<DictItemAO> toAOList(List<SysDictItemDTO> dtos);

    List<SysDictItem> toDO(List<SysDictItemDTO> items);
}
