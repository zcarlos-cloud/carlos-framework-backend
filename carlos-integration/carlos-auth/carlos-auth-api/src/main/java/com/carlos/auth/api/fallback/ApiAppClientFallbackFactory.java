package com.carlos.auth.api.fallback;

import com.carlos.auth.api.ApiAppClient;
import com.carlos.auth.api.pojo.ao.AppClientAO;
import com.carlos.core.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;


/**
 * <p>
 * 应用信息 api 降级
 * </p>
 *
 * @author Carlos
 * @date 2025-3-12 14:00:14
 */
@Slf4j
public class ApiAppClientFallbackFactory implements FallbackFactory
    <ApiAppClient> {

    @Override
    public ApiAppClient create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("应用信息服务调用失败: message:{}", message);
        return new ApiAppClient() {

            @Override
            public Result<AppClientAO> getById(Long id) {
                return Result.error("应用信息获取失败！");
            }

            @Override
            public Result<AppClientAO> getByAppKey(String appKey) {
                return Result.error("应用信息获取失败！");
            }
        };
    }


    // 将此段配置放入配置类中进行Bean加载
    // @Bean
    // public ApiAppClientFallbackFactory registeredClientFallbackFactory() {
    //     return new ApiAppClientFallbackFactory();
    // }
}
