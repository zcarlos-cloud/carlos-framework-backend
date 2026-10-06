package com.carlos.license.verify;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 *   验证内容
 * </p>
 *
 * @author Carlos
 * @date 2025-04-10 13:46 
 */
@AllArgsConstructor
@Getter
public enum ValidateContent {
    /**
     * ip地址
     */
    IP,

    /**
     * mac地址
     */
    MAC,

    /**
     * cpu序列号
     */
    CPU,

    /**
     * 主板序列号
     */
    MAIN_BOARD;
}
