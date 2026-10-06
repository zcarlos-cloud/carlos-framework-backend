package com.carlos.system.menu.convert;

import com.carlos.system.menu.pojo.dto.SysModuleDTO;
import com.carlos.system.menu.pojo.entity.SysModule;
import com.carlos.system.menu.pojo.param.SysModuleCreateParam;
import com.carlos.system.menu.pojo.param.SysModuleUpdateParam;
import com.carlos.system.menu.pojo.vo.SysModuleVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 功能模块 转换器
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
@Mapper
public interface SysModuleConvert {

    SysModuleConvert INSTANCE = Mappers.getMapper(SysModuleConvert.class);

    SysModuleDTO toDTO(SysModuleCreateParam param);

    SysModuleDTO toDTO(SysModuleUpdateParam param);

    List<SysModuleDTO> toDTO(List<SysModule> dos);

    SysModuleDTO toDTO(SysModule entity);

    SysModule toDO(SysModuleDTO dto);

    SysModuleVO toVO(SysModuleDTO dto);

    List<SysModuleVO> toVO(List<SysModuleDTO> dtos);

    List<SysModuleVO> do2vo(List<SysModule> sysModules);
}
