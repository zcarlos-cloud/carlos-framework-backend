package com.carlos.monitor.instance.pojo.dto;

import com.carlos.monitor.enums.InstanceTypeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 实例草稿（Nacos 配置自动发现结果，不含密码） 数据传输对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Data
@Accessors(chain = true)
public class InstanceDraftDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 实例名称（建议值，可改） */
    private String instanceName;

    /** 实例类型 */
    private InstanceTypeEnum instanceType;

    /** 连接地址，host:port 逗号分隔 */
    private String addresses;

    /** 数据库名（DB 类型） */
    private String dbName;

    /** Redis 逻辑库编号 */
    private Integer dbIndex;

    /** 连接账号 */
    private String username;

    /** 来源配置（生成草稿的 Nacos dataId 与配置路径） */
    private String sourceConfig;
}
