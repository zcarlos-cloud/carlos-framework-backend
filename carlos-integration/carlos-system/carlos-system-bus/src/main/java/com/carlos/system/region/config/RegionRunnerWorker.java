package com.carlos.system.region.config;

import com.carlos.system.region.manager.SysRegionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 区域缓存初始化
 * </p>
 *
 * @author carlos
 * @date 2021/11/26 16:45
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RegionRunnerWorker implements ApplicationRunner, Ordered {

    private final SysRegionManager regionManager;


    @Override
    public void run(ApplicationArguments args) {
        // ThreadUtil.execute((() -> {
        //     this.regionManager.initCache();
        // }));
    }

    @Override
    public int getOrder() {
        return 3;
    }
}
