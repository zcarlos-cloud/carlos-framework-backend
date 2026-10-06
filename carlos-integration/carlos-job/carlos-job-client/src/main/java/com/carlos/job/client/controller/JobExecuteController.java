package com.carlos.job.client.controller;

import com.carlos.core.response.Result;
import com.carlos.job.JobEndpointConstant;
import com.carlos.job.client.config.JobClientProperties;
import com.carlos.job.client.handler.JobHandler;
import com.carlos.job.client.handler.JobHandlerRegistry;
import com.carlos.job.client.handler.JobResult;
import com.carlos.job.pojo.ao.JobResultAO;
import com.carlos.job.pojo.param.ApiJobExecuteParam;
import com.carlos.job.security.JobTokenValidator;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 调度中心内部回调端点（不注册到网关路由，仅供内网服务间调用）
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Hidden
@Slf4j
@RestController
@RequiredArgsConstructor
// 本类由 JobClientAutoConfiguration 以 @Bean 注册（那里已按同一开关把关），并非靠扫描生效。
// 但 @RestController 是 @Component 的元注解，宿主扫描根一旦覆盖 com.carlos.job.client.controller，
// 扫描会再注册一份<b>完全不过条件</b>的实例，带来两个后果：
//   1. 任务客户端关闭时仍被注册 → 构造失败、服务起不来；
//   2. 与自动配置那份同名 Bean 冲突（overriding 默认 false）→ 同样起不来。
// 补上条件后，关闭时扫描跳过本类；开启时由自动配置的 @ConditionalOnMissingBean 让路。
@ConditionalOnProperty(prefix = JobClientProperties.PREFIX, name = "enabled",
    havingValue = "true", matchIfMissing = true)
public class JobExecuteController {

    /**
     * 调度中心回调鉴权请求头
     *
     * @deprecated 改用 {@link JobEndpointConstant#TOKEN_HEADER}，保留仅为源码兼容
     */
    @Deprecated
    public static final String TOKEN_HEADER = JobEndpointConstant.TOKEN_HEADER;

    private final JobHandlerRegistry handlerRegistry;

    private final JobClientProperties clientProperties;

    /**
     * 执行指定任务处理器
     *
     * @param param 执行参数
     * @return 执行结果（执行失败是业务结果，返回 200 + success=false）
     */
    @PostMapping(JobEndpointConstant.EXECUTE_PATH)
    public Result<JobResultAO> execute(@RequestBody @Validated ApiJobExecuteParam param,
                                       @RequestHeader(value = JobEndpointConstant.TOKEN_HEADER, required = false)
                                       String token) {
        if (!JobTokenValidator.valid(clientProperties.getAccessToken(), token)) {
            log.warn("任务回调鉴权失败: handler={}", param.getHandlerName());
            return Result.error("任务回调鉴权失败");
        }
        JobHandler handler = handlerRegistry.getHandler(param.getHandlerName());
        if (handler == null) {
            // 不回显已注册处理器清单，避免泄露内部结构
            log.warn("任务处理器不存在: handler={}", param.getHandlerName());
            return Result.success(JobResultAO.fail("任务处理器不存在"));
        }
        try {
            JobResult result = handler.execute(param.getParams());
            return Result.success(new JobResultAO()
                .setSuccess(result.getSuccess())
                .setMessage(result.getMessage()));
        } catch (Exception e) {
            log.error("任务执行异常: handler={}", param.getHandlerName(), e);
            return Result.success(JobResultAO.fail("任务执行异常: " + e.getMessage()));
        }
    }

}
