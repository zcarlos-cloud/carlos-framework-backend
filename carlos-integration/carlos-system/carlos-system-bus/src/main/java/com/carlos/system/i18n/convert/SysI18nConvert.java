package com.carlos.system.i18n.convert;

import com.carlos.system.i18n.pojo.dto.SysI18nMessageDTO;
import com.carlos.system.i18n.pojo.entity.SysI18nMessage;
import com.carlos.system.i18n.pojo.param.SysI18nCreateParam;
import com.carlos.system.i18n.pojo.param.SysI18nUpdateParam;
import com.carlos.system.i18n.pojo.vo.SysI18nMessageVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 国际化消息 转换器
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
@Mapper
public interface SysI18nConvert {

    SysI18nConvert INSTANCE = Mappers.getMapper(SysI18nConvert.class);

    SysI18nMessageDTO toDTO(SysI18nCreateParam param);

    SysI18nMessageDTO toDTO(SysI18nUpdateParam param);

    List<SysI18nMessageDTO> toDTO(List<SysI18nMessage> dos);

    SysI18nMessageDTO toDTO(SysI18nMessage entity);

    SysI18nMessage toDO(SysI18nMessageDTO dto);

    SysI18nMessageVO toVO(SysI18nMessageDTO dto);

    List<SysI18nMessageVO> toVO(List<SysI18nMessageDTO> dtos);

    List<SysI18nMessageVO> do2vo(List<SysI18nMessage> sysI18nMessages);
}
