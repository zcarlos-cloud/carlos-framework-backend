package com.carlos.auth.mfa.pojo.param;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MFA 设置请求参数
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MfaSetupParam {

    /**
     * 用户名（可选，默认使用当前登录用户）
     */
    private String username;

    /**
     * MFA 类型（预留：TOTP/SMS/EMAIL）
     */
    private String mfaType = "TOTP";
}
