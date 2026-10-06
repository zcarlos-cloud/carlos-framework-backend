package com.carlos.audit.convert;

import com.carlos.audit.api.pojo.ao.AuditLogArchiveRecordAO;
import com.carlos.audit.api.pojo.param.ApiAuditLogArchiveParam;
import com.carlos.audit.pojo.dto.AuditLogArchiveRecordDTO;
import com.carlos.audit.pojo.entity.AuditLogArchiveRecord;
import com.carlos.audit.pojo.param.AuditLogArchiveRecordCreateParam;
import com.carlos.audit.pojo.param.AuditLogArchiveRecordUpdateParam;
import com.carlos.audit.pojo.vo.AuditLogArchiveRecordVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 审计日志归档记录（管理冷数据归档） 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Mapper(uses = {CommonConvert.class})
public interface AuditLogArchiveRecordConvert {

    AuditLogArchiveRecordConvert INSTANCE = Mappers.getMapper(AuditLogArchiveRecordConvert.class);

    /**
     * API 参数对象转数据传输对象
     *
     * @param param API 参数
     * @return 数据传输对象
     */
    AuditLogArchiveRecordDTO toDTO(ApiAuditLogArchiveParam param);

    /**
     * 数据传输对象转 API 输出对象
     *
     * @param dto 数据传输对象
     * @return API 输出对象
     */
    AuditLogArchiveRecordAO toAO(AuditLogArchiveRecordDTO dto);

    /**
     * 数据传输对象列表转 API 输出对象列表
     *
     * @param dtos 数据传输对象列表
     * @return API 输出对象列表
     */
    List<AuditLogArchiveRecordAO> toAO(List<AuditLogArchiveRecordDTO> dtos);

    /**
     * 数据显示对象转 API 输出对象
     *
     * @param vo 数据显示对象
     * @return API 输出对象
     */
    AuditLogArchiveRecordAO toAO(AuditLogArchiveRecordVO vo);

    /**
     * 数据显示对象列表转 API 输出对象列表
     *
     * @param vos 数据显示对象列表
     * @return API 输出对象列表
     */
    List<AuditLogArchiveRecordAO> toAOFromVO(List<AuditLogArchiveRecordVO> vos);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogArchiveRecordDTO toDTO(AuditLogArchiveRecordCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogArchiveRecordDTO toDTO(AuditLogArchiveRecordUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    List<AuditLogArchiveRecordDTO> toDTO(List<AuditLogArchiveRecord> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogArchiveRecordDTO toDTO(AuditLogArchiveRecord entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogArchiveRecord toDO(AuditLogArchiveRecordDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogArchiveRecordVO toVO(AuditLogArchiveRecordDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    List<AuditLogArchiveRecordVO> toVO(List<AuditLogArchiveRecord> dos);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param entity 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogArchiveRecordVO toVO(AuditLogArchiveRecord entity);
}
