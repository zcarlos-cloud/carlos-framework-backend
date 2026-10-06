package com.carlos.devkit.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.carlos.devkit.pojo.enums.DbTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 开发工具-受管数据源 数据源对象
 * </p>
 * <p>
 * {@code passwordCipher} 为 SM4 密文，绝不出现在任何 VO / 日志中。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@TableName("dev_datasource")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DevDataSource implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花ID）
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 数据源名称
     */
    @TableField("ds_name")
    private String dsName;

    /**
     * 数据源编码
     */
    @TableField("ds_code")
    private String dsCode;

    /**
     * 数据库类型
     */
    @TableField("db_type")
    private DbTypeEnum dbType;

    /**
     * 主机地址
     */
    @TableField("host")
    private String host;

    /**
     * 端口
     */
    @TableField("port")
    private Integer port;

    /**
     * 库名
     */
    @TableField("database_name")
    private String databaseName;

    /**
     * 默认 schema（PostgreSQL 专用）
     */
    @TableField("schema_name")
    private String schemaName;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 密码 SM4 密文
     */
    @TableField("password_cipher")
    private String passwordCipher;

    /**
     * 额外 JDBC 参数
     */
    @TableField("jdbc_params")
    private String jdbcParams;

    /**
     * 是否只读数据源
     */
    @TableField("read_only")
    private Boolean readOnly;

    /**
     * 状态：1=启用 0=停用
     */
    @TableField("status")
    private Integer status;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

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

    /**
     * 逻辑删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
