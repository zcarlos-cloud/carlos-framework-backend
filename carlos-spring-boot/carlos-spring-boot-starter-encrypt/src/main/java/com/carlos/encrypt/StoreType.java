package com.carlos.encrypt;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * <p>
 * 密文存储方式
 * </p>
 *
 * @author Carlos
 * @date 2023/3/22 13:46
 */
@Getter
@AllArgsConstructor
public enum StoreType {

    /**
     * 十六进制字符串
     */
    HEX,
    /**
     * Base64
     */
    BASE64
}
