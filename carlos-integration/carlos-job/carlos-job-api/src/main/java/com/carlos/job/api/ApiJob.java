package com.carlos.job.api;

import com.carlos.core.response.Result;
import com.carlos.job.ServiceNameConstant;
import com.carlos.job.api.fallback.ApiJobFallbackFactory;
import com.carlos.job.pojo.ao.JobInfoAO;
import com.carlos.job.pojo.param.ApiJobCreateParam;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * 定时任务管理 Feign 接口（供其他微服务编程式注册/操作任务）
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@FeignClient(
    name = ServiceNameConstant.SERVICE_NAME,
    contextId = "apiJob",
    path = "/api/job",
    fallbackFactory = ApiJobFallbackFactory.class
)
public interface ApiJob {

    /**
     * 创建任务（已存在则失败）
     *
     * @param param 任务参数
     * @return 是否成功
     */
    @PostMapping("/create")
    Result<Boolean> create(@RequestBody @Valid ApiJobCreateParam param);

    /**
     * 手动触发一次
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     * @return 是否成功
     */
    @PostMapping("/runOnce")
    Result<Boolean> runOnce(@RequestParam("jobName") String jobName,
                            @RequestParam(value = "jobGroup", required = false) String jobGroup);

    /**
     * 暂停任务
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     * @return 是否成功
     */
    @PostMapping("/pause")
    Result<Boolean> pause(@RequestParam("jobName") String jobName,
                          @RequestParam(value = "jobGroup", required = false) String jobGroup);

    /**
     * 恢复任务
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     * @return 是否成功
     */
    @PostMapping("/resume")
    Result<Boolean> resume(@RequestParam("jobName") String jobName,
                           @RequestParam(value = "jobGroup", required = false) String jobGroup);

    /**
     * 删除任务
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     * @return 是否成功
     */
    @PostMapping("/delete")
    Result<Boolean> delete(@RequestParam("jobName") String jobName,
                           @RequestParam(value = "jobGroup", required = false) String jobGroup);

    /**
     * 查询任务信息
     *
     * @param jobName  任务名称
     * @param jobGroup 任务分组
     * @return 任务信息
     */
    @GetMapping("/info")
    Result<JobInfoAO> info(@RequestParam("jobName") String jobName,
                           @RequestParam(value = "jobGroup", required = false) String jobGroup);

}
