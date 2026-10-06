package com.carlos.monitor.instance.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.enums.MonitorEnvEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 监控实例（中间件/数据库注册） 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Data
@Accessors(chain = true)
@TableName("monitor_instance")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonitorInstance implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花ID）
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 实例名称
     */
    @TableField("instance_name")
    private String instanceName;

    /**
     * 实例类型（字典 monitor_instance_type）
     */
    @TableField("instance_type")
    private InstanceTypeEnum instanceType;

    /**
     * 环境（字典 monitor_env）：dev/test/prod
     */
    @TableField("env")
    private MonitorEnvEnum env;

    /**
     * 所属分组ID
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 连接地址，host:port 逗号分隔（cluster/sentinel 多节点）
     */
    @TableField("addresses")
    private String addresses;

    /**
     * 数据库名（DB类型必填）
     */
    @TableField("db_name")
    private String dbName;

    /**
     * Redis 逻辑库编号（standalone 用，cluster 忽略）
     */
    @TableField("db_index")
    private Integer dbIndex;

    /**
     * 连接账号
     */
    @TableField("username")
    private String username;

    /**
     * 连接密码（SM4 密文，接口永不回显）
     */
    @TableField("password_cipher")
    private String passwordCipher;

    /**
     * 扩展配置JSON：ssl/sentinel masterName/拓扑刷新等
     */
    @TableField("ext_config")
    private String extConfig;

    /**
     * 采集开关
     */
    @TableField("collect_enabled")
    private Boolean collectEnabled;

    /**
     * 采集间隔（秒，10~600）
     */
    @TableField("collect_interval")
    private Integer collectInterval;

    /**
     * 连接超时（毫秒）
     */
    @TableField("connect_timeout")
    private Integer connectTimeout;

    /**
     * 读写超时（毫秒）
     */
    @TableField("socket_timeout")
    private Integer socketTimeout;

    /**
     * 数据管理入口开关（关闭则仅监控）
     */
    @TableField("manage_enabled")
    private Boolean manageEnabled;

    /**
     * SQL 写模式开关（默认关闭，prod 实例需管理员显式开启）
     */
    @TableField("sql_write_enabled")
    private Boolean sqlWriteEnabled;

    /**
     * 健康状态冗余：UNKNOWN/UP/DOWN（由采集结果回写）
     */
    @TableField("health_status")
    private String healthStatus;

    /**
     * 最近成功采集时间
     */
    @TableField("last_collect_time")
    private LocalDateTime lastCollectTime;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 修改人
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private Long updateBy;

    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
