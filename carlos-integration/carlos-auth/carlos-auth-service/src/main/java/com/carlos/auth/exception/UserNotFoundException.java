package com.carlos.auth.exception;

import org.springframework.security.authentication.AccountStatusException;

/**
 * 代码描述<p>
 *
 * @author hemx
 * @since 2023/9/21
 */
public class UserNotFoundException extends AccountStatusException {
    public UserNotFoundException(String msg) {
        super(msg);
    }

    public UserNotFoundException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
