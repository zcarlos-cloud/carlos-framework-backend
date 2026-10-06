package com.carlos.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.message.manager.MessageRecordManager;
import com.carlos.message.manager.MessageSendLogManager;
import com.carlos.message.pojo.entity.MessageRecord;
import com.carlos.message.pojo.entity.MessageSendLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 消息统计服务
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageStatsService {

    private final MessageRecordManager messageRecordManager;
    private final MessageSendLogManager messageSendLogManager;

    /**
     * 获取今日发送统计
     *
     * @return 统计结果
     */
    public Map<String, Object> getTodayStats() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return getStatsByTimeRange(startOfDay, LocalDateTime.now());
    }

    /**
     * 获取指定时间范围的统计
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 统计结果
     */
    public Map<String, Object> getStatsByTimeRange(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> result = new HashMap<>();

        // 1. 消息记录统计
        LambdaQueryWrapper<MessageRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.ge(MessageRecord::getCreateTime, start)
            .le(MessageRecord::getCreateTime, end);
        long totalRecords = messageRecordManager.count(recordWrapper);

        // 2. 发送日志统计
        LambdaQueryWrapper<MessageSendLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.ge(MessageSendLog::getCreateTime, start)
            .le(MessageSendLog::getCreateTime, end);
        long totalLogs = messageSendLogManager.count(logWrapper);

        LambdaQueryWrapper<MessageSendLog> successWrapper = new LambdaQueryWrapper<>();
        successWrapper.ge(MessageSendLog::getCreateTime, start)
            .le(MessageSendLog::getCreateTime, end)
            .eq(MessageSendLog::getSuccess, true);
        long successLogs = messageSendLogManager.count(successWrapper);

        LambdaQueryWrapper<MessageSendLog> failWrapper = new LambdaQueryWrapper<>();
        failWrapper.ge(MessageSendLog::getCreateTime, start)
            .le(MessageSendLog::getCreateTime, end)
            .eq(MessageSendLog::getSuccess, false);
        long failLogs = messageSendLogManager.count(failWrapper);

        // 3. 平均耗时
        LambdaQueryWrapper<MessageSendLog> avgWrapper = new LambdaQueryWrapper<>();
        avgWrapper.ge(MessageSendLog::getCreateTime, start)
            .le(MessageSendLog::getCreateTime, end);

        result.put("totalRecords", totalRecords);
        result.put("totalSends", totalLogs);
        result.put("successCount", successLogs);
        result.put("failCount", failLogs);
        result.put("successRate", totalLogs > 0 ? Math.round((double) successLogs / totalLogs * 10000) / 100.0 : 0.0);
        result.put("startTime", start);
        result.put("endTime", end);

        return result;
    }

    /**
     * 获取渠道发送分布
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 各渠道发送数量
     */
    public Map<String, Long> getChannelDistribution(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> distribution = new HashMap<>();
        // 实际应通过 Mapper 自定义 SQL 分组统计
        // 简化处理：查询日志后内存分组
        LambdaQueryWrapper<MessageSendLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(MessageSendLog::getCreateTime, start)
            .le(MessageSendLog::getCreateTime, end);
        var logs = messageSendLogManager.list(wrapper);
        for (MessageSendLog log : logs) {
            String channelCode = log.getChannelCode() != null ? log.getChannelCode() : "unknown";
            distribution.merge(channelCode, 1L, Long::sum);
        }
        return distribution;
    }
}
