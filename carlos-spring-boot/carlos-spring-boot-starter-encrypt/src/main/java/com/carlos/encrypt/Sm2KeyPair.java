package com.carlos.encrypt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>
 *  sm2 密钥对
 * </p>
 *
 * @author Carlos
 * @date 2025-09-07 22:52
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Sm2KeyPair {
    public String publicKey;  // 130 位 hex，04 开头
    public String privateKey; // 64  位 hex


}