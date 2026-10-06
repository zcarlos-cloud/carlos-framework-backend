package com.carlos.job.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.job.pojo.param.JobCreateParam;
import com.carlos.job.pojo.param.JobPageParam;
import com.carlos.job.pojo.param.JobUpdateParam;
import com.carlos.job.pojo.vo.JobVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 任务调度封装接口（包装 Quartz Scheduler 操作）
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
public interface JobScheduleManager {

    /**
     * 默认任务分组
     */
    String DEFAULT_GROUP = "DEFAULT";

    /**
     * 创建任务，已存在抛异常
     *
     * @param param 任务参数
     */
    void createJob(JobCreateParam param);

    /**
     * 更新任务（cron/参数/描述），任务不存在抛异常
     *
     * @param param 任务参数
     */
    void updateJob(JobUpdateParam param);

    /**
     * 暂停任务
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     */
    void pauseJob(String jobName, String jobGroup);

    /**
     * 恢复任务
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     */
    void resumeJob(String jobName, String jobGroup);

    /**
     * 删除任务
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     */
    void deleteJob(String jobName, String jobGroup);

    /**
     * 手动触发一次
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     */
    void triggerJob(String jobName, String jobGroup);

    /**
     * 任务是否存在
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     * @return 是否存在
     */
    boolean exists(String jobName, String jobGroup);

    /**
     * 查询任务详情
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     * @return 任务视图对象，不存在返回 null
     */
    JobVO getJobInfo(String jobName, String jobGroup);

    /**
     * 分页查询任务列表（基于 Scheduler API 内存分页，不直连 qrtz_* 表）
     *
     * @param param 分页参数
     * @return 分页结果
     */
    Paging<JobVO> getPage(JobPageParam param);

    /**
     * 预览未来触发时间（用于 cron/间隔配置的前端即时预览）
     *
     * @param scheduleType 调度类型: 1-CRON 2-固定间隔 3-一次性
     * @param scheduleConf 调度配置
     * @param count        预览条数（<=0 默认 5，上限 20）
     * @return 未来触发时间列表
     */
    List<LocalDateTime> previewFireTimes(Integer scheduleType, String scheduleConf, int count);

}
