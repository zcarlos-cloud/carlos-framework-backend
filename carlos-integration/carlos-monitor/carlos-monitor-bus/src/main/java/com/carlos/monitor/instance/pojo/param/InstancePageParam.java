package com.carlos.monitor.instance.pojo.param;

import com.carlos.core.param.ParamPage;
import com.carlos.monitor.enums.InstanceTypeEnum;
import com.carlos.monitor.enums.MonitorEnvEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 监控实例 分页查询参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "监控实例分页查询参数")
public class InstancePageParam extends ParamPage {

    @Size(max = 100, message = "实例名称长度不能超过100")
    @Schema(description = "实例名称（模糊）")
    private String instanceName;

    @Schema(description = "实例类型（字典 monitor_instance_type）")
    private InstanceTypeEnum instanceType;

    @Schema(description = "环境（字典 monitor_env）")
    private MonitorEnvEnum env;

    @Schema(description = "所属分组ID")
    private Long groupId;

    @Schema(description = "健康状态：UNKNOWN/UP/DOWN")
    private String healthStatus;

    @Size(max = 100, message = "关键字长度不能超过100")
    @Schema(description = "关键字（匹配实例名称/连接地址）")
    private String keyword;
}
