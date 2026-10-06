package com.carlos.monitor.instance.pojo.vo;

import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.enums.MonitorEnvEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 监控实例 显示层对象（严禁包含 passwordCipher，密码永不回显）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "监控实例视图对象")
public class InstanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键（雪花ID）")
    private Long id;

    @Schema(description = "实例名称")
    private String instanceName;

    @Schema(description = "实例类型（字典 monitor_instance_type）")
    private InstanceTypeEnum instanceType;

    @Schema(description = "环境（字典 monitor_env）：dev/test/prod")
    private MonitorEnvEnum env;

    @Schema(description = "所属分组ID")
    private Long groupId;

    @Schema(description = "分组名称")
    private String groupName;

    @Schema(description = "连接地址，host:port 逗号分隔")
    private String addresses;

    @Schema(description = "数据库名")
    private String dbName;

    @Schema(description = "Redis 逻辑库编号")
    private Integer dbIndex;

    @Schema(description = "连接账号")
    private String username;

    @Schema(description = "是否已配置连接密码（仅返回有无，不返回密文/明文）")
    private Boolean hasPassword;

    @Schema(description = "扩展配置JSON")
    private String extConfig;

    @Schema(description = "采集开关")
    private Boolean collectEnabled;

    @Schema(description = "采集间隔（秒）")
    private Integer collectInterval;

    @Schema(description = "连接超时（毫秒）")
    private Integer connectTimeout;

    @Schema(description = "读写超时（毫秒）")
    private Integer socketTimeout;

    @Schema(description = "数据管理入口开关")
    private Boolean manageEnabled;

    @Schema(description = "SQL 写模式开关")
    private Boolean sqlWriteEnabled;

    @Schema(description = "健康状态：UNKNOWN/UP/DOWN")
    private String healthStatus;

    @Schema(description = "最近成功采集时间")
    private LocalDateTime lastCollectTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "关联应用名列表")
    private List<String> appNames;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    private LocalDateTime updateTime;
}
