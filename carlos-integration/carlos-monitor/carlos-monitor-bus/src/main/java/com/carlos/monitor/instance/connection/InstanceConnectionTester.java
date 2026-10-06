package com.carlos.monitor.instance.connection;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.exception.BusinessException;
import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.enums.MonitorErrorCode;
import com.carlos.monitor.instance.manager.MonitorInstanceManager;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import com.carlos.monitor.instance.pojo.dto.TestConnectionDTO;
import com.carlos.monitor.instance.pojo.param.TestConnectionParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 实例连接测试
 * Redis：PING + INFO server（redis_version/role），cluster 追加 CLUSTER NODES 解析节点列表；
 * DB：SELECT 1 + 数据库版本查询；返回版本/角色/节点/耗时。
 * 失败抛 {@link MonitorErrorCode#MONITOR_CONNECT_TEST_FAILED}（11303），异常消息已脱敏不含密码
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class InstanceConnectionTester {

    private final MonitorInstanceManager instanceManager;

    private final CredentialCipher credentialCipher;

    private final ManagedConnectionFactory connectionFactory;

    /**
     * 连接测试（入参为未保存的连接信息或 instanceId）
     *
     * @param param 测试参数
     * @return 测试结果
     */
    public TestConnectionDTO test(TestConnectionParam param) {
        MonitorInstanceDTO dto = resolveInstance(param);
        String plainPassword = resolvePassword(param, dto);
        long start = System.currentTimeMillis();
        ManagedConnection managed = null;
        try {
            managed = connectionFactory.create(dto, plainPassword);
            TestConnectionDTO result;
            if (managed instanceof RedisManagedConnection redisManaged) {
                result = testRedis(redisManaged);
            } else if (managed instanceof DbManagedConnection dbManaged) {
                result = testDb(dbManaged);
            } else {
                throw MonitorErrorCode.MONITOR_INSTANCE_TYPE_UNSUPPORTED.exception();
            }
            result.setElapsedMs(System.currentTimeMillis() - start);
            return result;
        } catch (BusinessException e) {
            throw MonitorErrorCode.MONITOR_CONNECT_TEST_FAILED.exception(sanitize(e.getMessage(), plainPassword));
        } catch (Exception e) {
            throw MonitorErrorCode.MONITOR_CONNECT_TEST_FAILED.exception(sanitize(e.getMessage(), plainPassword));
        } finally {
            if (managed != null) {
                managed.close();
            }
        }
    }

    /**
     * 按已保存实例做连接测试
     *
     * @param instanceId 实例ID
     * @return 测试结果
     */
    public TestConnectionDTO testByInstanceId(Long instanceId) {
        TestConnectionParam param = new TestConnectionParam();
        param.setInstanceId(instanceId);
        return test(param);
    }

    private MonitorInstanceDTO resolveInstance(TestConnectionParam param) {
        if (param.getInstanceId() != null) {
            MonitorInstanceDTO dto = instanceManager.getDtoById(param.getInstanceId());
            if (dto == null) {
                throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception();
            }
            return dto;
        }
        if (param.getInstanceType() == null || StrUtil.isBlank(param.getAddresses())) {
            throw MonitorErrorCode.MONITOR_CONNECT_TEST_FAILED.exception("连接测试缺少实例类型或连接地址");
        }
        MonitorInstanceDTO dto = new MonitorInstanceDTO();
        dto.setInstanceType(param.getInstanceType());
        dto.setAddresses(param.getAddresses());
        dto.setDbName(param.getDbName());
        dto.setDbIndex(param.getDbIndex());
        dto.setUsername(param.getUsername());
        dto.setExtConfig(param.getExtConfig());
        dto.setConnectTimeout(param.getConnectTimeout());
        dto.setSocketTimeout(param.getSocketTimeout());
        return dto;
    }

    private String resolvePassword(TestConnectionParam param, MonitorInstanceDTO dto) {
        if (param.getInstanceId() != null) {
            return credentialCipher.decrypt(dto.getPasswordCipher());
        }
        return param.getPassword();
    }

    private TestConnectionDTO testRedis(RedisManagedConnection managed) {
        TestConnectionDTO result = new TestConnectionDTO();
        if (managed.isCluster()) {
            String pong = managed.getClusterConnection().sync().ping();
            String info = managed.getClusterConnection().sync().info("server");
            result.setSuccess("PONG".equalsIgnoreCase(pong));
            result.setVersion(parseInfoValue(info, "redis_version"));
            result.setRole(parseInfoValue(info, "role"));
            result.setNodes(parseClusterNodes(managed.getClusterConnection().sync().clusterNodes()));
        } else {
            String pong = managed.getConnection().sync().ping();
            String info = managed.getConnection().sync().info("server");
            result.setSuccess("PONG".equalsIgnoreCase(pong));
            result.setVersion(parseInfoValue(info, "redis_version"));
            result.setRole(parseInfoValue(info, "role"));
        }
        return result;
    }

    private TestConnectionDTO testDb(DbManagedConnection managed) {
        TestConnectionDTO result = new TestConnectionDTO();
        try (Connection connection = managed.getDataSource().getConnection();
             Statement statement = connection.createStatement();
             ResultSet ignored = statement.executeQuery("SELECT 1")) {
            result.setSuccess(true);
            result.setVersion(connection.getMetaData().getDatabaseProductVersion());
        } catch (Exception e) {
            throw MonitorErrorCode.MONITOR_CONNECT_TEST_FAILED.exception(sanitize(e.getMessage(), null));
        }
        return result;
    }

    /**
     * 解析 INFO 输出中的键值
     */
    private String parseInfoValue(String info, String key) {
        if (StrUtil.isBlank(info)) {
            return null;
        }
        for (String line : info.split("\r?\n")) {
            if (line.startsWith(key + ":")) {
                return line.substring(key.length() + 1).trim();
            }
        }
        return null;
    }

    /**
     * 解析 CLUSTER NODES 输出
     * 行格式：{nodeId} {ip:port@busPort} {flags} {master} {ping} {pong} {epoch} {linkState} [slots...]
     */
    private List<TestConnectionDTO.NodeInfo> parseClusterNodes(String clusterNodes) {
        List<TestConnectionDTO.NodeInfo> nodes = new ArrayList<>();
        if (StrUtil.isBlank(clusterNodes)) {
            return nodes;
        }
        for (String line : clusterNodes.split("\r?\n")) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 8) {
                continue;
            }
            TestConnectionDTO.NodeInfo node = new TestConnectionDTO.NodeInfo();
            String address = parts[1];
            int atIdx = address.indexOf('@');
            node.setAddress(atIdx > 0 ? address.substring(0, atIdx) : address);
            String flags = parts[2];
            node.setRole(flags.contains("master") ? "master" : "slave");
            node.setStatus(parts[7]);
            nodes.add(node);
        }
        return nodes;
    }

    /**
     * 异常消息脱敏：剔除可能出现的明文密码
     */
    private String sanitize(String message, String plainPassword) {
        String msg = StrUtil.nullToEmpty(message);
        if (StrUtil.isNotBlank(plainPassword)) {
            msg = msg.replace(plainPassword, "***");
        }
        // 截断过长的堆栈型消息
        return StrUtil.maxLength(msg, 300);
    }
}
