package com.carlos.monitor.instance.service.impl;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.exception.BusinessException;
import com.carlos.monitor.enums.MonitorEnvEnum;
import com.carlos.monitor.enums.MonitorErrorCode;
import com.carlos.monitor.instance.connection.CredentialCipher;
import com.carlos.monitor.instance.connection.InstanceConnectionTester;
import com.carlos.monitor.instance.connection.NacosConfigDiscoverer;
import com.carlos.monitor.instance.manager.MonitorInstanceAppRelManager;
import com.carlos.monitor.instance.manager.MonitorInstanceGroupManager;
import com.carlos.monitor.instance.manager.MonitorInstanceManager;
import com.carlos.monitor.instance.pojo.dto.InstanceDraftDTO;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceGroupDTO;
import com.carlos.monitor.instance.pojo.dto.TestConnectionDTO;
import com.carlos.monitor.instance.pojo.param.InstanceStatusParam;
import com.carlos.monitor.instance.pojo.param.TestConnectionParam;
import com.carlos.monitor.instance.service.MonitorInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 监控实例 业务接口实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorInstanceServiceImpl implements MonitorInstanceService {

    /**
     * 健康状态
     */
    private static final String HEALTH_UP = "UP";

    private static final String HEALTH_DOWN = "DOWN";

    private final MonitorInstanceManager instanceManager;

    private final MonitorInstanceGroupManager groupManager;

    private final MonitorInstanceAppRelManager appRelManager;

    private final InstanceConnectionTester connectionTester;

    private final NacosConfigDiscoverer nacosConfigDiscoverer;

    private final CredentialCipher credentialCipher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addInstance(MonitorInstanceDTO dto, String password) {
        MonitorEnvEnum env = dto.getEnv() == null ? MonitorEnvEnum.PROD : dto.getEnv();
        if (instanceManager.countByNameEnv(dto.getInstanceName(), env.getCode(), null) > 0) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NAME_EXISTS.exception();
        }
        // 保存前强制一次连接测试：失败允许保存但标记 DOWN（设计 4.1）
        String healthStatus = HEALTH_UP;
        try {
            TestConnectionParam testParam = buildTestParam(dto, password);
            connectionTester.test(testParam);
        } catch (BusinessException e) {
            log.warn("新增实例连接测试失败，仍允许保存并标记 DOWN，instanceName:{}, error:{}",
                dto.getInstanceName(), e.getMessage());
            healthStatus = HEALTH_DOWN;
        }
        dto.setEnv(env);
        dto.setHealthStatus(healthStatus);
        dto.setPasswordCipher(credentialCipher.encrypt(password));
        instanceManager.add(dto);
        appRelManager.replaceAppRels(dto.getId(), dto.getAppNames());
        log.info("新增监控实例, id:{}, instanceName:{}, healthStatus:{}", dto.getId(), dto.getInstanceName(), healthStatus);
        return dto.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateInstance(MonitorInstanceDTO dto, String password) {
        MonitorInstanceDTO old = instanceManager.getDtoById(dto.getId());
        if (old == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception();
        }
        MonitorEnvEnum env = dto.getEnv() == null ? old.getEnv() : dto.getEnv();
        if (instanceManager.countByNameEnv(dto.getInstanceName(), env.getCode(), dto.getId()) > 0) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NAME_EXISTS.exception();
        }
        // 密码留空表示不修改，沿用原密文
        if (StrUtil.isBlank(password)) {
            dto.setPasswordCipher(old.getPasswordCipher());
        } else {
            dto.setPasswordCipher(credentialCipher.encrypt(password));
        }
        dto.setEnv(env);
        boolean success = instanceManager.modify(dto);
        if (success && dto.getAppNames() != null) {
            appRelManager.replaceAppRels(dto.getId(), dto.getAppNames());
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteInstance(Serializable id) {
        MonitorInstanceDTO old = instanceManager.getDtoById(id);
        if (old == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception();
        }
        boolean success = instanceManager.delete(id);
        if (success) {
            appRelManager.deleteByInstanceId(id);
        }
        return success;
    }

    @Override
    public MonitorInstanceDTO getDetail(Serializable id) {
        MonitorInstanceDTO dto = instanceManager.getDtoById(id);
        if (dto == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception();
        }
        dto.setAppNames(appRelManager.listAppNames(dto.getId()));
        if (dto.getGroupId() != null) {
            MonitorInstanceGroupDTO group = groupManager.getDtoById(dto.getGroupId());
            if (group != null) {
                dto.setGroupName(group.getGroupName());
            }
        }
        return dto;
    }

    @Override
    public TestConnectionDTO testConnection(TestConnectionParam param) {
        return connectionTester.test(param);
    }

    @Override
    public boolean updateStatus(Serializable id, InstanceStatusParam param) {
        MonitorInstanceDTO old = instanceManager.getDtoById(id);
        if (old == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception();
        }
        return instanceManager.updateStatus(id, param);
    }

    @Override
    public List<InstanceDraftDTO> discover(String appName, String group) {
        return nacosConfigDiscoverer.discover(appName, group);
    }

    @Override
    public List<MonitorInstanceDTO> listByAppName(String appName) {
        List<Long> instanceIds = appRelManager.listInstanceIdsByAppName(appName);
        List<MonitorInstanceDTO> instances = instanceManager.listDtosByIds(instanceIds);
        // 补充分组名
        Map<Long, String> groupNames = new HashMap<>();
        for (MonitorInstanceDTO dto : instances) {
            if (dto.getGroupId() != null && !groupNames.containsKey(dto.getGroupId())) {
                MonitorInstanceGroupDTO group = groupManager.getDtoById(dto.getGroupId());
                groupNames.put(dto.getGroupId(), group == null ? null : group.getGroupName());
            }
        }
        for (MonitorInstanceDTO dto : instances) {
            if (dto.getGroupId() != null) {
                dto.setGroupName(groupNames.get(dto.getGroupId()));
            }
        }
        return instances;
    }

    private TestConnectionParam buildTestParam(MonitorInstanceDTO dto, String password) {
        TestConnectionParam param = new TestConnectionParam();
        param.setInstanceType(dto.getInstanceType());
        param.setAddresses(dto.getAddresses());
        param.setDbName(dto.getDbName());
        param.setDbIndex(dto.getDbIndex());
        param.setUsername(dto.getUsername());
        param.setPassword(password);
        param.setExtConfig(dto.getExtConfig());
        param.setConnectTimeout(dto.getConnectTimeout());
        param.setSocketTimeout(dto.getSocketTimeout());
        return param;
    }
}
