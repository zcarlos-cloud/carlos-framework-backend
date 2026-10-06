package com.carlos.message.controller;

import com.carlos.core.response.Result;
import com.carlos.message.service.MessageStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * <p>
 * 消息统计接口
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@RestController
@RequestMapping("/message/stats")
@RequiredArgsConstructor
@Tag(name = "消息统计")
public class MessageStatsController {

    private final MessageStatsService messageStatsService;

    @PreAuthorize("hasPermission('message:stats', 'view')")
    @GetMapping("/today")
    @Operation(summary = "今日发送统计")
    public Result<Map<String, Object>> todayStats() {
        return Result.success(messageStatsService.getTodayStats());
    }

    @PreAuthorize("hasPermission('message:stats', 'view')")
    @GetMapping("/range")
    @Operation(summary = "时间范围统计")
    public Result<Map<String, Object>> rangeStats(
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        return Result.success(messageStatsService.getStatsByTimeRange(start, end));
    }

    @PreAuthorize("hasPermission('message:stats', 'view')")
    @GetMapping("/channel-distribution")
    @Operation(summary = "渠道发送分布")
    public Result<Map<String, Long>> channelDistribution(
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        return Result.success(messageStatsService.getChannelDistribution(start, end));
    }
}
