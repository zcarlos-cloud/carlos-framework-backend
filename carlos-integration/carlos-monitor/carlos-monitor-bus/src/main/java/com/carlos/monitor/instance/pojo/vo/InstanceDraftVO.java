package com.carlos.monitor.instance.pojo.vo;

import com.carlos.monitor.enums.InstanceTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 实例草稿（Nacos 自动发现结果） 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "实例草稿视图对象")
public class InstanceDraftVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "实例名称（建议值）")
    private String instanceName;

    @Schema(description = "实例类型")
    private InstanceTypeEnum instanceType;

    @Schema(description = "连接地址，host:port 逗号分隔")
    private String addresses;

    @Schema(description = "数据库名")
    private String dbName;

    @Schema(description = "Redis 逻辑库编号")
    private Integer dbIndex;

    @Schema(description = "连接账号")
    private String username;

    @Schema(description = "来源配置（Nacos dataId 与配置路径）")
    private String sourceConfig;
}
