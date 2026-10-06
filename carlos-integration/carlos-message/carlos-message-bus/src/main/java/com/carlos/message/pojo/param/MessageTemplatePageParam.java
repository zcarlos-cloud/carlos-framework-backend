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
 * 消息模板 列表查询参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "消息模板列表查询参数")
public class MessageTemplatePageParam extends ParamPage {
    @Schema(description = "消息类型ID")
    private Long typeId;
    @Size(max = 50, message = "模板编码长度不能超过 50")
    @Schema(description = "模板编码")
    private String templateCode;
    @Size(max = 50, message = "模板名称长度不能超过 50")
    @Schema(description = "模板名称")
    private String templateName;
    @Size(max = 100, message = "标题模板长度不能超过 100")
    @Schema(description = "标题模板")
    private String titleTemplate;
    @Size(max = 2000, message = "模板内容长度不能超过 2000")
    @Schema(description = "模板内容")
    private String contentTemplate;
    @Size(max = 2000, message = "参数定义JSON长度不能超过 2000")
    @Schema(description = "参数定义JSON")
    private String paramSchema;
    @Size(max = 2000, message = "渠道特殊配置长度不能超过 2000")
    @Schema(description = "渠道特殊配置")
    private String channelConfig;
    @Schema(description = "是否启用: 0-禁用 1-启用 2-草稿")
    private Boolean enabled;
    @Schema(description = "开始时间")
    private LocalDateTime start;

    @Schema(description = "结束时间")
    private LocalDateTime end;
}
