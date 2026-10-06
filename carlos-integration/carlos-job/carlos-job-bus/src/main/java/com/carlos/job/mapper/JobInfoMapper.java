package com.carlos.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.job.pojo.entity.JobInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 定时任务元数据 Mapper 接口
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Mapper
public interface JobInfoMapper extends BaseMapper<JobInfo> {

}
