package com.carlos.auth.api;

import com.carlos.auth.api.fallback.FeignAuthPasswordFallbackFactory;
import com.carlos.auth.api.pojo.ao.PasswordEncodeAO;
import com.carlos.auth.api.pojo.ao.PasswordMatchAO;
import com.carlos.core.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 统一认证中心 Feign 接口
 *
 * @author carlos
 * @date 2021/11/11 13:51
 */
@FeignClient(
    value = ServiceNameConstant.SERVICE_NAME,
    contextId = "authPassword",
    path = "/api/auth/password",
    fallbackFactory = FeignAuthPasswordFallbackFactory.class)
public interface ApiAuthPassword {

    /**
     * 密码编码
     *
     * @param request 密码编码请求
     * @return 编码后的密码
     * @author carlos
     * @date 2021/12/27 16:07
     */
    @PostMapping(value = "encode")
    Result<String> encodePassword(@RequestBody PasswordEncodeAO request);

    /**
     * 密码匹配校验
     *
     * @param param 匹配参数
     * @return 是否匹配
     * @author carlos
     * @date 2022/4/25 15:30
     */
    @PostMapping(value = "match")
    Result<Boolean> match(@RequestBody PasswordMatchAO param);
}
