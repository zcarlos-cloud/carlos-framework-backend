package com.carlos.monitor.instance.controller;

import com.carlos.core.response.Result;
import com.carlos.monitor.config.MonitorProperties;
import com.carlos.monitor.instance.pojo.param.ConfirmTokenIssueParam;
import com.carlos.monitor.redis.pojo.vo.ConfirmTokenVO;
import com.carlos.monitor.redis.proxy.RedisWriteGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 通用高危操作确认令牌签发 rest服务接口
 * </p>
 * <p>
 * <b>为什么不挂特定权限点（仅要求已认证）：</b>
 * 签发行为本身无任何危害——令牌只是一串随机串，真正的高危操作（DB 终止会话、
 * SQL 写执行、Redis 批量删除、服务日志级别调整）在各自消费端仍有双重校验：
 * ① 消费接口的 @PreAuthorize 高危权限点（monitor:db:session:kill / monitor:db:sql:write /
 * monitor:redis:data:batch-delete / monitor:service:logger）；
 * ② 令牌与 instanceId+operation 摘要绑定，60s 一次性、消费即删。
 * 即：无高危权限的用户即使签出令牌，也无法通过消费端权限校验；
 * 有权限的用户签发自己的令牌不构成越权。若给签发端点错挂单一域权限
 * （如 monitor:redis:data:edit），反而会把其他域的合法用户挡在门外。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月7日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/monitor/confirm-tokens")
@Tag(name = "高危操作确认令牌")
public class ConfirmTokenController {

    private final RedisWriteGuard writeGuard;

    private final MonitorProperties monitorProperties;

    @PostMapping
    @Operation(summary = "签发高危操作确认令牌",
        description = "入参实例ID+操作摘要，60s 一次性，绑定实例与操作防篡改；"
            + "仅要求已认证（安全论证见类注释），消费端仍按各自高危权限点校验")
    public Result<ConfirmTokenVO> issue(@RequestBody @Validated ConfirmTokenIssueParam param) {
        String token = writeGuard.issueConfirmToken(param.getInstanceId(), param.getOperation());
        ConfirmTokenVO vo = new ConfirmTokenVO();
        vo.setToken(token);
        vo.setTtlSeconds(monitorProperties.getRedis().getConfirmTokenTtlSec());
        return Result.success(vo);
    }
}
