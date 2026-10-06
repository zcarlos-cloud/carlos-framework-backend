package com.carlos.test.controller;

import com.carlos.encrypt.EncryptUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("encrypt")
@Tag(name = "加解密工具测")
@Slf4j
public class EncryptController {

    @GetMapping("sm2Encrypt")
    @Operation(summary = "sm2加密")
    public String encrypt(String content) {
        String encStr = EncryptUtil.sm2EncryptHex(content);
        System.out.println(encStr);
        System.out.println(EncryptUtil.sm2Decrypt(encStr));
        return encStr;
    }

    @GetMapping("sm2DEncrypt")
    @Operation(summary = "sm2解密")
    public String decrypt(String content) {
        return EncryptUtil.sm2Decrypt(content);
    }


}
