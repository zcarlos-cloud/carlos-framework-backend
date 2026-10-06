package com.carlos.org.pojo.excel;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Description: Excel错误信息对象
 * @Date: 2023/2/21 10:44
 */
@Data
@Accessors(chain = true)
public class ExcelErrorMessage {

    /**
     * 行号
     */
    private Integer row;

    /**
     * 列号
     */
    private Integer column;

    /**
     * 错误信息
     */
    private String message;

    /**
     * 错误值
     */
    private Object value;
}
