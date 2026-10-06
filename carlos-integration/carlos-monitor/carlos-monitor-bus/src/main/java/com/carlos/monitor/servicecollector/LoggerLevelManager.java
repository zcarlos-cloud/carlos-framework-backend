package com.carlos.monitor.servicecollector;

import cn.hutool.core.util.StrUtil;
import com.carlos.json.JsonUtils;
import com.carlos.monitor.config.MonitorProperties;
import com.carlos.monitor.enums.MonitorErrorCode;
import com.carlos.monitor.util.MonitorCacheUtil;
import com.carlos.redis.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 日志级别调整管理
 * 调整前记录原级别到 Redis（monitor:logger:ttl:{service}:{addr}:{logger}，含过期时间），
 * 到期由 ServiceRegistrySyncTask 调用 restoreExpired() 自动恢复原级别
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@Component
public class LoggerLevelManager {

    /**
     * 级别调整记录键前缀
     */
    private static final String LOGGER_TTL_PREFIX = "monitor:logger:ttl:";

    private final ActuatorClient actuatorClient;

    private final MonitorProperties monitorProperties;

    public LoggerLevelManager(ActuatorClient actuatorClient, MonitorProperties monitorProperties) {
        this.actuatorClient = actuatorClient;
        this.monitorProperties = monitorProperties;
    }

    /**
     * 调整目标实例日志级别（先记录原级别，TTL 到期自动恢复）
     *
     * @param serviceName 服务名
     * @param address     实例地址 ip:port
     * @param loggerName  logger 名
     * @param level       目标级别（TRACE/DEBUG/INFO/WARN/ERROR/OFF；null 表示重置）
     * @param ttlSeconds  有效期（秒，到期自动恢复原级别）
     */
    public void applyLevel(String serviceName, String address, String loggerName,
                           String level, long ttlSeconds) {
        // 1. 读取当前级别作为原级别
        String originalLevel = actuatorClient.getLoggerLevel(address, loggerName);
        // 2. 下发调整
        actuatorClient.setLoggerLevel(address, loggerName, level);
        // 3. 记录恢复信息（键 TTL 兜底，value 内含过期时间供 sweep 判断）
        Map<String, Object> record = new HashMap<>();
        record.put("service", serviceName);
        record.put("address", address);
        record.put("logger", loggerName);
        record.put("originalLevel", originalLevel);
        record.put("expireAt", System.currentTimeMillis() + ttlSeconds * 1000);
        String key = LOGGER_TTL_PREFIX + serviceName + ":" + address + ":" + loggerName;
        RedisUtil.setValue(key, JsonUtils.toJson(record), ttlSeconds + 60);
        log.info("日志级别已调整, service:{}, addr:{}, logger:{}: {} -> {} (ttl {}s)",
            serviceName, address, loggerName, originalLevel, level, ttlSeconds);
    }

    /**
     * 扫描到期记录并恢复原级别（由 ServiceRegistrySyncTask 周期调用）
     */
    public void restoreExpired() {
        Set<String> keys = RedisUtil.scanKeys(LOGGER_TTL_PREFIX + "*", 200);
        if (keys == null || keys.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (String key : keys) {
            try {
                String json = MonitorCacheUtil.getString(key);
                if (StrUtil.isBlank(json) || !JsonUtils.isValidJson(json)) {
                    RedisUtil.delete(key);
                    continue;
                }
                Map<String, Object> record = JsonUtils.toMap(json);
                Object expireAt = record.get("expireAt");
                if (!(expireAt instanceof Number expireMs) || expireMs.longValue() > now) {
                    continue;
                }
                String address = String.valueOf(record.get("address"));
                String loggerName = String.valueOf(record.get("logger"));
                Object originalLevel = record.get("originalLevel");
                actuatorClient.setLoggerLevel(address, loggerName,
                    originalLevel == null ? null : String.valueOf(originalLevel));
                RedisUtil.delete(key);
                log.info("日志级别已自动恢复, addr:{}, logger:{} -> {}", address, loggerName, originalLevel);
            } catch (Exception e) {
                log.warn("日志级别恢复失败, key:{}, error:{}", key, e.getMessage());
            }
        }
    }
}
