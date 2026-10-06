package com.carlos.job.handler;

import com.carlos.job.client.handler.JobHandler;
import com.carlos.job.client.handler.JobResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * <p>
 * 联调测试用的任务处理器：打印一条日志并返回成功，处理器名称即 Bean 名 demoJobHandler
 * </p>
 * <p>
 * 调度中心自身的任务由 {@code JobDispatcher} 的本机分支执行 —— 任务的 serviceName 留空或
 * 等于本机应用名（carlos-job）时不再走 HTTP 回环。业务模块接入时应在各自服务中实现
 * {@link JobHandler}，并引入 carlos-job-client 暴露 /inner/job/execute 走远程派发。
 * </p>
 * <p>
 * ⚠️ 仅为联调存在，不承载任何业务语义，生产环境应删除或替换为真实处理器。
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Slf4j
@Component
public class DemoJobHandler implements JobHandler {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public JobResult execute(String params) {
        String now = LocalDateTime.now().format(TIME_FORMATTER);
        log.info("测试任务执行: time={}, params={}", now, params);
        return JobResult.ok("执行成功: " + now);
    }

}
