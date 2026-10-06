package com.carlos.org.position.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.org.position.pojo.dto.OrgUserPositionDTO;
import com.carlos.org.position.pojo.entity.OrgUserPosition;
import com.carlos.org.position.pojo.param.OrgUserPositionPageParam;
import com.carlos.org.position.pojo.vo.OrgUserPositionVO;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 用户岗位职级关联表（核心任职信息） 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
public interface OrgUserPositionManager extends BaseService<OrgUserPosition> {

    /**
     * 新增用户岗位职级关联表（核心任职信息）
     *
     * @param dto 用户岗位职级关联表（核心任职信息）数据
     * @return boolean
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    boolean add(OrgUserPositionDTO dto);

    /**
     * 删除用户岗位职级关联表（核心任职信息）
     *
     * @param id 用户岗位职级关联表（核心任职信息）id
     * @return boolean
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    boolean delete(Serializable id);

    /**
     * 修改用户岗位职级关联表（核心任职信息）信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    boolean modify(OrgUserPositionDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.org.position.pojo.dto.OrgUserPositionDTO
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    OrgUserPositionDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param  param 分页参数
     * @author Carlos
     * @date 2026年3月3日 下午11:19:10
     */
    Paging<OrgUserPositionVO> getPage(OrgUserPositionPageParam param);

    /**
     * 根据用户ID删除岗位关联
     *
     * @param userId 用户ID
     * @return boolean
     */
    boolean removeByUserId(Long userId);

    /**
     * 根据用户ID查询岗位关联列表
     *
     * @param userId 用户ID
     * @return List<OrgUserPositionDTO>
     */
    List<OrgUserPositionDTO> listByUserId(Long userId);

    /**
     * 查询用户主岗位列表
     *
     * @param userId 用户ID
     * @return List<OrgUserPositionDTO>
     */
    List<OrgUserPositionDTO> listMainByUserId(Long userId);

    /**
     * 根据用户ID和岗位ID查询关联
     *
     * @param userId     用户ID
     * @param positionId 岗位ID
     * @return OrgUserPositionDTO
     */
    OrgUserPositionDTO getByUserIdAndPositionId(Long userId, Long positionId);

    /**
     * 根据用户ID、岗位ID、部门ID查询关联
     *
     * @param userId       用户ID
     * @param positionId   岗位ID
     * @param departmentId 部门ID
     * @return OrgUserPositionDTO
     */
    OrgUserPositionDTO getByUserIdAndPositionIdAndDeptId(Long userId, Long positionId, Long departmentId);

    /**
     * 清除用户的主岗位标记
     *
     * @param userId 用户ID
     * @return boolean
     */
    boolean clearMainPosition(Long userId);

    /**
     * 更新岗位状态为已卸任
     *
     * @param userId       用户ID
     * @param positionId   岗位ID
     * @param status       状态
     * @param dimissionDate 卸任日期
     * @return boolean
     */
    boolean updatePositionStatus(Long userId, Long positionId, Integer status, LocalDate dimissionDate);

    /**
     * 统计指定岗位的在职人数
     *
     * @param positionId 岗位ID
     * @return 在职人数
     */
    int countOccupiedByPositionId(Serializable positionId);

    /**
     * 根据汇报对象（直接上级）查询任职记录列表
     *
     * <p>以 {@code org_user_position.report_to} 为查询条件，返回以该用户为直接上级的任职记录。
     * 支撑「审批人 = 直属上级」「超时升级上级」等场景。</p>
     *
     * <p>不做任职状态过滤，同一个人可能存在多条任职记录（兼岗），
     * 调用方如需唯一人员集合请自行按 user_id 去重。</p>
     *
     * @param reportTo 汇报对象（直接上级）用户ID
     * @return List<OrgUserPositionDTO>
     */
    List<OrgUserPositionDTO> listByReportTo(Serializable reportTo);

