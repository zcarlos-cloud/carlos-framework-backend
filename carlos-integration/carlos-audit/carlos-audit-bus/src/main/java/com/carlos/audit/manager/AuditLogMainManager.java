package com.carlos.audit.manager;

import com.carlos.audit.pojo.dto.AuditLogMainDTO;
import com.carlos.audit.pojo.entity.AuditLogMain;
import com.carlos.audit.pojo.param.AuditLogMainPageParam;
import com.carlos.audit.pojo.vo.AuditLogMainVO;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 审计日志宽主表（合并数据变更、技术上下文、标签、附件，保留7天热数据） 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
public interface AuditLogMainManager extends BaseService<AuditLogMain> {

    /**
     * 新增审计日志宽主表（合并数据变更、技术上下文、标签、附件，保留7天热数据）
     *
     * @param dto 审计日志宽主表（合并数据变更、技术上下文、标签、附件，保留7天热数据）数据
     * @return boolean
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    boolean add(AuditLogMainDTO dto);

    /**
     * 删除审计日志宽主表（合并数据变更、技术上下文、标签、附件，保留7天热数据）
     *
     * @param id 审计日志宽主表（合并数据变更、技术上下文、标签、附件，保留7天热数据）id
     * @return boolean
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    boolean delete(Serializable id);

    /**
     * 修改审计日志宽主表（合并数据变更、技术上下文、标签、附件，保留7天热数据）信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    boolean modify(AuditLogMainDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.audit.pojo.dto.AuditLogMainDTO
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogMainDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param  param 分页参数
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    Paging<AuditLogMainVO> getPage(AuditLogMainPageParam param);

    /**
     * 查询用户行为轨迹
     *
     * @param principalId 主体ID
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @return 行为轨迹列表
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    List<AuditLogMainDTO> getUserTrail(String principalId, LocalDateTime startTime, LocalDateTime endTime);
}
