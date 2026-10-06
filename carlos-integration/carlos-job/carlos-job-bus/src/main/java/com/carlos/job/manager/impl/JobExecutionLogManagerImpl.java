package com.carlos.job.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.job.convert.JobExecutionLogConvert;
import com.carlos.job.manager.JobExecutionLogManager;
import com.carlos.job.mapper.JobExecutionLogMapper;
import com.carlos.job.pojo.dto.JobExecutionLogDTO;
import com.carlos.job.pojo.entity.JobExecutionLog;
import com.carlos.job.pojo.param.JobLogPageParam;
import com.carlos.job.pojo.vo.JobExecutionLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 任务执行日志 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class JobExecutionLogManagerImpl extends BaseServiceImpl<JobExecutionLogMapper, JobExecutionLog>
    implements JobExecutionLogManager {

    /**
     * 单页最大条数上限，防大查询拖库
     */
    private static final int MAX_PAGE_SIZE = 500;

    @Override
    public boolean add(JobExecutionLogDTO dto) {
        JobExecutionLog entity = JobExecutionLogConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'JobExecutionLog' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        log.debug("Insert 'JobExecutionLog' data: id:{}", entity.getId());
        return true;
    }

    @Override
    public JobExecutionLogDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        JobExecutionLog entity = getBaseMapper().selectById(id);
        return JobExecutionLogConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<JobExecutionLogVO> getPage(JobLogPageParam param) {
        // 入参规范化：钳制单页上限（ParamPage 无上限约束，本模块兜底）
        param.setSize(Math.min(param.getSize(), MAX_PAGE_SIZE));
        LambdaQueryWrapper<JobExecutionLog> wrapper = queryWrapper();
        wrapper.select(
            JobExecutionLog::getId,
            JobExecutionLog::getJobName,
            JobExecutionLog::getJobGroup,
            JobExecutionLog::getServiceName,
            JobExecutionLog::getHandlerName,
            JobExecutionLog::getTriggerType,
            JobExecutionLog::getCronExpression,
            JobExecutionLog::getStatus,
            JobExecutionLog::getStartTime,
            JobExecutionLog::getEndTime,
            JobExecutionLog::getDurationMs,
            JobExecutionLog::getResultMessage,
            JobExecutionLog::getServerNode
        );
        if (StrUtil.isNotBlank(param.getJobName())) {
            wrapper.like(JobExecutionLog::getJobName, param.getJobName());
        }
        if (StrUtil.isNotBlank(param.getJobGroup())) {
            wrapper.eq(JobExecutionLog::getJobGroup, param.getJobGroup());
        }
        if (StrUtil.isNotBlank(param.getServiceName())) {
            wrapper.eq(JobExecutionLog::getServiceName, param.getServiceName());
        }
        if (StrUtil.isNotBlank(param.getHandlerName())) {
            wrapper.like(JobExecutionLog::getHandlerName, param.getHandlerName());
        }
        if (param.getStatus() != null) {
            wrapper.eq(JobExecutionLog::getStatus, param.getStatus());
        }
        if (param.getStart() != null) {
            wrapper.ge(JobExecutionLog::getStartTime, param.getStart());
        }
        if (param.getEnd() != null) {
            wrapper.le(JobExecutionLog::getStartTime, param.getEnd());
        }
        // 分页必须有稳定排序，否则 PostgreSQL 下翻页结果可能重复或漏行
        wrapper.orderByDesc(JobExecutionLog::getStartTime).orderByDesc(JobExecutionLog::getId);
        PageInfo<JobExecutionLog> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, JobExecutionLogConvert.INSTANCE::toVO);
    }

    @Override
    public int cleanBefore(int retainDays) {
        LocalDateTime deadline = LocalDateTime.now().minusDays(retainDays);
        LambdaQueryWrapper<JobExecutionLog> wrapper = queryWrapper();
        wrapper.lt(JobExecutionLog::getStartTime, deadline);
        int count = getBaseMapper().delete(wrapper);
        log.info("Clean job execution logs before {}, removed: {}", deadline, count);
        return count;
    }

    @Override
    public Map<Integer, Long> countByStatus(LocalDateTime start, LocalDateTime end) {
        QueryWrapper<JobExecutionLog> wrapper = new QueryWrapper<>();
        wrapper.select("status AS status", "COUNT(*) AS cnt");
        applyTimeRange(wrapper, start, end);
        wrapper.groupBy("status");
        Map<Integer, Long> result = new HashMap<>();
        for (Map<String, Object> row : getBaseMapper().selectMaps(wrapper)) {
            result.put(toInt(row.get("status")), toLong(row.get("cnt")));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> countDailyByStatus(LocalDateTime start, LocalDateTime end) {
        QueryWrapper<JobExecutionLog> wrapper = new QueryWrapper<>();
        // PostgreSQL 按日期分组 + 条件聚合，分离成功/失败两列
        // 别名必须全小写：PostgreSQL 会把未加引号的别名折叠为小写（successCount -> successcount），
        // 而 selectMaps 的 Map 结果不会做下划线转驼峰（MapWrapper.findProperty 原样返回 key），
        // 写成驼峰会让取值恒为 null（曾导致趋势图每日数据全为 0）
        wrapper.select(
            "to_char(start_time, 'YYYY-MM-DD') AS date",
            "COUNT(*) FILTER (WHERE status = 1) AS success_count",
            "COUNT(*) FILTER (WHERE status = 2) AS fail_count"
        );
        applyTimeRange(wrapper, start, end);
        wrapper.groupBy("to_char(start_time, 'YYYY-MM-DD')");
        wrapper.orderByAsc("date");
        return getBaseMapper().selectMaps(wrapper);
    }

    private void applyTimeRange(QueryWrapper<JobExecutionLog> wrapper, LocalDateTime start, LocalDateTime end) {
        if (start != null) {
            wrapper.ge("start_time", start);
        }
        if (end != null) {
            wrapper.le("start_time", end);
        }
    }

    private Integer toInt(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private Long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

}
