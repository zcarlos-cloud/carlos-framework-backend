package com.carlos.encrypt;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * <p>
 * 加密方式
 * </p>
 *
 * @author Carlos
 * @date 2023/3/22 13:44
 */
@Getter
@AllArgsConstructor
public enum EncryptMode {


    /**
     * 密码分组连接模式（Cipher Block Chaining）
     */
    CBC,
    /**
     * 电子密码本模式（Electronic CodeBook）
     */
    ECB
}
