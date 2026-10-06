package com.carlos.util;

import com.carlos.encrypt.EncryptUtil;
import org.junit.jupiter.api.Test;

class EncryptUtilTest {

    @Test
    void sm2Test() {
        final String content = "123456";
        String encStr = EncryptUtil.sm2Encrypt(content);
        System.out.println(encStr);
        System.out.println(EncryptUtil.sm2Decrypt(encStr));
    }

}
