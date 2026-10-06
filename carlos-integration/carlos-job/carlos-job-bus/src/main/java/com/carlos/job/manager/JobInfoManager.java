package com.carlos.job.manager;

import com.carlos.datasource.base.BaseService;
import com.carlos.job.pojo.dto.JobInfoDTO;
import com.carlos.job.pojo.entity.JobInfo;

import java.util.List;

/**
 * <p>
 * 定时任务元数据 查询封装接口（控制面）
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
public interface JobInfoManager extends BaseService<JobInfo> {

    /**
     * 新增任务元数据
     *
     * @param dto 任务元数据
     * @return boolean
     */
    boolean add(JobInfoDTO dto);

    /**
     * 按业务键查询任务元数据
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组（空则默认 DEFAULT）
     * @return 任务元数据，不存在返回 null
     */
    JobInfoDTO getByJobKey(String jobName, String jobGroup);

    /**
     * 按业务键更新任务元数据（只更新非空字段）
     *
     * @param dto 任务元数据（jobName/jobGroup 必填定位）
     * @return boolean
     */
    boolean updateByJobKey(JobInfoDTO dto);

    /**
     * 按业务键更新任务状态
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     * @param status   Quartz 触发器状态
     * @return boolean
     */
    boolean updateStatus(String jobName, String jobGroup, String status);

    /**
     * 按业务键删除任务元数据（逻辑删除）
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     * @return boolean
     */
    boolean removeByJobKey(String jobName, String jobGroup);

    /**
     * 全量任务元数据（对账用）
     *
     * @return 任务元数据列表
     */
    List<JobInfoDTO> listAll();

}
