package ${project.packageName}.api;

import ${project.packageName}.ServiceNameConstant;
import ${project.packageName}.api.fallback.Api${table.classPrefix}FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;

/**
* <p>
    * ${table.comment} feign 提供接口
    * </p>
*
* @author  ${project.author}
* @date    ${.now}
*/
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "${table.classMainPrefix}", path = "/api/${table.apiPath}", fallbackFactory = Api${table.classPrefix}FallbackFactory.class)
public interface Api${table.classPrefix} {


}
