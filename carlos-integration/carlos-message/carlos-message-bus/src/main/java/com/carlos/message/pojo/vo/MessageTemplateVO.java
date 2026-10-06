package com.carlos.message.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 消息模板 显示层对象，向页面传输的对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:06
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageTemplateVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "消息类型ID")
    private Long typeId;
    @Schema(description = "模板编码")
    private String templateCode;
    @Schema(description = "模板名称")
    private String templateName;
    @Schema(description = "标题模板")
    private String titleTemplate;
    @Schema(description = "模板内容")
    private String contentTemplate;
    @Schema(description = "参数定义JSON")
    private String paramSchema;
    @Schema(description = "渠道特殊配置")
    private String channelConfig;
    @Schema(description = "是否启用: 0-禁用 1-启用 2-草稿")
    private Boolean enabled;
    @Schema(description = "创建者编号")
    private Long createBy;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新者编号")
    private Long updateBy;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
