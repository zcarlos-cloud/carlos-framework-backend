package com.carlos.job.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.job.convert.JobInfoConvert;
import com.carlos.job.manager.JobInfoManager;
import com.carlos.job.manager.JobScheduleManager;
import com.carlos.job.mapper.JobInfoMapper;
import com.carlos.job.pojo.dto.JobInfoDTO;
import com.carlos.job.pojo.entity.JobInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>
 * 定时任务元数据 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class JobInfoManagerImpl extends BaseServiceImpl<JobInfoMapper, JobInfo> implements JobInfoManager {

    @Override
    public boolean add(JobInfoDTO dto) {
        JobInfo entity = JobInfoConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'JobInfo' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        log.debug("Insert 'JobInfo' data: id:{}", entity.getId());
        return true;
    }

    @Override
    public JobInfoDTO getByJobKey(String jobName, String jobGroup) {
        if (StrUtil.isBlank(jobName)) {
            log.warn("jobName is null");
            return null;
        }
        JobInfo entity = getOne(jobKeyWrapper(jobName, jobGroup), false);
        return JobInfoConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public boolean updateByJobKey(JobInfoDTO dto) {
        JobInfo entity = JobInfoConvert.INSTANCE.toDO(dto);
        // id 置空，避免按主键更新错位；以业务键定位
        entity.setId(null);
        return update(entity, jobKeyWrapper(dto.getJobName(), dto.getJobGroup()));
    }

    @Override
    public boolean updateStatus(String jobName, String jobGroup, String status) {
        JobInfo entity = new JobInfo().setStatus(status);
        return update(entity, jobKeyWrapper(jobName, jobGroup));
    }

    @Override
    public boolean removeByJobKey(String jobName, String jobGroup) {
        return remove(jobKeyWrapper(jobName, jobGroup));
    }

    @Override
    public List<JobInfoDTO> listAll() {
        return JobInfoConvert.INSTANCE.toDTO(list());
    }

    /**
     * 业务键查询条件（分组空值兜底 DEFAULT，与 Quartz 侧一致）
     */
    private LambdaQueryWrapper<JobInfo> jobKeyWrapper(String jobName, String jobGroup) {
        LambdaQueryWrapper<JobInfo> wrapper = queryWrapper();
        wrapper.eq(JobInfo::getJobName, jobName);
        wrapper.eq(JobInfo::getJobGroup, StrUtil.blankToDefault(jobGroup, JobScheduleManager.DEFAULT_GROUP));
        return wrapper;
    }

}
