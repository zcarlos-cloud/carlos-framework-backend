package com.carlos.job.controller;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.response.Result;
import com.carlos.job.JobEndpointConstant;
import com.carlos.job.config.JobProperties;
import com.carlos.job.convert.JobRegisterConvert;
import com.carlos.job.pojo.ao.JobRegisterAO;
import com.carlos.job.pojo.dto.JobRegisterDTO;
import com.carlos.job.pojo.param.ApiJobRegisterParam;
import com.carlos.job.security.JobTokenValidator;
import com.carlos.job.service.JobService;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 调度中心内部注册端点：业务服务启动时按其注解声明注册任务
 * </p>
 * <p>
 * 幂等：以 (jobName, jobGroup) 为业务键，已存在则跳过、不覆盖 —— job_info 是唯一真相源。
 * 与 /inner/job/execute 共用 X-Job-Token 鉴权，且不注册网关路由，仅供内网服务间调用。
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Hidden
@Slf4j
@RestController
public class JobRegisterController {

    private final JobService jobService;

    private final JobProperties jobProperties;

    public JobRegisterController(JobService jobService, JobProperties jobProperties) {
        this.jobService = jobService;
        this.jobProperties = jobProperties;
        if (StrUtil.isBlank(jobProperties.getAccessToken())) {
            log.warn("============================================================");
            log.warn("carlos.job.access-token 未配置，/inner/job/register 无鉴权！");
            log.warn("该端点虽不经过网关，但集群内任意进程都能创建任意任务（serviceName 可任选）");
            log.warn("============================================================");
        }
    }

    /**
     * 幂等注册任务
     *
     * @param param 注册参数
     * @param token 内部调用令牌
     * @return 注册结果（未创建不是错误，返回 200 + created=false）
     */
    @Log(
        title = "任务注册",
        logType = "JOB_REGISTER",
        businessType = BusinessType.INSERT,
        operation = "'业务服务注册任务: ' + #param.jobName + ' / 目标服务: ' + #param.serviceName",
        targetType = "JOB_INFO",
        targetId = "#param.jobName",
        riskLevel = 40,
        // 该端点没有登录人，鉴权完全依赖入参 token（X-Job-Token 共享密钥）；
        // 参数序列化会把裸字符串 token 原样写进审计库（LogAspect 的 excludeParams
        // 只按字段名剔除对象字段，匹配不到数组里的裸标量），故整体不落请求参数。
        saveRequestParams = false
    )
    @PostMapping(JobEndpointConstant.REGISTER_PATH)
    public Result<JobRegisterAO> register(@RequestBody @Validated ApiJobRegisterParam param,
                                          @RequestHeader(value = JobEndpointConstant.TOKEN_HEADER, required = false)
                                          String token) {
        if (!JobTokenValidator.valid(jobProperties.getAccessToken(), token)) {
            log.warn("任务注册鉴权失败: jobName={}", param.getJobName());
            return Result.error("任务注册鉴权失败");
        }
        JobRegisterDTO dto = jobService.registerIfAbsent(param);
        return Result.success(JobRegisterConvert.INSTANCE.toAO(dto));
    }

}
