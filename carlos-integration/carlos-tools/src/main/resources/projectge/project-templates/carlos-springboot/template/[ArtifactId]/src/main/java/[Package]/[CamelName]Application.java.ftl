package ${project.packageName};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
* <p>
    * Spring Boot 启动类
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@SpringBootApplication
public class ${project.camelName}Application {

public static void main(String[] args) {
SpringApplication.run(${project.camelName}BootApplication.class, args);
}
}
