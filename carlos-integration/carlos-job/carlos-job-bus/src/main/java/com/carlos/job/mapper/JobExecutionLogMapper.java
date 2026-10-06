package com.carlos.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.job.pojo.entity.JobExecutionLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 任务执行日志 Mapper 接口
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Mapper
public interface JobExecutionLogMapper extends BaseMapper<JobExecutionLog> {

}
