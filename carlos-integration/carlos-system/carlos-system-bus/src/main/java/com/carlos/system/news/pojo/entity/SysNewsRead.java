package com.carlos.system.news.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 公告已读记录 数据源对象（用户维度，物理删除）
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
@TableName("sys_news_read")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysNewsRead implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 主键（雪花 ID）
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 公告ID
     */
    @TableField("news_id")
    private Long newsId;
    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;
    /**
     * 阅读时间（Service 显式赋值；框架注解通用填充同样能填，此处显式赋值保持语义直白）
     */
    @TableField("read_time")
    private LocalDateTime readTime;
    /**
     * 租户ID（预留）
     */
    @TableField("tenant_id")
    private Long tenantId;

}
