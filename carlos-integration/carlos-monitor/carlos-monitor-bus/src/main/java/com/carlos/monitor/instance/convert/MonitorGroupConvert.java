package com.carlos.monitor.instance.convert;

import com.carlos.monitor.instance.pojo.dto.MonitorInstanceGroupDTO;
import com.carlos.monitor.instance.pojo.entity.MonitorInstanceGroup;
import com.carlos.monitor.instance.pojo.param.GroupCreateParam;
import com.carlos.monitor.instance.pojo.param.GroupUpdateParam;
import com.carlos.monitor.instance.pojo.vo.InstanceGroupVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 监控实例分组 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Mapper
public interface MonitorGroupConvert {

    MonitorGroupConvert INSTANCE = Mappers.getMapper(MonitorGroupConvert.class);

    /**
     * 新增参数转 DTO
     */
    MonitorInstanceGroupDTO toDTO(GroupCreateParam param);

    /**
     * 修改参数转 DTO
     */
    MonitorInstanceGroupDTO toDTO(GroupUpdateParam param);

    /**
     * 实体转 DTO
     */
    MonitorInstanceGroupDTO toDTO(MonitorInstanceGroup entity);

    /**
     * 实体列表转 DTO 列表
     */
    List<MonitorInstanceGroupDTO> toDTO(List<MonitorInstanceGroup> entities);

    /**
     * DTO 转实体
     */
    MonitorInstanceGroup toDO(MonitorInstanceGroupDTO dto);

    /**
     * DTO 转 VO
     */
    InstanceGroupVO toVO(MonitorInstanceGroupDTO dto);

    /**
     * DTO 列表转 VO 列表
     */
    List<InstanceGroupVO> toVO(List<MonitorInstanceGroupDTO> dtos);
}
