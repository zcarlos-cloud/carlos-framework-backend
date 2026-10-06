package com.carlos.job.convert;

import com.carlos.job.pojo.dto.JobInfoDTO;
import com.carlos.job.pojo.entity.JobInfo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 定时任务元数据 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Mapper
public interface JobInfoConvert {

    JobInfoConvert INSTANCE = Mappers.getMapper(JobInfoConvert.class);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象
     */
    JobInfoDTO toDTO(JobInfo entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     */
    JobInfo toDO(JobInfoDTO dto);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     */
    List<JobInfoDTO> toDTO(List<JobInfo> dos);

}
