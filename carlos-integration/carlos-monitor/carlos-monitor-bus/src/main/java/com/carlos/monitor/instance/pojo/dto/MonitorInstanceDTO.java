package com.carlos.monitor.instance.pojo.dto;

import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.enums.MonitorEnvEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 监控实例 数据传输对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Data
@Accessors(chain = true)
public class MonitorInstanceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long id;

    /** 实例名称 */
    private String instanceName;

    /** 实例类型 */
    private InstanceTypeEnum instanceType;

    /** 环境：dev/test/prod */
    private MonitorEnvEnum env;

    /** 所属分组ID */
    private Long groupId;

    /** 分组名称（冗余展示） */
    private String groupName;

    /** 连接地址，host:port 逗号分隔 */
    private String addresses;

    /** 数据库名（DB类型必填） */
    private String dbName;

    /** Redis 逻辑库编号 */
    private Integer dbIndex;

    /** 连接账号 */
    private String username;

    /** 连接密码（SM4 密文，仅服务内部使用，禁止出 VO） */
    private String passwordCipher;

    /** 扩展配置JSON */
    private String extConfig;

    /** 采集开关 */
    private Boolean collectEnabled;

    /** 采集间隔（秒） */
    private Integer collectInterval;

    /** 连接超时（毫秒） */
    private Integer connectTimeout;

    /** 读写超时（毫秒） */
    private Integer socketTimeout;

    /** 数据管理入口开关 */
    private Boolean manageEnabled;

    /** SQL 写模式开关 */
    private Boolean sqlWriteEnabled;

    /** 健康状态：UNKNOWN/UP/DOWN */
    private String healthStatus;

    /** 最近成功采集时间 */
    private LocalDateTime lastCollectTime;

    /** 备注 */
    private String remark;

    /** 关联应用名列表（monitor_instance_app_rel） */
    private List<String> appNames;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;
}
