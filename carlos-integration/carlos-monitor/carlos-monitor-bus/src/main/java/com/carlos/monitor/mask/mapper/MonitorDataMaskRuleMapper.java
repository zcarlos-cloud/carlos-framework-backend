package com.carlos.monitor.mask.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.monitor.mask.pojo.entity.MonitorDataMaskRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 数据管理脱敏规则 Mapper 接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Mapper
public interface MonitorDataMaskRuleMapper extends BaseMapper<MonitorDataMaskRule> {

}
