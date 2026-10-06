package com.carlos.gateway.config;

import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import com.carlos.core.spring.BootUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.context.config.annotation.RefreshScope;
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
@RefreshScope
public class GatewayRunnerWorker implements ApplicationRunner, Ordered {

    public static final String DOC = "/doc.html";

    private final GatewayProperties gatewayProperties;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Knife4j: {}", getKnife4jUrl());
    }

    @Override
    public int getOrder() {
        return 1;
    }

    /**
     * 获取Knife4j地址
     *
     * @author carlos
     * @date 2020/7/20 17:05
     */
    public String getKnife4jUrl() {
        return BootUtil.getAddress() + stripApiPrefix() + DOC;
    }

    private String stripApiPrefix() {
        String prefix = gatewayProperties.getPrefix();
        if (StrUtil.startWith(prefix, StrPool.SLASH)) {
            return prefix.substring(1);
        }

        return prefix;
    }
}
