package com.carlos.org.config;

import com.carlos.org.manager.OrgDepartmentManager;
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
public class CacheInitRunnerWorker implements ApplicationRunner, Ordered {

    private final OrgDepartmentManager departmentManager;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting cache preheat...");
        try {
            departmentManager.initCache();
            log.info("Department cache preheat completed successfully");
        } catch (Exception e) {
            log.error("Department cache preheat failed", e);
        }
    }

    @Override
    public int getOrder() {
        return 3;
    }
}
