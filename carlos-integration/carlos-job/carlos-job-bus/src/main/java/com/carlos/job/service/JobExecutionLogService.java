package com.carlos.job.service;

import com.carlos.core.pagination.Paging;
import com.carlos.job.enums.JobErrorCode;
import com.carlos.job.manager.JobExecutionLogManager;
import com.carlos.job.pojo.dto.JobExecutionLogDTO;
import com.carlos.job.pojo.param.JobLogPageParam;
import com.carlos.job.pojo.vo.JobExecutionLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 任务执行日志 业务
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutionLogService {

    private final JobExecutionLogManager executionLogManager;

    /**
     * 日志详情
     *
     * @param id 主键ID
     * @return 日志DTO
     */
    public JobExecutionLogDTO getLogDetail(Long id) {
        JobExecutionLogDTO dto = executionLogManager.getDtoById(id);
        if (dto == null) {
            throw JobErrorCode.JOB_LOG_NOT_FOUND.exception();
        }
        return dto;
    }

    /**
     * 分页查询日志
     *
     * @param param 分页参数
     * @return 分页结果
     */
    public Paging<JobExecutionLogVO> getPage(JobLogPageParam param) {
        return executionLogManager.getPage(param);
    }

    /**
     * 清理历史日志
     *
     * @param retainDays 保留天数
     * @return 清理条数
     */
    public int cleanLogs(int retainDays) {
        return executionLogManager.cleanBefore(retainDays);
    }

}
