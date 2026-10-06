package com.carlos.sample.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <p>
 * 短信服务示例应用启动类
 * </p>
 *
 * @author Carlos
 * @date 2026/3/15 22:50
 */
@Slf4j
@SpringBootApplication
public class SmsSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmsSampleApplication.class, args);
        log.info("=== Carlos SMS Sample Application Started Successfully ===");
    }
}
