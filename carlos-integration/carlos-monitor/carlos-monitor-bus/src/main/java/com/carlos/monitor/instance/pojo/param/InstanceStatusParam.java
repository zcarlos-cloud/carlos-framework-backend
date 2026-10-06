package com.carlos.monitor.instance.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 监控实例 启停状态修改参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Data
@Accessors(chain = true)
@Schema(description = "监控实例启停状态修改参数")
public class InstanceStatusParam {

    @Schema(description = "采集开关")
    private Boolean collectEnabled;

    @Schema(description = "数据管理入口开关")
    private Boolean manageEnabled;

    @Schema(description = "SQL 写模式开关")
    private Boolean sqlWriteEnabled;
}
