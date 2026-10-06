package com.carlos.auth.web;

import com.carlos.auth.api.ApiAuthPassword;
import com.carlos.auth.api.pojo.ao.PasswordEncodeAO;
import com.carlos.auth.api.pojo.ao.PasswordMatchAO;
import com.carlos.boot.web.request.WebRequestContext;
import com.carlos.core.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 密码相关接口
 * </p>
 *
 * <p><strong>安全说明：</strong>本接口仅限内部服务调用，禁止暴露给公网。</p>
 *
 * @author carlos
 * @date 2021/12/27 16:05
 */
@Slf4j
@RestController
@Tag(name = "密码接口", description = "密码相关接口（内部服务专用）")
@RequestMapping({"/auth/password", "/api/auth/password"})
@AllArgsConstructor
@ConditionalOnBean(PasswordEncoder.class)
public class ApiAuthPasswordImpl implements ApiAuthPassword {

    private final PasswordEncoder passwordEncoder;

    /**
     * 密码编码
     *
     * <p>前端在传输密码前应通过 SM2 加密，本接口仅用于内部场景生成编码后的密码。</p>
     *
     * @param request 密码编码请求
     * @return 编码后的密码
     */
    @Override
    @Operation(summary = "密码编码", description = "对明文密码进行编码（内部服务专用，POST 方式避免密码进入 URL）")
    @PostMapping(value = "/encode")
    public Result<String> encodePassword(@RequestBody @Validated PasswordEncodeAO request) {
        String password = request.getPassword();
        if (StringUtils.isBlank(password)) {
            return Result.error("密码不能为空");
        }
        String encoded = passwordEncoder.encode(password);
        return Result.success(encoded);
    }

    /**
     * 密码匹配校验
     *
     * <p><strong>警告：</strong>此接口可被用于离线字典攻击，仅限内部服务调用。
     * 外网访问将被拒绝。</p>
     *
     * @param param   匹配参数
     * @return 是否匹配
     */
    @Override
    @Operation(summary = "密码匹配校验", description = "校验明文密码是否与编码密码匹配（仅限内网）")
    @PostMapping(value = "/match")
    public Result<Boolean> match(@RequestBody @Validated PasswordMatchAO param) {
        String clientIp = getClientIp();
        if (!isInternalIp(clientIp)) {
            log.error("Blocked external access to password match API from IP: {}", clientIp);
            return Result.error("该接口仅限内部网络访问");
        }

        log.warn("Password match API called from internal IP: {} — ensure this is legitimate internal traffic", clientIp);
        boolean match = passwordEncoder.matches(param.getPassword(), param.getEncodePassword());
        return Result.success(match);
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp() {
        HttpServletRequest request = WebRequestContext.getCurrentRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 判断是否为内网 IP
     */
    private boolean isInternalIp(String ip) {
        if (StringUtils.isBlank(ip)) {
            return false;
        }
        return ip.startsWith("127.")
            || ip.startsWith("10.")
            || ip.startsWith("192.168.")
            || (ip.startsWith("172.") && is172Private(ip));
    }

    private boolean is172Private(String ip) {
        try {
            String secondOctet = ip.split("\\.")[1];
            int octet = Integer.parseInt(secondOctet);
            return octet >= 16 && octet <= 31;
        } catch (Exception e) {
            return false;
        }
    }


}
