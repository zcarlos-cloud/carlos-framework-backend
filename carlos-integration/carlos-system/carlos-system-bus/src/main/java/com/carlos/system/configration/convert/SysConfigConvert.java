package com.carlos.system.configration.convert;

import com.carlos.system.config.CommonConvert;
import com.carlos.system.configration.pojo.dto.SysConfigDTO;
import com.carlos.system.configration.pojo.entity.SysConfig;
import com.carlos.system.configration.pojo.excel.SysConfigExcel;
import com.carlos.system.configration.pojo.param.SysConfigCreateParam;
import com.carlos.system.configration.pojo.param.SysConfigUpdateParam;
import com.carlos.system.configration.pojo.vo.SysConfigVO;
import com.carlos.system.enums.ConfigValueType;
import com.carlos.system.pojo.ao.SysConfigAO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 系统配置 转换器
 * </p>
 *
 * @author carlos
 * @date 2022-11-3 13:47:54
 */
@Mapper(uses = {CommonConvert.class})
public interface SysConfigConvert {

    SysConfigConvert INSTANCE = Mappers.getMapper(SysConfigConvert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author carlos
     * @date 2022-11-3 13:47:54
     */
    SysConfigDTO toDTO(SysConfigCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author carlos
     * @date 2022-11-3 13:47:54
     */
    SysConfigDTO toDTO(SysConfigUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author carlos
     * @date 2022-11-3 13:47:54
     */
    List<SysConfigDTO> toDTO(List<SysConfig> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author carlos
     * @date 2022-11-3 13:47:54
     */
    SysConfigDTO toDTO(SysConfig entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author carlos
     * @date 2022-11-3 13:47:54
     */
    SysConfig toDO(SysConfigDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author carlos
     * @date 2022-11-3 13:47:54
     */
    SysConfigVO toVO(SysConfigDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author carlos
     * @date 2022-11-3 13:47:54
     */
    List<SysConfigVO> toVO(List<SysConfig> dos);

    SysConfigAO toAO(SysConfigDTO config);

    // Excel conversions

    @Mapping(source = "valueType", target = "valueType", qualifiedByName = "valueTypeToExcel")
    @Mapping(source = "configStatus", target = "configStatus", qualifiedByName = "configStatusToExcel")
    SysConfigExcel toExcel(SysConfigDTO dto);

    List<SysConfigExcel> dtoToExcel(List<SysConfigDTO> list);

    @Mapping(source = "valueType", target = "valueType", qualifiedByName = "valueTypeToDTO")
    @Mapping(source = "configStatus", target = "configStatus", qualifiedByName = "configStatusToDTO")
    SysConfigDTO toDTO(SysConfigExcel excel);

    List<SysConfigDTO> excelToDTO(List<SysConfigExcel> list);

    @Named("valueTypeToExcel")
    default String valueTypeToExcel(ConfigValueType valueType) {
        if (valueType == null) {
            return null;
        }
        return valueType.getDesc();
    }

    @Named("valueTypeToDTO")
    default ConfigValueType valueTypeToDTO(String valueType) {
        if (valueType == null || valueType.isEmpty()) {
            return null;
        }
        String trimmed = valueType.trim();
        for (ConfigValueType type : ConfigValueType.values()) {
            if (type.getDesc().equals(trimmed) || String.valueOf(type.getCode()).equals(trimmed)) {
                return type;
            }
        }
        return null;
    }

    @Named("configStatusToExcel")
    default String configStatusToExcel(Boolean configStatus) {
        if (configStatus == null) {
            return null;
        }
        return Boolean.TRUE.equals(configStatus) ? "启用" : "禁用";
    }

    @Named("configStatusToDTO")
    default Boolean configStatusToDTO(String configStatus) {
        if (configStatus == null) {
            return null;
        }
        String trimmed = configStatus.trim();
        return "启用".equals(trimmed) || "是".equals(trimmed) || "true".equalsIgnoreCase(trimmed) || "1".equals(trimmed);
    }
}