    /**
     * 根据汇报对象（直接上级）和租户查询任职记录列表
     *
     * <p>语义与 {@link #listByReportTo} 完全一致（不做任职状态过滤，同一个人可能有多条兼岗记录），
     * 但绕过租户拦截器后显式按 {@code tenant_id} 过滤，
     * 供**无 {@code HttpServletRequest} 上下文**的调用方使用——典型场景是 carlos-job
     * 调度线程执行「超时升级上级」时，租户无法经 {@code FeignRequestInterceptor} 透传，
     * 会静默回落到平台租户，从而升级给错误租户的上级。</p>
     *
     * @param reportTo 汇报对象（直接上级）用户ID
     * @param tenantId 租户ID
     * @return List&lt;OrgUserPositionDTO&gt;，任一参数为空时返回空列表
     */
    List<OrgUserPositionDTO> listByReportToAndTenant(Serializable reportTo, Long tenantId);

    /**
     * 查询用户自己的汇报对象（直接上级）用户ID
     *
     * <p>读的是<b>该用户自己</b>任职记录上的 {@code org_user_position.report_to}，
     * 与 {@link #listByReportTo}（按 {@code report_to} 查<b>下级</b>）方向相反。</p>
     *
     * <p>用户可能存在多条任职记录（兼岗），取数规则：优先主岗位（{@code is_main = true}）
     * 记录的 {@code report_to}；主岗位未配置 {@code report_to} 或用户没有主岗位记录时，
     * 回退为任意一条已配置 {@code report_to} 的记录。全部为空时返回 {@code null}
     * ——「汇报线上没有上级」是正常数据状态，不视为错误。</p>
     *
     * <p>不做任职状态过滤，口径与 {@link #listByReportTo} 一致。</p>
     *
     * @param userId 用户ID
     * @return 汇报对象（直接上级）用户ID；无任职记录或未配置时返回 null
     */
    Long getReportToByUserId(Serializable userId);

    /**
     * 查询指定租户内用户自己的汇报对象（直接上级）用户ID
     *
     * <p>取数规则与 {@link #getReportToByUserId} 完全一致（优先主岗位、其次任意一条已配置记录、
     * 不做任职状态过滤），但绕过租户拦截器后显式按 {@code tenant_id} 过滤，
     * 供**无 {@code HttpServletRequest} 上下文**的调用方使用——典型场景是 carlos-job
     * 调度线程执行「超时升级上级」时，租户无法经 {@code FeignRequestInterceptor} 透传，
     * 会静默回落到平台租户，从而解析出另一个租户的上级。</p>
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return 汇报对象用户ID；任一参数为空、无任职记录或未配置时返回 null
     */
    Long getReportToByUserIdAndTenant(Serializable userId, Long tenantId);

    /**
     * 查询任职某岗位的用户ID列表（去重）
     *
     * <p>以 {@code org_user_position.position_id} 为查询条件。一个人在同一岗位上可能有多条
     * 任职记录（跨部门兼岗），结果按 {@code user_id} 去重。供 Feign 远程按岗位解析用户列表
     * 使用：先取 id 集合，再由调用方批量查询用户，避免逐条查询（N+1）。</p>
     *
     * <p><b>刻意不按 {@code position_status} 过滤</b>，与 {@link #listByReportTo} 口径一致：
     * 已卸任（{@code position_status = 5}）的记录若未清理也会返回。理由是
     * {@code position_status = 2}（试用期）属于正常在职状态，按「仅在职（1）」过滤会漏人；
     * 是否排除已卸任由调用方按业务判断。注意这与
     * {@link #countOccupiedByPositionId}（岗位编制占用人数，只数在职）口径不同，两者数值本就不应一致。</p>
     *
     * @param positionId 岗位ID
     * @return 用户ID列表；入参为空时返回空列表
     */
    List<Long> listUserIdsByPositionId(Serializable positionId);

    /**
     * 查询指定租户内任职某岗位的用户ID列表（去重）
     *
     * <p>口径与 {@link #listUserIdsByPositionId} 完全一致（不做任职状态过滤、按 {@code user_id} 去重），
     * 但绕过租户拦截器后显式按 {@code tenant_id} 过滤，
     * 供**无 {@code HttpServletRequest} 上下文**的调用方（后台任务、系统间调用）使用。</p>
     *
     * @param positionId 岗位ID
     * @param tenantId   租户ID
     * @return 用户ID列表；任一参数为空时返回空列表
     */
    List<Long> listUserIdsByPositionIdAndTenant(Serializable positionId, Long tenantId);
}
