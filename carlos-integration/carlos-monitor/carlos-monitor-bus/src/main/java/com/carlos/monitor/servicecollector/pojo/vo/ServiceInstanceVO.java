package com.carlos.monitor.servicecollector.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Map;

/**
 * <p>
 * 微服务实例明细 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "微服务实例明细视图对象")
public class ServiceInstanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "IP")
    private String ip;

    @Schema(description = "端口")
    private Integer port;

    @Schema(description = "Nacos 健康标记")
    private Boolean healthy;

    @Schema(description = "权重")
    private Double weight;

    @Schema(description = "元数据")
    private Map<String, String> metadata;

    @Schema(description = "actuator health（UP/DOWN/UNREACHABLE）")
    private String actuatorHealth;
}
