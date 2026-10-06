package com.carlos.monitor.instance.convert;

import com.carlos.monitor.api.pojo.ao.MonitorInstanceHealthAO;
import com.carlos.monitor.enums.MonitorEnvEnum;
import com.carlos.monitor.instance.pojo.dto.InstanceDraftDTO;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import com.carlos.monitor.instance.pojo.dto.TestConnectionDTO;
import com.carlos.monitor.instance.pojo.entity.MonitorInstance;
import com.carlos.monitor.instance.pojo.param.InstanceCreateParam;
import com.carlos.monitor.instance.pojo.param.InstanceUpdateParam;
import com.carlos.monitor.instance.pojo.vo.InstanceDraftVO;
import com.carlos.monitor.instance.pojo.vo.InstanceVO;
import com.carlos.monitor.instance.pojo.vo.TestConnectionVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 监控实例 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Mapper
public interface MonitorInstanceConvert {

    MonitorInstanceConvert INSTANCE = Mappers.getMapper(MonitorInstanceConvert.class);

    /**
     * 新增参数转 DTO
     */
    MonitorInstanceDTO toDTO(InstanceCreateParam param);

    /**
     * 修改参数转 DTO
     */
    MonitorInstanceDTO toDTO(InstanceUpdateParam param);

    /**
     * 实体转 DTO
     */
    MonitorInstanceDTO toDTO(MonitorInstance entity);

    /**
     * 实体列表转 DTO 列表
     */
    List<MonitorInstanceDTO> toDTO(List<MonitorInstance> entities);

    /**
     * DTO 转实体
     */
    MonitorInstance toDO(MonitorInstanceDTO dto);

    /**
     * DTO 转 VO（passwordCipher 不落 VO；hasPassword 由 Service/Controller 按密文是否为空单独赋值）
     */
    @Mapping(target = "hasPassword", ignore = true)
    @Mapping(target = "groupName", ignore = true)
    InstanceVO toVO(MonitorInstanceDTO dto);

    /**
     * DTO 列表转 VO 列表
     */
    List<InstanceVO> toVO(List<MonitorInstanceDTO> dtos);

    /**
     * 连接测试结果 DTO 转 VO
     */
    TestConnectionVO toVO(TestConnectionDTO dto);

    /**
     * 实例草稿 DTO 转 VO
     */
    InstanceDraftVO toVO(InstanceDraftDTO dto);

    /**
     * 实例草稿 DTO 列表转 VO 列表
     */
    List<InstanceDraftVO> toDraftVO(List<InstanceDraftDTO> dtos);

    /**
     * DTO 转健康状态 AO（供 Feign 调用方）
     */
    @Mapping(target = "instanceId", source = "id")
    MonitorInstanceHealthAO toAO(MonitorInstanceDTO dto);

    /**
     * DTO 列表转健康状态 AO 列表
     */
    List<MonitorInstanceHealthAO> toAO(List<MonitorInstanceDTO> dtos);

    /**
     * 环境枚举转存储码（AO 中 env 输出字典码 dev/test/prod，而非枚举名）
     */
    default String map(MonitorEnvEnum env) {
        return env == null ? null : env.getCode();
    }
}
