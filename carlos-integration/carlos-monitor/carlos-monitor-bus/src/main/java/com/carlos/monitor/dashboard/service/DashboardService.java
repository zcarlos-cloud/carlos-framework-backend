package com.carlos.monitor.dashboard.service;

import com.carlos.monitor.dashboard.pojo.vo.AbnormalVO;
import com.carlos.monitor.dashboard.pojo.vo.AppDashboardVO;
import com.carlos.monitor.dashboard.pojo.vo.DashboardSummaryVO;
import com.carlos.monitor.dashboard.pojo.vo.DashboardTrendVO;

/**
 * <p>
 * 监控大盘聚合 业务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
public interface DashboardService {

    /**
     * 大盘汇总：实例总数/健康/异常（按类型分组）、当前告警数（按级别）、最近采集统计
     */
    DashboardSummaryVO summary();

    /**
     * 跨实例关键指标趋势（Top N 实例，CK 小时聚合）
     *
     * @param metric 指标名
     * @param range  范围：6h/24h/7d
     * @param topN   Top N 实例
     */
    DashboardTrendVO trend(String metric, String range, int topN);

    /**
     * 应用视角：关联实例列表 + 健康 + 各实例关键当前值（复用快照缓存）
     */
    AppDashboardVO appDetail(String appName);

    /**
     * 异常实例 + 最新告警滚动列表
     */
    AbnormalVO abnormal(int alarmLimit);
}
