package com.carlos.system.news.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.carlos.system.news.pojo.enums.SysNewsScopeEnum;
import com.carlos.system.news.pojo.enums.SysNewsStatusEnum;
import com.carlos.system.news.pojo.enums.SysNewsTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 公告/站内通知 数据源对象
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
@TableName("sys_news")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysNews implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 主键（雪花 ID）
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 标题
     */
    @TableField("title")
    private String title;
    /**
     * 类型：1-公告 2-通知 3-更新日志
     */
    @TableField("news_type")
    private SysNewsTypeEnum newsType;
    /**
     * 内容
     */
    @TableField("content")
    private String content;
    /**
     * 发布范围：1-全员 2-指定角色
     */
    @TableField("publish_scope")
    private SysNewsScopeEnum publishScope;
    /**
     * 指定角色的角色ID JSON 数组（publish_scope=2 时生效）
     */
    @TableField("scope_role_ids")
    private String scopeRoleIds;
    /**
     * 是否置顶
     */
    @TableField("is_top")
    private Boolean isTop;
    /**
     * 状态：1-草稿 2-已发布 3-已下线
     */
    @TableField("status")
    private SysNewsStatusEnum status;
    /**
     * 发布时间
     */
    @TableField("publish_time")
    private LocalDateTime publishTime;
    /**
     * 过期时间（NULL 表示永不过期）
     */
    @TableField("expire_time")
    private LocalDateTime expireTime;
    /**
     * 租户ID（预留，system 模块暂无租户拦截器）
     */
    @TableField("tenant_id")
    private Long tenantId;
    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;
    /**
     * 修改人
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private Long updateBy;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
    /**
     * 逻辑删除
     */
    @TableLogic
    @TableField(value = "is_deleted")
    private Boolean deleted;

}
