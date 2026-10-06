package com.carlos.openapi;

import com.carlos.core.spring.BootUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;

/**
 * <p>
 * 应用启动成功需要做的工作
 * </p>
 *
 * @author carlos
 * @date 2020-2-18 10:11:05
 */
@Slf4j
@RequiredArgsConstructor
public class Knife4jRunnerWorker implements ApplicationRunner, Ordered {


    @Override
    public void run(ApplicationArguments args) {
        log.info("Knife4j: {}", getKnife4jUrl());
    }

    @Override
    public int getOrder() {
        return 1000;
    }

    /**
     * 获取Knife4j地址
     *
     * @author carlos
     * @date 2020/7/20 17:05
     */
    public static String getKnife4jUrl() {
        return BootUtil.getAddress() + OpenApiConstant.HTML_KNIFE4J;
    }
}
