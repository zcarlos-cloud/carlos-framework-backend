package com.carlos.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.job.pojo.entity.QrtzSchedulerState;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Quartz 集群节点状态 Mapper（只读，禁止写入 QRTZ_SCHEDULER_STATE）
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Mapper
public interface QrtzSchedulerStateMapper extends BaseMapper<QrtzSchedulerState> {

}
