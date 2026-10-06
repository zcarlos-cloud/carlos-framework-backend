package com.carlos.system.upload.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * <p>
 * 系统文件配置
 * </p>
 *
 * @author Carlos
 * @date 2024/1/5 13:23
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SystemFileProperties.class)
public class SystemFileConfig {


}
