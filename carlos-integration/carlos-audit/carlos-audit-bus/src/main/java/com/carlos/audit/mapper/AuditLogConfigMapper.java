package com.carlos.audit.mapper;

import com.carlos.audit.pojo.entity.AuditLogConfig;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;


/**
 * <p>
 * 审计日志配置（动态TTL与采样策略） 查询接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Mapper
public interface AuditLogConfigMapper extends MPJBaseMapper<AuditLogConfig> {


}
