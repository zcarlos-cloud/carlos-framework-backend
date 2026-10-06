package ${project.packageName}.api.fallback;

import ${project.packageName}.api.Api${table.classPrefix};
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;


/**
 * <p>
 * ${table.comment} api 降级
 * </p>
 *
 * @author  ${project.author}
 * @date    ${.now}
 */
@Slf4j
public class Api${table.classPrefix}FallbackFactory implements FallbackFactory<Api${table.classPrefix}> {

    @Override
    public Api${table.classPrefix} create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("${table.comment}服务调用失败: message:{}", message);
        return new Api${table.classPrefix}() {

        };
    }
}
