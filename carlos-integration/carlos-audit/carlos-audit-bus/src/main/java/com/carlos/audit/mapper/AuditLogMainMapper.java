package com.carlos.audit.mapper;

import com.carlos.audit.pojo.entity.AuditLogMain;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;


/**
 * <p>
 * 审计日志宽主表（合并数据变更、技术上下文、标签、附件，保留7天热数据） 查询接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Mapper
public interface AuditLogMainMapper extends MPJBaseMapper<AuditLogMain> {


}
