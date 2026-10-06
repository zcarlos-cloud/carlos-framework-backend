package com.carlos.auth.api;

import com.carlos.auth.api.fallback.FeignAuthFallbackFactory;
import com.carlos.auth.api.pojo.ao.CheckTokenAO;
import com.carlos.core.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 统一认证中心 Feign 接口
 *
 * @author carlos
 * @date 2021/11/11 13:51
 */
@FeignClient(
    value = ServiceNameConstant.SERVICE_NAME,
    contextId = "auth",
    fallbackFactory = FeignAuthFallbackFactory.class)
public interface ApiAuth {

    /**
     * 获取认证 Token（代理 OAuth2 标准端点）
     *
     * @param param         认证信息（form-urlencoded）
     * @param authorization Basic Auth 客户端认证头（可选）
     * @return 认证中心返回的原始 Token Map
     * @author carlos
     * @date 2021/12/27 16:08
     */
    @PostMapping(value = "/oauth2/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    Map<String, Object> getAccessToken(
        @RequestBody MultiValueMap<String, String> param,
        @RequestHeader(value = "Authorization", required = false) String authorization);


    /**
     * 检查 Token 有效性
     *
     * @param token 访问令牌
     * @return Token 检查结果
     * @author Carlos
     * @date 2022/11/13 17:11
     */
    @GetMapping("/auth/token/checkToken")
    Result<CheckTokenAO> checkToken(@RequestParam("token") String token);
}
