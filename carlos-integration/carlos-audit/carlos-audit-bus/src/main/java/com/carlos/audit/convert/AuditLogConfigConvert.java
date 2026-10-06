package com.carlos.audit.convert;

import com.carlos.audit.api.pojo.ao.AuditLogConfigAO;
import com.carlos.audit.api.pojo.param.ApiAuditLogConfigParam;
import com.carlos.audit.pojo.dto.AuditLogConfigDTO;
import com.carlos.audit.pojo.entity.AuditLogConfig;
import com.carlos.audit.pojo.param.AuditLogConfigCreateParam;
import com.carlos.audit.pojo.param.AuditLogConfigUpdateParam;
import com.carlos.audit.pojo.vo.AuditLogConfigVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 审计日志配置（动态TTL与采样策略） 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Mapper(uses = {CommonConvert.class})
public interface AuditLogConfigConvert {

    AuditLogConfigConvert INSTANCE = Mappers.getMapper(AuditLogConfigConvert.class);

    /**
     * API 参数对象转数据传输对象
     *
     * @param param API 参数
     * @return 数据传输对象
     */
    AuditLogConfigDTO toDTO(ApiAuditLogConfigParam param);

    /**
     * 数据传输对象转 API 输出对象
     *
     * @param dto 数据传输对象
     * @return API 输出对象
     */
    AuditLogConfigAO toAO(AuditLogConfigDTO dto);

    /**
     * 数据传输对象列表转 API 输出对象列表
     *
     * @param dtos 数据传输对象列表
     * @return API 输出对象列表
     */
    List<AuditLogConfigAO> toAO(List<AuditLogConfigDTO> dtos);

    /**
     * 数据显示对象转 API 输出对象
     *
     * @param vo 数据显示对象
     * @return API 输出对象
     */
    AuditLogConfigAO toAO(AuditLogConfigVO vo);

    /**
     * 数据显示对象列表转 API 输出对象列表
     *
     * @param vos 数据显示对象列表
     * @return API 输出对象列表
     */
    List<AuditLogConfigAO> toAOFromVO(List<AuditLogConfigVO> vos);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogConfigDTO toDTO(AuditLogConfigCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogConfigDTO toDTO(AuditLogConfigUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    List<AuditLogConfigDTO> toDTO(List<AuditLogConfig> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogConfigDTO toDTO(AuditLogConfig entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogConfig toDO(AuditLogConfigDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogConfigVO toVO(AuditLogConfigDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    List<AuditLogConfigVO> toVO(List<AuditLogConfig> dos);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param entity 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogConfigVO toVO(AuditLogConfig entity);
}
