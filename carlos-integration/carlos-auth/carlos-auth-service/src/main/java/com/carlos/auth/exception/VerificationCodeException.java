package com.carlos.auth.exception;

import org.springframework.security.authentication.AccountStatusException;

/**
 * 验证码exception<p>
 *
 * @author hemx
 * @since 2023/9/21
 */
public class VerificationCodeException extends AccountStatusException {
    public VerificationCodeException(String msg) {
        super(msg);
    }

    public VerificationCodeException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
