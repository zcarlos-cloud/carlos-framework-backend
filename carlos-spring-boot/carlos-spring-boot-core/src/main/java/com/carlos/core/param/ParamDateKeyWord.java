package com.carlos.core.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <p>
 * 接口时间参数封装
 * </p>
 *
 * @author carlos
 * @date 2020/4/14 17:31
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ParamDateKeyWord extends ParamKeyWord {

    @Schema(description = "时间起点")
    private LocalDateTime startDate;

    @Schema(description = "时间终点")
    private LocalDateTime endDate;
}
