package com.carlos.monitor.servicecollector.service;

import com.carlos.monitor.collect.sink.MonitorCkQueryClient;
import com.carlos.monitor.servicecollector.pojo.vo.ServiceInstanceVO;
import com.carlos.monitor.servicecollector.pojo.vo.ServiceSummaryVO;

import java.util.List;

/**
 * <p>
 * 微服务监控 业务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
public interface ServiceMonitorService {

    /**
     * 服务清单（服务名/实例数/健康实例数/最近采集时间）
     */
    List<ServiceSummaryVO> listServices();

    /**
     * 服务实例明细（Nacos 实时 + actuator health 合并）
     */
    List<ServiceInstanceVO> listInstances(String serviceName);

    /**
     * 指标时序（支持 node 维度多实例对比）
     *
     * @param metric 指标名
     * @param range  范围：1h/6h/24h/7d
     * @param node   节点 ip:port（可空=服务级聚合点）
     */
    List<?> metricHistory(String serviceName, String metric, String range, String node);

    /**
     * 日志级别调整（高危：confirm-token + TTL 自动恢复）
     *
     * @param serviceName 服务名
     * @param address     实例地址 ip:port
     * @param loggerName  logger 名
     * @param level       目标级别
     * @param ttlSeconds  有效期（秒）
     * @param confirmToken 高危确认令牌
     */
    void updateLoggerLevel(String serviceName, String address, String loggerName,
                           String level, long ttlSeconds, String confirmToken);

    /**
     * SkyWalking 跳转链接（模板含 {service} 占位符；未配置返回空串）
     */
    String skywalkingUrl(String serviceName);
}
