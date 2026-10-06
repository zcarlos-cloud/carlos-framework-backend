package com.carlos.system.sequence.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.system.sequence.pojo.dto.SequenceBatch;
import com.carlos.system.sequence.pojo.dto.SysSequenceRuleDTO;
import com.carlos.system.sequence.pojo.entity.SysSequenceRule;
import com.carlos.system.sequence.pojo.param.SysSequenceRulePageParam;
import com.carlos.system.sequence.pojo.vo.SysSequenceRuleVO;

import java.io.Serializable;

/**
 * <p>
 * 业务编码/序列号规则 Manager
 * </p>
 *
 * <p>计数自增（Redis 原子自增 + 数据库 CAS 兜底）属于数据层原子操作，统一收敛在本层。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
public interface SysSequenceRuleManager extends BaseService<SysSequenceRule> {

    /**
     * 新增规则
     *
     * @param dto 规则数据
     * @return 是否成功
     */
    boolean add(SysSequenceRuleDTO dto);

    /**
     * 逻辑删除规则并清理其 Redis 计数键
     *
     * @param id 规则ID
     * @return 是否成功
     */
    boolean delete(Serializable id);

    /**
     * 更新规则（规则编码不参与更新）
     *
     * @param dto 规则数据
     * @return 是否成功
     */
    boolean modify(SysSequenceRuleDTO dto);

    /**
     * 按ID查询规则
     *
     * @param id 规则ID
     * @return 规则数据，不存在返回 null
     */
    SysSequenceRuleDTO getDtoById(Long id);

    /**
     * 按规则编码查询规则
     *
     * @param ruleCode 规则编码
     * @return 规则数据，不存在返回 null
     */
    SysSequenceRuleDTO getByCode(String ruleCode);

    /**
     * 分页查询规则
     *
     * @param param 查询参数
     * @return 分页结果
     */
    Paging<SysSequenceRuleVO> getPage(SysSequenceRulePageParam param);

    /**
     * 判断规则编码是否已存在
     *
     * <p>规则编码创建后不可修改（更新参数里根本没有该字段），故无需排除自身。</p>
     *
     * @param ruleCode 规则编码
     * @return 是否已存在
     */
    boolean existsByCode(String ruleCode);

    /**
     * 清理规则的所有 Redis 计数键（删规则时调用）
     *
     * <p>⚠️ 只有「删规则」用它：删完规则后没有生成入口，清理是安全的。改规则配置时**不能**清键 ——
     * 清键与配置生效之间必然存在窗口，窗口内用旧规则生成的请求会给计数键播种，
     * 而清键若落在它之后，下一个请求就会从 1 重新发号、与已发出的编码重号（详见
     * {@code SysSequenceRuleService#updateRule}）。</p>
     *
     * @param ruleCode 规则编码
     */
    void clearCounters(String ruleCode);

    /**
     * 取下一个号段
     *
     * <p>优先 Redis 原子自增（单个 Lua 脚本内完成「按数据库水位播种 + 溢出预检 + 自增 + 首次设过期」）；
     * Redis 不可用时降级数据库 CAS 自增。</p>
     *
     * @param rule  规则数据
     * @param count 本段编码数量（1 ~ {@link com.carlos.system.api.ApiSequence#MAX_BATCH_SIZE}）
     * @return 号段（含流水区间与日期段）
     */
    SequenceBatch next(SysSequenceRuleDTO rule, int count);

}
