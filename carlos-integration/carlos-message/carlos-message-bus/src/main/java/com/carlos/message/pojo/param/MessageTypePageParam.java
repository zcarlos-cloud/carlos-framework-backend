package com.carlos.message.pojo.param;


import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


/**
 * <p>
 * 消息类型 列表查询参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "消息类型列表查询参数")
public class MessageTypePageParam extends ParamPage {
    @Size(max = 32, message = "类型编码长度不能超过 32")
    @Schema(description = "类型编码")
    private String typeCode;
    @Size(max = 32, message = "类型名称长度不能超过 32")
    @Schema(description = "类型名称")
    private String typeName;
    @Schema(description = "是否启用: 0-禁用 1-启用")
    private Boolean enabled;
    @Schema(description = "开始时间")
    private LocalDateTime start;

    @Schema(description = "结束时间")
    private LocalDateTime end;
}
