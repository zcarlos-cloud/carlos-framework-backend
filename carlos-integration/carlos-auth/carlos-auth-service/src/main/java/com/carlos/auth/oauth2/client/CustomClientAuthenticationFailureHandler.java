package com.carlos.auth.oauth2.client;

import com.carlos.boot.web.util.ResponseWriter;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.core.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * <p>
 * client鉴权失败
 * </p>
 *
 * @author Carlos
 * @date 2022/11/9 10:39
 */
@Slf4j
public class CustomClientAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) {
        log.info("Client authentication failed ：message:{}", exception.getLocalizedMessage());
        ResponseWriter.writeJson(response, Result.error(CommonErrorCode.UNAUTHORIZED, "客户端认证失败！"));
    }


}
