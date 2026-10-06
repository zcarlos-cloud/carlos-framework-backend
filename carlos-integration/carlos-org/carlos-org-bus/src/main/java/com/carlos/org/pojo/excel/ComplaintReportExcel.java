package com.carlos.org.pojo.excel;


import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 投诉反馈 导入导出数据对象
 * </p>
 *
 * @author carlos
 * @date 2022-11-11 18:19:17
 */
@Data
public class ComplaintReportExcel implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "投诉来源")
    private String complaintSourceName;

    @ExcelProperty(value = "任务来源")
    private String taskSourceName;

    @ExcelProperty(value = "投诉类型")
    private String complaintTypeName;

    @ExcelProperty(value = "投诉表单")
    private String complaintFormName;

    @ExcelProperty(value = "制表部门")
    private String formDept;

    @ExcelProperty(value = "投诉内容")
    private String reason;

    @ExcelProperty(value = "投诉时间")
    private String complaintTime;

    @ExcelProperty(value = "投诉状态")
    private String statusName;

    @ExcelProperty(value = "投诉反馈")
    private String reply;

}
