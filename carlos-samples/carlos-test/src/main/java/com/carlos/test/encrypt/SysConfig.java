package com.carlos.test.encrypt;


import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 系统配置 数据传输对象，service和manager向外传输对象
 * </p>
 *
 * @author carlos
 * @date 2022-11-3 13:47:54
 */
@Data
@Accessors(chain = true)
public class SysConfig {

    /**
     * 参数Id
     */
    private Long id;
    /**
     * 参数名称
     */
    private String configName;
    /**
     * 参数编码
     */
    private String configCode;
    /**
     * 参数键
     */
    private String configValue;
    /**
     * 值类
     */
    private String valueType;
    /**
     * 状
     */
    private Boolean state;
    /**
     * 显示顺序
     */
    private Long sort;
    /**
     * 备注
     */
    private String remark;
    /**
     * 创建
     */
    private Long createBy;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新
     */
    private Long updateBy;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
