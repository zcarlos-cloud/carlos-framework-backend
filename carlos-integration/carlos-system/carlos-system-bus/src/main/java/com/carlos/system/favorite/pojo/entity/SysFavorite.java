package com.carlos.system.favorite.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
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
 * 通用用户收藏 数据源对象（用户维度，物理删除）
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
@TableName("sys_favorite")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysFavorite implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 主键（雪花 ID）
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 用户ID（数据按用户隔离）
     */
    @TableField("user_id")
    private Long userId;
    /**
     * 业务类型：小写字母/数字/中划线，如 quick-nav、sys-news
     */
    @TableField("target_type")
    private String targetType;
    /**
     * 目标标识：路由 key、公告 id 等业务侧字符串
     */
    @TableField("target_id")
    private String targetId;
    /**
     * 同 user_id+target_type 内排序（replace 按数组顺序 1..n 写入；add 单条添加时为 0）
     */
    @TableField("sort_no")
    private Integer sortNo;
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
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 修改人
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private Long updateBy;
    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

}
