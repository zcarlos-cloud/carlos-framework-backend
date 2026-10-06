package com.carlos.system.configration.pojo.excel;


import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 系统配置 导入导出数据对象
 * </p>
 *
 * @author carlos
 */
@Data
public class SysConfigExcel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 参数名称
     */
    @ExcelProperty(value = "参数名称")
    private String configName;

    /**
     * 参数编码
     */
    @ExcelProperty(value = "参数编码")
    private String configCode;

    /**
     * 参数键值
     */
    @ExcelProperty(value = "参数值")
    private String configValue;

    /**
     * 值类型(文本/文件)
     */
    @ExcelProperty(value = "值类型")
    private String valueType;

    /**
     * 状态(启用/禁用)
     */
    @ExcelProperty(value = "启用状态")
    private String configStatus;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 错误信息（导入校验用）
     */
    @ExcelProperty(value = "错误信息")
    private String error;

}
