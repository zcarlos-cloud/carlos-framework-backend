package com.carlos.system.sequence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.system.sequence.pojo.entity.SysSequenceRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 业务编码/序列号规则 Mapper
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Mapper
public interface SysSequenceRuleMapper extends BaseMapper<SysSequenceRule> {

}
