package com.carlos.system.dict.config;

import cn.hutool.core.thread.ThreadUtil;
import com.carlos.system.dict.manager.SysDictCacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 启动时进行字典初始化
 * </p>
 *
 * @author carlos
 * @date 2021/11/26 16:45
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SysDictInitRunnerWorker implements ApplicationRunner, Ordered {

    private final SysDictCacheManager cacheManager;


    @Override
    public void run(ApplicationArguments args) {
        ThreadUtil.execute(() -> {
            // 初始化字典信息
            cacheManager.initCache();
        });

    }

    @Override
    public int getOrder() {
        return 2;
    }
}
