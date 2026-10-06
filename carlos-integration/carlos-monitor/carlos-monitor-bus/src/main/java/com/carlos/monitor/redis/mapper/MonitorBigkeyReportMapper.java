package com.carlos.monitor.redis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.monitor.redis.pojo.entity.MonitorBigkeyReport;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Redis 大key/热key 报告 Mapper 接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Mapper
public interface MonitorBigkeyReportMapper extends BaseMapper<MonitorBigkeyReport> {

}
