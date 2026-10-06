package com.carlos.monitor.db.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.monitor.db.pojo.entity.MonitorDbCapacitySnapshot;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 数据库监控 Mapper 接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
@Mapper
public interface MonitorDbCapacitySnapshotMapper extends BaseMapper<MonitorDbCapacitySnapshot> {

}
