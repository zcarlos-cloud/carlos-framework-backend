package com.carlos.monitor.instance.pojo.param;

import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.enums.MonitorEnvEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 监控实例 新增参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Data
@Accessors(chain = true)
@Schema(description = "监控实例新增参数")
public class InstanceCreateParam {

    @NotBlank(message = "实例名称不能为空")
    @Size(max = 100, message = "实例名称长度不能超过100")
    @Schema(description = "实例名称")
    private String instanceName;

    @NotNull(message = "实例类型不能为空")
    @Schema(description = "实例类型（字典 monitor_instance_type）")
    private InstanceTypeEnum instanceType;

    @NotNull(message = "环境不能为空")
    @Schema(description = "环境（字典 monitor_env）：dev/test/prod")
    private MonitorEnvEnum env;

    @Schema(description = "所属分组ID")
    private Long groupId;

    @NotBlank(message = "连接地址不能为空")
    @Size(max = 1000, message = "连接地址长度不能超过1000")
    @Schema(description = "连接地址，host:port 逗号分隔（cluster/sentinel 多节点）")
    private String addresses;

    @Size(max = 100, message = "数据库名长度不能超过100")
    @Schema(description = "数据库名（DB类型必填）")
    private String dbName;

    @Schema(description = "Redis 逻辑库编号（standalone 用）")
    private Integer dbIndex;

    @Size(max = 100, message = "连接账号长度不能超过100")
    @Schema(description = "连接账号")
    private String username;

    @Size(max = 200, message = "连接密码长度不能超过200")
    @Schema(description = "连接密码（明文，服务端 SM4 加密后落库，永不回显）")
    private String password;

    @Size(max = 2000, message = "扩展配置长度不能超过2000")
    @Schema(description = "扩展配置JSON：ssl/sentinel masterName/拓扑刷新等")
    private String extConfig;

    @Schema(description = "采集开关，默认 true")
    private Boolean collectEnabled = true;

    @Min(value = 10, message = "采集间隔不能小于10秒")
    @Max(value = 600, message = "采集间隔不能大于600秒")
    @Schema(description = "采集间隔（秒，10~600），默认 30")
    private Integer collectInterval = 30;

    @Schema(description = "连接超时（毫秒），默认 3000")
    private Integer connectTimeout = 3000;

    @Schema(description = "读写超时（毫秒），默认 5000")
    private Integer socketTimeout = 5000;

    @Schema(description = "数据管理入口开关，默认 true")
    private Boolean manageEnabled = true;

    @Schema(description = "SQL 写模式开关，默认 false")
    private Boolean sqlWriteEnabled = false;

    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;

    @Schema(description = "关联应用名列表（微服务 spring.application.name）")
    private List<String> appNames;
}
