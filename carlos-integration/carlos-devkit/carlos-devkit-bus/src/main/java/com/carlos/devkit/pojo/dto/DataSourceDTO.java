package com.carlos.devkit.pojo.dto;

import com.carlos.devkit.pojo.enums.DbTypeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 受管数据源 传输对象（Service ↔ Manager）
 * </p>
 * <p>
 * {@code password} 仅在「新增 / 测试连接」场景下携带明文；
 * {@code passwordCipher} 仅在后端内部流转，绝不返回给前端。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
public class DataSourceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 数据源名称
     */
    private String dsName;

    /**
     * 数据源编码
     */
    private String dsCode;

    /**
     * 数据库类型
     */
    private DbTypeEnum dbType;

    /**
     * 主机地址
     */
    private String host;

    /**
     * 端口
     */
    private Integer port;

    /**
     * 库名
     */
    private String databaseName;

    /**
     * 默认 schema
     */
    private String schemaName;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码 SM4 密文（后端内部使用）
     */
    private String passwordCipher;

    /**
     * 密码明文（仅入参 / 测试连接使用，不落库、不外发）
     */
    private String password;

    /**
     * 是否已配置密码（列表/详情回填；列表故意不查密文，故需单独判定）
     */
    private Boolean hasPassword;

    /**
     * 额外 JDBC 参数
     */
    private String jdbcParams;

    /**
     * 是否只读
     */
    private Boolean readOnly;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
