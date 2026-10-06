package com.carlos.monitor.instance.service;

import com.carlos.monitor.instance.pojo.dto.InstanceDraftDTO;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import com.carlos.monitor.instance.pojo.dto.TestConnectionDTO;
import com.carlos.monitor.instance.pojo.param.InstanceStatusParam;
import com.carlos.monitor.instance.pojo.param.TestConnectionParam;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 监控实例 业务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
public interface MonitorInstanceService {

    /**
     * 新增实例（保存前强制一次连接测试，失败允许保存但标记 DOWN）
     *
     * @param dto      实例数据
     * @param password 明文密码（服务端 SM4 加密落库）
     * @return 实例ID
     */
    Long addInstance(MonitorInstanceDTO dto, String password);

    /**
     * 修改实例（密码留空表示不修改；连接配置变更时失效连接缓存）
     *
     * @param dto      实例数据
     * @param password 明文密码，可为空
     * @return 是否成功
     */
    boolean updateInstance(MonitorInstanceDTO dto, String password);

    /**
     * 删除实例（逻辑删除 + 连接失效 + 停采 + 清理应用关联）
     *
     * @param id 实例ID
     * @return 是否成功
     */
    boolean deleteInstance(Serializable id);

    /**
     * 实例详情（含关联应用名与分组名）
     *
     * @param id 实例ID
     * @return 实例 DTO
     */
    MonitorInstanceDTO getDetail(Serializable id);

    /**
     * 连接测试（未保存连接信息或 instanceId）
     *
     * @param param 测试参数
     * @return 测试结果
     */
    TestConnectionDTO testConnection(TestConnectionParam param);

    /**
     * 启停采集/数据管理/SQL 写
     *
     * @param id    实例ID
     * @param param 状态参数
     * @return 是否成功
     */
    boolean updateStatus(Serializable id, InstanceStatusParam param);

    /**
     * Nacos 配置自动发现生成实例草稿
     *
     * @param appName 应用名
     * @param group   Nacos 分组
     * @return 草稿列表
     */
    List<InstanceDraftDTO> discover(String appName, String group);

    /**
     * 按应用名查询关联实例（供 Feign health 接口）
     *
     * @param appName 应用名
     * @return 实例 DTO 列表
     */
    List<MonitorInstanceDTO> listByAppName(String appName);
}
