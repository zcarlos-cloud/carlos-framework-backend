package com.carlos.job.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 目标服务实例 显示对象（来自 Nacos 服务发现）
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Data
@Accessors(chain = true)
@Schema(description = "目标服务实例")
public class ServiceInstanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "服务名")
    private String serviceName;

    @Schema(description = "实例ID")
    private String instanceId;

    @Schema(description = "主机")
    private String host;

    @Schema(description = "端口")
    private Integer port;

    @Schema(description = "是否健康")
    private Boolean healthy;

    @Schema(description = "实例地址(host:port)")
    private String address;

}
