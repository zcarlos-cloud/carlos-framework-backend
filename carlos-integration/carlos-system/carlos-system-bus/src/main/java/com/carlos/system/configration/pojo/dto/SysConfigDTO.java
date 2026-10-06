package com.carlos.system.configration.pojo.dto;


import com.carlos.system.enums.ConfigValueType;
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
public class SysConfigDTO {

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
     * 参数键值
     */
    private String configValue;
    /**
     * 值类型
     */
    private ConfigValueType valueType;
    /**
     * 状态
     */
    private Boolean configStatus;
    /**
     * 显示顺序
     */
    private Long orderNum;
    /**
     * 备注
     */
    private String remark;
    /**
     * 创建者
     */
    private Long createBy;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新者
     */
    private Long updateBy;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
