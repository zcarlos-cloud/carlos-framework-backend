package com.carlos.monitor.instance.pojo.vo;

import com.carlos.monitor.instance.pojo.dto.TestConnectionDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 连接测试结果 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "连接测试结果视图对象")
public class TestConnectionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否连接成功")
    private Boolean success;

    @Schema(description = "服务端版本")
    private String version;

    @Schema(description = "角色（master/slave）")
    private String role;

    @Schema(description = "连接耗时（毫秒）")
    private Long elapsedMs;

    @Schema(description = "节点明细（cluster/sentinel 模式）")
    private List<TestConnectionDTO.NodeInfo> nodes;

    @Schema(description = "失败原因（已脱敏）")
    private String errorMsg;
}
