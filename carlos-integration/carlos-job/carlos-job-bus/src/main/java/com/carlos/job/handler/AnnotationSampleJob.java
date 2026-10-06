package com.carlos.job.handler;

import com.carlos.job.client.annotation.AutoRegisterJob;
import com.carlos.job.client.annotation.ScheduleJob;
import com.carlos.job.client.handler.JobResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 注解式任务处理器示例（对标 xxl-job 的 {@code @XxlJob}），与接口式的 {@link DemoJobHandler} 并存
 * </p>
 * <p>
 * 与接口式的差别：不必实现 {@code JobHandler}、处理器名由注解声明（这里显式写
 * {@code annotationSampleHandler}，不写则取方法名）、任务定义也能随代码走（{@link AutoRegisterJob}）。
 * </p>
 * <p>
 * 注意本类位于<b>调度中心自身</b>，注解里的 serviceName 留空即回退本机应用名 carlos-job，
 * 派发走 {@code JobDispatcher} 的本机分支、不产生 HTTP 回环。业务服务侧的写法完全一致，
 * 只是 serviceName 会回退成那个业务服务的应用名，派发走 {@code /inner/job/execute}。
 * </p>
 * <p>
 * ⚠️ 仅为联调存在，不承载任何业务语义，生产环境应删除或替换为真实任务。
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Slf4j
@Component
public class AnnotationSampleJob {

    /**
     * 示例任务：每天 3 点执行，返回结果即为任务执行结果（void 方法则正常返回即成功）
     *
     * @param params 任务参数（任务配置中的 JSON 字符串，未配置时为 null）
     * @return 执行结果
     */
    @ScheduleJob("annotationSampleHandler")
    @AutoRegisterJob(cron = "0 0 3 * * ?",
        description = "注解式任务示例（每天 3 点）",
        params = "{\"source\":\"annotation\"}",
        author = "carlos")
    public JobResult sample(String params) {
        log.info("注解任务执行: params={}", params);
        return JobResult.ok("注解任务执行成功");
    }

}
