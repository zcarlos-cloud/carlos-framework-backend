package com.carlos.system.menu.convert;

import com.carlos.system.menu.pojo.dto.SysOperationDTO;
import com.carlos.system.menu.pojo.entity.SysOperation;
import com.carlos.system.menu.pojo.param.SysOperationCreateParam;
import com.carlos.system.menu.pojo.param.SysOperationUpdateParam;
import com.carlos.system.menu.pojo.vo.SysOperationVO;
import com.carlos.system.pojo.ao.SysOperationAO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 统一操作表 转换器
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Mapper(uses = {CommonConvert.class})
public interface SysOperationConvert {

    SysOperationConvert INSTANCE = Mappers.getMapper(SysOperationConvert.class);

    SysOperationDTO toDTO(SysOperationCreateParam param);

    SysOperationDTO toDTO(SysOperationUpdateParam param);

    List<SysOperationDTO> toDTO(List<SysOperation> dos);

    SysOperationDTO toDTO(SysOperation entity);

    SysOperation toDO(SysOperationDTO dto);

    SysOperationVO toVO(SysOperationDTO dto);

    List<SysOperationVO> toVO(List<SysOperation> dos);

    SysOperationVO toVO(SysOperation entity);

    List<SysOperationVO> toVOS(List<SysOperationDTO> list);

    SysOperationAO toAO(SysOperationDTO dto);

    List<SysOperationAO> toAOList(List<SysOperationDTO> list);

}
