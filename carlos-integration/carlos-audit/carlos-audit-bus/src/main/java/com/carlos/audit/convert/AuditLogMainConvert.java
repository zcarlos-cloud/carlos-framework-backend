package com.carlos.audit.convert;

import com.carlos.audit.api.pojo.ao.AuditLogMainAO;
import com.carlos.audit.api.pojo.ao.AuditLogStatsAO;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.audit.pojo.entity.AuditLogMain;
import com.carlos.audit.pojo.param.AuditLogMainCreateParam;
import com.carlos.audit.pojo.param.AuditLogMainUpdateParam;
import com.carlos.audit.pojo.vo.AuditLogMainVO;
import com.carlos.audit.pojo.vo.AuditLogStatsVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 审计日志宽主表（合并数据变更、技术上下文、标签、附件，保留7天热数据） 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Mapper(uses = {CommonConvert.class})
public interface AuditLogMainConvert {

    AuditLogMainConvert INSTANCE = Mappers.getMapper(AuditLogMainConvert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogMainDTO toDTO(AuditLogMainCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogMainDTO toDTO(AuditLogMainUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    List<AuditLogMainDTO> toDTO(List<AuditLogMain> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogMainDTO toDTO(AuditLogMain entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogMain toDO(AuditLogMainDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogMainVO toVO(AuditLogMainDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    List<AuditLogMainVO> toVO(List<AuditLogMain> dos);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param entity 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogMainVO toVO(AuditLogMain entity);

    /**
     * 数据传输对象转 API 输出对象
     *
     * @param dto 数据传输对象
     * @return API 输出对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogMainAO toAO(AuditLogMainDTO dto);

    /**
     * 数据持久对象转 API 输出对象
     *
     * @param entity 数据持久化对象
     * @return API 输出对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogMainAO toAO(AuditLogMain entity);

    /**
     * 数据传输对象列表转 API 输出对象列表
     *
     * @param dtos 数据传输对象列表
     * @return API 输出对象列表
     */
    List<AuditLogMainAO> toAO(List<AuditLogMainDTO> dtos);

    /**
     * 统计 VO 转 API 输出对象
     *
     * @param vo 统计 VO
     * @return API 输出对象
     */
    AuditLogStatsAO toAO(AuditLogStatsVO vo);

    /**
     * 接口参数对象转数据传输对象
     *
     * @param param 接口参数对象
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogMainDTO toDTO(ApiAuditLogMainParam param);

    /**
     * 复制数据传输对象
     *
     * @param source 源对象
     * @return 新的数据传输对象
     */
    AuditLogMainDTO copy(AuditLogMainDTO source);
}
