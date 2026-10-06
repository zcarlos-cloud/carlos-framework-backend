package com.carlos.org.pojo.excel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 用户导入导出 Excel 对象
 * </p>
 *
 * @author Carlos
 * @date 2026年5月19日
 */
@Data
public class OrgUserExcel implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "账号")
    private String account;

    @ExcelProperty(value = "昵称")
    private String nickname;

    @ExcelProperty(value = "手机号")
    private String phone;

    @ExcelProperty(value = "性别")
    private String gender;

    @ExcelProperty(value = "邮箱")
    private String email;

    @ExcelProperty(value = "部门")
    private String department;

    @ExcelProperty(value = "角色")
    private String role;

    @ExcelProperty(value = "错误信息")
    private String error;

}
