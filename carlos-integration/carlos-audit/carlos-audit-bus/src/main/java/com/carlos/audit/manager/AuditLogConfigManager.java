package com.carlos.audit.manager;

import com.carlos.audit.pojo.dto.AuditLogConfigDTO;
import com.carlos.audit.pojo.entity.AuditLogConfig;
import com.carlos.audit.pojo.param.AuditLogConfigPageParam;
import com.carlos.audit.pojo.vo.AuditLogConfigVO;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;

import java.io.Serializable;

/**
 * <p>
 * 审计日志配置（动态TTL与采样策略） 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
public interface AuditLogConfigManager extends BaseService<AuditLogConfig> {

    /**
     * 新增审计日志配置（动态TTL与采样策略）
     *
     * @param dto 审计日志配置（动态TTL与采样策略）数据
     * @return boolean
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    boolean add(AuditLogConfigDTO dto);

    /**
     * 删除审计日志配置（动态TTL与采样策略）
     *
     * @param id 审计日志配置（动态TTL与采样策略）id
     * @return boolean
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    boolean delete(Serializable id);

    /**
     * 修改审计日志配置（动态TTL与采样策略）信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    boolean modify(AuditLogConfigDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.audit.pojo.dto.AuditLogConfigDTO
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    AuditLogConfigDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param  param 分页参数
     * @author Carlos
     * @date 2026年3月6日 下午9:31:12
     */
    Paging<AuditLogConfigVO> getPage(AuditLogConfigPageParam param);

    /**
     * 获取生效的审计日志配置
     *
     * @param logType  日志类型
     * @param tenantId 租户ID
     * @return 生效配置，不存在时返回系统默认配置
     */
    AuditLogConfigDTO getEffectiveConfig(String logType, String tenantId);
}
