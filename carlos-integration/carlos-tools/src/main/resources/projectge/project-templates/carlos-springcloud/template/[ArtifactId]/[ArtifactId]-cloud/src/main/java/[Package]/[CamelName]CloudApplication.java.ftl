package ${project.packageName};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * <p>
 * 微服务启动类
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@EnableDiscoveryClient
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class ${project.camelName}CloudApplication {

    public static void main ( final String[] args){
        SpringApplication.run(${project.camelName}CloudApplication.class, args);
    }
}
