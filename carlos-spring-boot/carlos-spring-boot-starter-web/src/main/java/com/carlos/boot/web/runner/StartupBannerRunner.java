package com.carlos.boot.web.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

/**
 * <p>
 * 应用启动 Banner 打印器。
 * </p>
 * <p>
 * 在应用启动后打印 "Carlos Framework" ASCII Art Banner，
 * 并输出激活的 Profile 与 Context-Path 信息。
 * </p>
 *
 * @author carlos
 * @date 2020-2-18 10:11:05
 */
@Slf4j
@RequiredArgsConstructor
public class StartupBannerRunner implements ApplicationRunner, Ordered {

    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        printBanner();
        printAppInfo();
    }

    @Override
    public int getOrder() {
        return 1;
    }

    /**
     * 打印 ASCII Art Banner。
     */
    private void printBanner() {
        String banner = """

                 _____ __             __     _____
                / ___// /_____ ______/ /_   / ___/__  _______________  __________
                \\__ \\/ __/ __ `/ ___/ __/   \\__ \\/ / / / ___/ ___/ _ \\/ ___/ ___/
              _____/ / /_/ /_/ /  / /     _____/ / /_/ /__/ /__/  __(__  |__  )
             /____/\\__/\\__,_/_/   \\__/   /____/\\__,_/\\___/\\___/\\___/____/____/
            """;
        log.info(banner);
    }

    /**
     * 打印应用信息：Profile 和 Context-Path。
     */
    private void printAppInfo() {
        String profileActive = environment.getProperty("spring.profiles.active");
        if (profileActive != null && !profileActive.isBlank()) {
            log.info("Profile-Active: {}", profileActive);
        }
        String contextPath = environment.getProperty("server.servlet.context-path");
        if (contextPath != null && !contextPath.isBlank()) {
            log.info("Content-Path: {}", contextPath);
        }
    }

}
