package com.carlos.integration.module.dingtalk.config;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.Date;

/**
 * <p>
 * 閽夐拤鎺ュ彛鍒濆嬪?
 * </p>
 *
 * @author Carlos
 * @date 2023/4/6 10:18
 */
@Slf4j
@RequiredArgsConstructor
public class DingtalkInitWorker implements ApplicationRunner {

    private final DingtalkAccessTokenManager tokenManager;


    private final String corn;


    @Override
    public void run(ApplicationArguments args) {
        // 鍒濆嬪寲accesstoken, 骞惰Е鍙戝畾鏃跺櫒鍒锋柊
        // 鍒ゆ柇鏄鍚﹀凡缁忓垵濮嬪寲浠诲?
        tokenManager.refreshAccessToken();

        String jobId = CronUtil.schedule("DINGTALK-ACCESSTOKEN-TASK", corn, new Task() {
            @Override
            public void execute() {
                try {
                    log.info("Dingtalk refresh accessToken, date:{}", DateUtil.format(new Date(), DatePattern.NORM_DATETIME_MS_PATTERN));
                    tokenManager.refreshAccessToken();
                } catch (Exception e) {
                    log.error("Dingtalk refresh accessToken fail, date:{}", DateUtil.format(new Date(), DatePattern.NORM_DATETIME_MS_PATTERN), e);
                }
            }
        });

        log.info("Job '{}' has been registered", jobId);
    }


}
