package com.carlos.job.convert;

import com.carlos.job.pojo.dto.JobExecutionLogDTO;
import com.carlos.job.pojo.entity.JobExecutionLog;
import com.carlos.job.pojo.vo.JobExecutionLogVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 任务执行日志 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Mapper
public interface JobExecutionLogConvert {

    JobExecutionLogConvert INSTANCE = Mappers.getMapper(JobExecutionLogConvert.class);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象
     */
    JobExecutionLogDTO toDTO(JobExecutionLog entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     */
    JobExecutionLog toDO(JobExecutionLogDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     */
    JobExecutionLogVO toVO(JobExecutionLogDTO dto);

    /**
     * 持久化对象转数据显示对象
     *
     * @param entity 数据持久化对象
     * @return 数据显示对象
     */
    JobExecutionLogVO toVO(JobExecutionLog entity);

    /**
     * 持久化对象列表转数据显示对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据显示对象列表
     */
    List<JobExecutionLogVO> toVO(List<JobExecutionLog> dos);

}
