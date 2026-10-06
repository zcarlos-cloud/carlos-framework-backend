package com.carlos.job.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.job.pojo.dto.JobExecutionLogDTO;
import com.carlos.job.pojo.entity.JobExecutionLog;
import com.carlos.job.pojo.param.JobLogPageParam;
import com.carlos.job.pojo.vo.JobExecutionLogVO;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 任务执行日志 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
public interface JobExecutionLogManager extends BaseService<JobExecutionLog> {

    /**
     * 新增执行日志
     *
     * @param dto 执行日志数据
     * @return boolean
     */
    boolean add(JobExecutionLogDTO dto);

    /**
     * 获取日志详情
     *
     * @param id 主键id
     * @return 日志DTO
     */
    JobExecutionLogDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param param 分页参数
     * @return 分页结果
     */
    Paging<JobExecutionLogVO> getPage(JobLogPageParam param);

    /**
     * 清理指定天数之前的历史日志
     *
     * @param retainDays 保留天数
     * @return 清理条数
     */
    int cleanBefore(int retainDays);

    /**
     * 统计时间范围内按执行状态分组的数量
     *
     * @param start 起始时间（含）
     * @param end   结束时间（含），为空则不限
     * @return 状态 -> 数量
     */
    Map<Integer, Long> countByStatus(LocalDateTime start, LocalDateTime end);

    /**
     * 统计时间范围内每日成功/失败次数（按 start_time 日期分组）
     *
     * @param start 起始时间（含）
     * @param end   结束时间（含）
     * @return 每日分布，元素含 date/success_count/fail_count（key 为下划线小写，与 SQL 别名一致）
     */
    List<Map<String, Object>> countDailyByStatus(LocalDateTime start, LocalDateTime end);

}
