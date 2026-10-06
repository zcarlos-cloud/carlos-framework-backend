package com.carlos.monitor.instance.manager;

import com.carlos.datasource.base.BaseService;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import com.carlos.monitor.instance.pojo.entity.MonitorInstance;
import com.carlos.monitor.instance.pojo.param.InstancePageParam;
import com.carlos.monitor.instance.pojo.param.InstanceStatusParam;
import com.carlos.monitor.instance.pojo.vo.InstanceVO;
import com.carlos.core.pagination.Paging;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 监控实例 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
public interface MonitorInstanceManager extends BaseService<MonitorInstance> {

    /**
     * 新增实例
     *
     * @param dto 实例数据
     * @return 是否成功
     */
    boolean add(MonitorInstanceDTO dto);

    /**
     * 修改实例（连接配置变更时发布连接失效事件）
     *
     * @param dto 实例数据
     * @return 是否成功
     */
    boolean modify(MonitorInstanceDTO dto);

    /**
     * 逻辑删除实例（发布连接失效事件，停采由采集域监听）
     *
     * @param id 实例ID
     * @return 是否成功
     */
    boolean delete(Serializable id);

    /**
     * 实例详情（Caffeine + Redis 二级缓存）
     *
     * @param id 实例ID
     * @return 实例 DTO，不存在返回 null
     */
    MonitorInstanceDTO getDtoById(Serializable id);

    /**
     * 分页查询（Caffeine + Redis 二级缓存，写操作按版本号失效）
     *
     * @param param 分页参数
     * @return 分页结果
     */
    Paging<InstanceVO> getPage(InstancePageParam param);

    /**
     * 启停状态修改（采集/数据管理/SQL 写开关）
     *
     * @param id    实例ID
     * @param param 状态参数
     * @return 是否成功
     */
    boolean updateStatus(Serializable id, InstanceStatusParam param);

    /**
     * 按 ID 集合查询实例
     *
     * @param ids 实例ID集合
     * @return 实例 DTO 列表
     */
    List<MonitorInstanceDTO> listDtosByIds(Collection<? extends Serializable> ids);

    /**
     * 按名称+环境查询实例（自动登记 upsert 用）
     *
     * @param instanceName 实例名称
     * @param env          环境码（dev/test/prod）
     * @return 实例 DTO，不存在返回 null
     */
    MonitorInstanceDTO getByNameEnv(String instanceName, String env);

    /**
     * 统计实例名称在当前环境下的数量（唯一性校验）
     *
     * @param instanceName 实例名称
     * @param env          环境
     * @param excludeId    排除的实例ID（编辑场景）
     * @return 数量
     */
    long countByNameEnv(String instanceName, String env, Serializable excludeId);

    /**
     * 查询开启采集的实例列表（调度扫描用，只取采集所需字段）
     *
     * @return 实例 DTO 列表
     */
    List<MonitorInstanceDTO> listCollectEnabled();

    /**
     * 查询全部实例（大盘统计用，含健康状态，不缓存）
     *
     * @return 实例 DTO 列表
     */
    List<MonitorInstanceDTO> listAllInstances();

    /**
     * 健康状态回写（Caffeine 合并节流：同实例同状态 10s 内只写一次库；
     * 状态翻转立即写）。lastCollectTime 仅成功采集时传入
     *
     * @param id              实例ID
     * @param healthStatus    健康状态：UP/DOWN
     * @param lastCollectTime 最近成功采集时间（失败回写传 null，不更新该字段）
     */
    void updateHealth(Serializable id, String healthStatus, LocalDateTime lastCollectTime);
}
