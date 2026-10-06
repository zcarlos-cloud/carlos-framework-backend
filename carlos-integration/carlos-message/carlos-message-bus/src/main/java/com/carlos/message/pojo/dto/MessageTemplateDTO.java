package com.carlos.message.pojo.dto;


import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 消息模板 数据传输对象，service和manager向外传输对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Data
@Accessors(chain = true)
public class MessageTemplateDTO {
    /** 主键ID */
    private Long id;
    /** 消息类型ID */
    private Long typeId;
    /** 消息类型编码 */
    private String typeCode;
    /** 模板编码 */
    private String templateCode;
    /** 模板名称 */
    private String templateName;
    /** 标题模板 */
    private String titleTemplate;
    /** 模板内容 */
    private String contentTemplate;
    /** 参数定义JSON */
    private String paramSchema;
    /** 渠道特殊配置 */
    private String channelConfig;
    /** 是否启用: 0-禁用 1-启用 2-草稿 */
    private Boolean enabled;
    /** 创建者编号 */
    private Long createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新者编号 */
    private Long updateBy;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
