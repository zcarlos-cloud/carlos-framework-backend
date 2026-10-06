package com.carlos.audit.mapper;

import com.carlos.audit.pojo.entity.AuditLogArchiveRecord;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;


/**
 * <p>
 * 审计日志归档记录（管理冷数据归档） 查询接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Mapper
public interface AuditLogArchiveRecordMapper extends MPJBaseMapper<AuditLogArchiveRecord> {


}
