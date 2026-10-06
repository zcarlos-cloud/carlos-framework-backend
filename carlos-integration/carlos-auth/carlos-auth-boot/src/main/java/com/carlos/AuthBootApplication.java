package com.carlos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * 启动程序
 *
 * @author carlos
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class AuthBootApplication {

    public static void main(final String[] args) {
        SpringApplication.run(AuthBootApplication.class, args);
    }
}
